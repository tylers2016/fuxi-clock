@echo off
set "GRADLE_EXEC=%USERPROFILE%\.gradle\wrapper\dists\gradle-9.1.0-bin\9agqghryom9wkf8r80qlhnts3\gradle-9.1.0\bin\gradle.bat"
if exist "%GRADLE_EXEC%" (
    call "%GRADLE_EXEC%" %*
) else (
    gradle %*
)
