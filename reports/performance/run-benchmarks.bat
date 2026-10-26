@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "PLAN=%SCRIPT_DIR%scalable-banking-benchmark.jmx"
set "HOST=%~1"
if "%HOST%"=="" set "HOST=localhost"
set "PORT=%~2"
if "%PORT%"=="" set "PORT=8080"

echo ==========================================================
echo  Starting UCS3513 JMeter Performance Benchmarking Suite
echo  Target: http://%HOST%:%PORT%
echo ==========================================================

if not exist "%SCRIPT_DIR%raw" mkdir "%SCRIPT_DIR%raw"

REM 1. Low Workload: 10 concurrent threads
echo.
echo Running Low Workload (10 threads)...
jmeter -n -t "%PLAN%" -Jhost=%HOST% -Jport=%PORT% -Jthreads=10 -Jrampup=5 -Jloops=20 -l "%SCRIPT_DIR%raw\results-low.jtl" -e -o "%SCRIPT_DIR%dashboard-low"

REM 2. Medium Workload: 25 concurrent threads
echo.
echo Running Medium Workload (25 threads)...
jmeter -n -t "%PLAN%" -Jhost=%HOST% -Jport=%PORT% -Jthreads=25 -Jrampup=10 -Jloops=20 -l "%SCRIPT_DIR%raw\results-medium.jtl" -e -o "%SCRIPT_DIR%dashboard-medium"

REM 3. High Workload: 50 concurrent threads
echo.
echo Running High Workload (50 threads)...
jmeter -n -t "%PLAN%" -Jhost=%HOST% -Jport=%PORT% -Jthreads=50 -Jrampup=15 -Jloops=20 -l "%SCRIPT_DIR%raw\results-high.jtl" -e -o "%SCRIPT_DIR%dashboard-high"

echo.
echo All JMeter benchmarks completed!
