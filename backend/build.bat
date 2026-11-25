@echo off
setlocal
set "MAVEN_OPTS=-Dmaven.multiModuleProjectDirectory=C:\HookVault\backend"
java %MAVEN_OPTS% -cp ".mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*