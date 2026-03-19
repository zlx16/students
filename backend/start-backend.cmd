@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
cd /d "%~dp0"

REM 1) 若当前 JAVA_HOME 无效则清空
if defined JAVA_HOME (
    if not exist "%JAVA_HOME%\bin\java.exe" set "JAVA_HOME="
)

REM 2) 若仍无有效 JAVA_HOME，从 PATH 的 java 反推
if not defined JAVA_HOME (
    for /f "tokens=*" %%i in ('where java 2^>nul') do (
        set "JAVACMD=%%i"
        goto :found_java
    )
    goto :try_common
)
goto :run_mvn

:found_java
REM JAVACMD 形如 C:\Program Files\Java\jdk-17\bin\java.exe，取其上级目录为 JAVA_HOME
for %%a in ("!JAVACMD!") do set "BINDIR=%%~dpa"
REM BINDIR 结尾为 \bin\，去掉 \bin\ 得 JDK 根目录
if defined BINDIR set "JAVA_HOME=!BINDIR:~0,-5!"
goto :run_mvn

:try_common
REM 3) 常见安装路径
for %%d in (
    "C:\Program Files\Java\jdk-21"
    "C:\Program Files\Java\jdk-17"
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.2+7"
    "C:\Program Files\Microsoft\jdk-21.0.5.11-hotspot"
) do (
    if exist %%d\bin\java.exe (
        set "JAVA_HOME=%%~d"
        goto :run_mvn
    )
)
REM 扫描 C:\Program Files\Java 下任意子目录
for /d %%d in ("C:\Program Files\Java\jdk-*") do (
    if exist "%%d\bin\java.exe" (
        set "JAVA_HOME=%%d"
        goto :run_mvn
    )
)

echo Error: 未找到 JDK。请安装 JDK 或将 java 加入 PATH，或设置正确的 JAVA_HOME。
exit /b 1

:run_mvn
if "%~1"=="" (call mvnw.cmd spring-boot:run) else (call mvnw.cmd %*)
exit /b %ERRORLEVEL%
