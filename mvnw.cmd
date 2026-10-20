@echo off
setlocal
set "DIR=%~dp0"
set "MAVEN_CMD=C:\Users\Mohammed Haris\.m2\wrapper\dists\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd"

if exist "%MAVEN_CMD%" (
    call "%MAVEN_CMD%" %*
) else (
    mvn %*
)
