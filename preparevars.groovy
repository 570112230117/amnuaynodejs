import groovy.json.JsonSlurperClassic
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy

// HELPER FUNCTIONS
def commitSha1() {
  sh 'git rev-parse HEAD > commitSha1'
  def commit = readFile('commitSha1').trim()
  sh 'rm commitSha1'
  // Use first 6 chars from full SHA1
  commit.substring(0, 6)
}
// GET USERS FROM SPECIFIED ROLE
def getUserFromRole(role) {
  echo "Retrieving users for ${role}..."
  def strategy = RoleBasedAuthorizationStrategy.instance;
  if (strategy != null) {
    return strategy.getGrantedRoles(RoleBasedAuthorizationStrategy.GLOBAL)
            .entrySet()
            .find { entry -> entry.key.getName().equals(role) }.getValue().join(',')
  } else {
    throw new Exception("Role Strategy Plugin not in use.  Please enable to retrieve users for a role")
  }
}
def getEnvName() {
  def envName = ""
  switch(env.BRANCH_NAME) {
    case "dev":
      return "dev"
      break
    case "sit":
      return "sit"
      break
    case "master":
      return "uat"
      break
    default:
      if(env.SELECTED_TAG != null && SELECTED_TAG != "") {
        return "production"
      } else {
        throw new Exception("Cannot define environment name!")
      }
  }
}

def getGitServerName() {
  def matcher = (scmVars.GIT_URL =~ /.*[\/*@](\w+).pttdigital\.com.*/)
  if (matcher.find()) {
    return matcher.group(1)
  }
  return false
}

// PREPARE VARIABLES FUNCTION
def prepareVars() {
  projectID = "bd-amnuaynodejs-bc-pttnge-devop"
  serviceID = "amnuaynodejs"
  commitHash = commitSha1()
  envName = getEnvName()
  hookServerName = getGitServerName()
  echo "This job running on [${envName}] configurations. Hook server [${hookServerName}]"
  appFullName = "${projectID}-${serviceID}-${envName}"
  devOpsTeamEmail = env.EMAIL_DEVOPS
  devOpsRoleName = "Operation"
  qaTeamEmail = env.EMAIL_QA
  qaRoleName = "QA"
  // BACKUP REPO
  backupServer = "https://pttgit.pttdigital.com"
  backupGroup = projectID
  backupProject = serviceID
  backupHookURL = "https://pttjenkins.pttdigital.com/project/${projectID}-${serviceID}-uat-build"
  backupApiToken = "gitlab-token-jenkinsci"
  backupRepoCred = "gitlab-user-jenkinsci"
  // NEXUS IMAGE REPO
  imgTag = "${envName}-${commitHash}"
  imgRepoDigital = "nexus-registry.pttdigital.com"
  imgCredDigital = "nexus-user-pm"
  imgCredPLC = "nexus-user-jenkins"
  imgRepoPLC = "pttnexus-registry.pttdigital.com"
  // HELM CHARTS
  helmRepoUrl = "https://git.pttdigital.com/devops/helm-charts-release.git"
  helmRepoCred = "gitlab-user-jenkinsci"
  helmSubDir = "helm-charts"
  helmChartName = "simple-generic-app-helm3"
  helmValuesFile = "${WORKSPACE}/.helmValues/${envName}.yaml"
  helmWaitTimeout = "5m0s"
  // OCP
  ocpServer = "https://api.ocpdev.pttdigital.com:6443" // SAME ACROSS ALL ENV
  ocpNonProdProject = "bd-amnuaynodejs-bc-pttnge-devop"
  ocpNonProdCred = "devops-deploy-non-prd"
  ocpProdProject = "bd-amnuaynodejs-bc-pttnge-devop-prd"
  ocpProdCred = "devops-deploy-prd"
  ocpAppName = appFullName
  // TESTS
  robotTestFileName ="ui-test.robot"
  bztTestFileName = "perf-test.jmx"
  testProto = "https" // SAME ACROSS ALL ENV
  testDomain = "${appFullName}.apps.ocpdev.pttdigital.com"
  switch(envName) {
    case "dev":
      imgRepoServerUrl = imgRepoDigital
      imgRepoCred = imgCredDigital

      ocpProject = "bd-projectnext-bc"
      ocpCred = ocpNonProdCred
      buildProfile = "${envName}"
      robotTestFile = "${robotTestFileName}"
      bztTestFile = "${bztTestFileName}"
      break
    case "sit":
      imgRepoServerUrl = imgRepoDigital
      imgRepoCred = imgCredDigital
      buildProfile = "${envName}"
      ocpProject = "${ocpNonProdProject}-sit"
      ocpCred = ocpNonProdCred
      buildProfile = "${envName}"
      robotTestFile = "${robotTestFileName}"
      bztTestFile = "${bztTestFileName}"
      break
    case "uat":
      imgRepoServerUrl = imgRepoPLC
      imgRepoCred = imgCredPLC
      buildProfile = "${envName}"
      ocpProject = "${ocpNonProdProject}-uat"
      ocpCred = ocpNonProdCred

      robotTestFile = "${robotTestFileName}"
      bztTestFile = "${bztTestFileName}"
      break
    case "production":
      imgRepoServerUrl = imgRepoPLC
      imgRepoCred = imgCredPLC
      imgTag = SELECTED_TAG // FROM JENKINS JOB PARAMETERS
      
      ocpProject = ocpProdProject
      ocpCred = ocpProdCred

      testDomain = "XXXXXXX"

      robotTestFile = "${robotTestFileName}"
      bztTestFile = "${bztTestFileName}"
      break
  }
  imgName = "${imgRepoServerUrl}/${projectID}/${serviceID}"
}
// END VARIABLES PREPARE
return this
