import groovy.json.JsonSlurperClassic
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy

// STAGE FUNCTIONS DEFINITION
def checkoutCode(Map args=[:]){
  stage('Clone repository') {
    // CHECKOUT CODE REPO
    if(args.checkout_tag){
      scmVars = checkout([
          $class: 'GitSCM',
          branches: [[name: "refs/tags/${SELECTED_TAG}"]],
          doGenerateSubmoduleConfigurations: false,
          extensions: [[$class: 'CleanCheckout'],
          [$class: 'PruneStaleBranch']],
          submoduleCfg: []
      ])
      // Re-init variables
      prepareVars()
    } else {
      scmVars = checkout scm
    }
  }
}

def checkoutCodePipeLine(){
    // CheckOut pipeline code 
    pipeline_url= "https://git.pttdigital.com/devops/common-jenkinsfile.git"
    stage('ClonePipeLine') {
      // CHECKOUT CODE REPO
      checkout changelog: false, poll: false, scm: [
        $class: 'GitSCM',
        branches: [[name: '*/master']],
        doGenerateSubmoduleConfigurations: false,
        extensions: [
          [$class: 'CleanCheckout'],
          [$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: true],
          [$class: 'SubmoduleOption', disableSubmodules: true, parentCredentials: false, recursiveSubmodules: false, reference: '', trackingSubmodules: false],
          [$class: 'RelativeTargetDirectory', relativeTargetDir: 'tmpdir']],
        submoduleCfg: [],
        userRemoteConfigs: [[credentialsId: 'gitlab-user-jenkinsci', url: pipeline_url]]
      ]
    }
  // LogRotator build 10
  properties([[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);
}

def runUnitTest(){
  stage('Unit test') {
    echo "Start building test image [${imgName}:${imgTag}-test]"
    // BUILD TEST IMAGE
    def dockerfile = "./Dockerfile"
    docker.withRegistry("https://${imgRepoServerUrl}", "${imgRepoCred}") {
      def img = docker.build("${imgName}:${imgTag}-test", "--pull -f ${dockerfile} .")
    }
    // RUN UNIT TEST WITH TEST IMAGE
    try {
      dir ("tests/TestResults" ) {
        sh """
        docker run --rm -e JEST_JUNIT_OUTPUT_DIR=TestResults -v "\$(pwd)":/usr/src/app/TestResults ${imgName}:${imgTag}-test node ./node_modules/jest/bin/jest.js --noStackTrace --silent --coverage --coverageDirectory=TestResults --ci --reporters=default --reporters=jest-junit
        """
        // COLLECT RESULT TO JENKINS
        junit 'junit.xml'
      } 
    } catch(err) {
        throw err
    } finally {
      dir ("tests/TestResults" ) {
        // dirty fix permission (for container with root user)
        sh """
        docker run --rm -v "\$(pwd)":/data alpine:latest chmod -R 777 /data
        """
      } 
    }
    echo "Removing test image ${imgName}:${imgTag}-test"
    sh "docker rmi ${imgName}:${imgTag}-test"
  } /* stage run test image */
}

node('pttdevops-slave') {
  
  // ALWAYS CHECKOUT CODE FIRST
  checkoutCode()
  checkoutCodePipeLine()
  // prepareVars
  def variables = load "preparevars.groovy"

  variables.prepareVars()

  // FOR RETAG JOB ONLY
  if(env.RETAG_ONLY) {
    variables.prepareVars()
    return this
  }

  // CALL PREPARE FUNCTION
  def common = load "tmpdir/pipeline/pipelinefunction.groovy"

  switch(envName) {
    case ["dev", "sit"]:
      // common.runUnitTest()
      //runUnitTest()
      common.runOWASP()
      // common.runCheckMarx()
      common.runSonarQube()
      common.buildAndPushDockerImage()
      common.prepareHelm()
      common.deployApp()
      // common.runUITest()
      	sleep(300)
      common.runPerfTest()
      break
    case "uat":
      if(hookServerName == "git"){ // HOOK FROM GITLAB(DIGITAL)
        common.waitBackupApproval()
        def backupRepoUrl = common.prepareGitlabBackupURL(project: backupProject, group: backupGroup)
        echo "repo url: ${backupRepoUrl}"
        def hookID = common.prepareHookURL(project: backupProject, group: backupGroup, hookURL: backupHookURL)
        echo "hook id: ${hookID}"
        common.backupRepoToPLC(url: backupRepoUrl)
      } else { // HOOK FROM GITLAB(PLC)
        common.waitBuildApproval()
        // common.runUnitTest()
        
        //runUnitTest()
        common.runOWASP()
        // common.runCheckMarx()
        common.runSonarQube()
        common.buildAndPushDockerImage()
        common.waitDeployApproval()
        common.prepareHelm()
        common.deployApp()
        // common.runUITest()
        	sleep(300)
        common.runPerfTest()
      }
      break
    case "production":
      common.checkoutCode(checkout_tag: true)
      common.prepareHelm()
      common.deployApp()
      //runUITest()
      break
  }

} /* node */
