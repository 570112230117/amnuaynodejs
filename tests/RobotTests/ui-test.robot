*** Settings ***
Library                  SeleniumLibrary
Suite Teardown           Close All Browsers

*** Variables ***
${PROTOCOL}              https
${DOMAIN}                www.google.com
${URL}                   ${PROTOCOL}://${DOMAIN}
${BROWSER}               headlesschrome
${WINDOW_WIDTH}          1920
${WINDOW_HEIGHT}         1080
@{chrome_arguments}      --headless  --disable-gpu  --no-sandbox

*** Keywords ***
Set Chrome Options
  [Documentation]        Set Chrome options for headless mode
  ${options}=            Evaluate    sys.modules['selenium.webdriver'].ChromeOptions()    sys, selenium.webdriver
  : FOR    ${option}    IN    @{chrome_arguments}
  \    Call Method    ${options}    add_argument    ${option}
  [Return]               ${options}

Open Headless Chrome Browser to Page
  [Arguments]            ${PAGE_URL}
  ${chrome_options}=     Set Chrome Options
  Create Webdriver       Chrome  chrome_options=${chrome_options}
  Set Window Size        ${WINDOW_WIDTH}  ${WINDOW_HEIGHT}
  Go To                  ${PAGE_URL}

*** Test Cases ***
Open Site
  [Tags]  web
  Open Headless Chrome Browser to Page  ${URL}
  Title Should Be                  PTT Smart Procurement
  Wait Until Page Contains         เข้าสู่ระบบ
  Capture Page Screenshot