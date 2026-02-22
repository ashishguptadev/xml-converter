@ECHO OFF
SETLOCAL

SET "BASE_DIR=%~dp0"
SET "WRAPPER_DIR=%BASE_DIR%.mvn\wrapper"
SET "WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar"
SET "WRAPPER_PROPS=%WRAPPER_DIR%\maven-wrapper.properties"

IF NOT EXIST "%WRAPPER_JAR%" (
  IF NOT EXIST "%WRAPPER_PROPS%" (
    ECHO Missing wrapper properties: "%WRAPPER_PROPS%"
    EXIT /B 1
  )

  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; $props = Get-Content '%WRAPPER_PROPS%'; $line = $props | Where-Object { $_ -like 'wrapperUrl=*' } | Select-Object -First 1; if (-not $line) { throw 'wrapperUrl missing in maven-wrapper.properties' }; $url = $line.Substring(11); New-Item -ItemType Directory -Force -Path '%WRAPPER_DIR%' | Out-Null; Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile '%WRAPPER_JAR%'"
  IF ERRORLEVEL 1 EXIT /B 1
)

IF "%JAVA_HOME%"=="" (
  SET "JAVA_EXE=java"
) ELSE (
  SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

"%JAVA_EXE%" -Dmaven.multiModuleProjectDirectory="%BASE_DIR%" -jar "%WRAPPER_JAR%" %*
EXIT /B %ERRORLEVEL%
