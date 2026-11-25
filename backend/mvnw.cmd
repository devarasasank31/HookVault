@echo off
setlocal

set "MAVEN_PROJECT_BASEDIR=%~dp0"
set "WRAPPER_JAR=%MAVEN_PROJECT_BASEDIR%.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

if not exist "%WRAPPER_JAR%" (
    echo Maven wrapper not found.
    exit /b 1
)

set "MAVEN_OPTS=-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECT_BASEDIR%"
"%JAVA_HOME%\bin\java" %MAVEN_OPTS% -cp "%WRAPPER_JAR%" %WRAPPER_LAUNCHER% %*