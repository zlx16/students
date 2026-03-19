# 在 backend 目录执行: .\run-backend.ps1
# 会依次尝试: mvnw.cmd -> PATH/MAVEN_HOME/M2_HOME 的 mvn -> 自动下载 Maven Wrapper 后启动

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# 若系统 JAVA_HOME 指向无效目录，先清除，避免 mvnw 报错且便于下面从 PATH/常见路径 重新检测
$isValidJdk = { param($p) $p -and (Test-Path $p) -and (Test-Path (Join-Path $p 'bin\java.exe')) }
if ($env:JAVA_HOME -and -not (& $isValidJdk ($env:JAVA_HOME -replace '\\$', '').Trim())) {
    $env:JAVA_HOME = $null
}

# 检测并设置有效的 JAVA_HOME（Maven Wrapper 需要）
function Get-ValidJavaHome {
    $isValid = { param($p) $p -and (Test-Path $p) -and (Test-Path (Join-Path $p 'bin\java.exe')) }
    # 1) 当前 JAVA_HOME（去除首尾空格与末尾反斜杠）
    $current = ($env:JAVA_HOME -replace '\\$', '').Trim()
    if (& $isValid $current) { return $current }
    # 2) PATH 中的 java
    try {
        $javaExe = (Get-Command java -ErrorAction Stop).Source
        $binDir = [System.IO.Path]::GetDirectoryName($javaExe)
        $jhome = [System.IO.Path]::GetDirectoryName($binDir)
        if (& $isValid $jhome) { return $jhome }
    } catch {}
    try {
        $where = (where.exe java 2>$null) | Select-Object -First 1
        if ($where -and (Test-Path $where)) {
            $binDir = [System.IO.Path]::GetDirectoryName($where)
            $jhome = [System.IO.Path]::GetDirectoryName($binDir)
            if (& $isValid $jhome) { return $jhome }
        }
    } catch {}
    # 3) 扫描常见安装目录下的所有子目录
    $searchRoots = @(
        'C:\Program Files\Java',
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\Microsoft',
        'C:\Program Files\Amazon Corretto',
        'C:\Program Files\Zulu',
        'C:\Program Files (x86)\Java',
        $env:ProgramFiles + '\Java',
        $env:ProgramFiles + '\Eclipse Adoptium',
        $env:ProgramFiles + '\Microsoft'
    )
    foreach ($root in $searchRoots) {
        if (-not (Test-Path $root)) { continue }
        try {
            $dirs = Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue
            foreach ($d in $dirs) {
                if (& $isValid $d.FullName) { return $d.FullName }
            }
        } catch {}
    }
    return $null
}

$javaHome = Get-ValidJavaHome
if ($javaHome) {
    $env:JAVA_HOME = $javaHome
} else {
    Write-Host ''
    Write-Host '未找到有效的 JDK（需要目录下存在 bin\java.exe）。' -ForegroundColor Red
    Write-Host ''
    Write-Host '请任选其一：' -ForegroundColor Yellow
    Write-Host '1. 若已安装 JDK：在 PowerShell 中设置（将路径改为你本机实际 JDK 目录）：'
    Write-Host '   $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"'
    Write-Host '   .\run-backend.ps1'
    Write-Host ''
    Write-Host '2. 若未安装 JDK：从以下地址下载并安装 JDK 17 或 21，安装后再运行本脚本：'
    Write-Host '   https://adoptium.net/zh-CN/temurin/releases/'
    Write-Host '   https://www.oracle.com/java/technologies/downloads/'
    Write-Host ''
    Write-Host '3. 用 IDE 启动：用 IntelliJ / Eclipse / VS Code 打开 backend，运行主类 HaomeiApplication。'
    Write-Host ''
    exit 1
}

# 1) 已有 Maven Wrapper 则直接用
if (Test-Path '.\mvnw.cmd') {
    & .\mvnw.cmd spring-boot:run
    exit $LASTEXITCODE
}

# 2) 查找 Maven
$mvnCmd = $null
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    $mvnCmd = 'mvn'
} elseif ($env:MAVEN_HOME -and (Test-Path "$env:MAVEN_HOME\bin\mvn.cmd")) {
    $mvnCmd = "$env:MAVEN_HOME\bin\mvn.cmd"
} elseif ($env:M2_HOME -and (Test-Path "$env:M2_HOME\bin\mvn.cmd")) {
    $mvnCmd = "$env:M2_HOME\bin\mvn.cmd"
} elseif (Test-Path 'C:\Program Files\Apache\Maven\bin\mvn.cmd') {
    $mvnCmd = 'C:\Program Files\Apache\Maven\bin\mvn.cmd'
} elseif (Test-Path 'C:\maven\bin\mvn.cmd') {
    $mvnCmd = 'C:\maven\bin\mvn.cmd'
}

if ($mvnCmd) {
    & $mvnCmd spring-boot:run
    exit $LASTEXITCODE
}

# 3) 未找到 Maven：下载 Maven Wrapper 后启动
Write-Host '未找到 Maven。正在下载 Maven Wrapper（仅首次需要）...' -ForegroundColor Yellow
$zipUrl = 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper-distribution/3.3.4/maven-wrapper-distribution-3.3.4-bin.zip'
$zipPath = Join-Path $PSScriptRoot 'mvnw-dist.zip'
$tempDir = Join-Path $PSScriptRoot 'mvnw-temp'

try {
    Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath -UseBasicParsing
    if (-not (Test-Path $zipPath)) { throw '下载失败' }
    New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
    Expand-Archive -Path $zipPath -DestinationPath $tempDir -Force
    # zip 可能根目录即 mvnw.cmd，或有一层子目录
    $hasMvnw = Test-Path (Join-Path $tempDir 'mvnw.cmd')
    if ($hasMvnw) {
        Copy-Item -Path "$tempDir\*" -Destination $PSScriptRoot -Recurse -Force
    } else {
        $sub = Get-ChildItem $tempDir -Directory | Select-Object -First 1
        if ($sub) { Copy-Item -Path "$($sub.FullName)\*" -Destination $PSScriptRoot -Recurse -Force }
        else { Copy-Item -Path "$tempDir\*" -Destination $PSScriptRoot -Recurse -Force }
    }
} finally {
    if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
    if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
}

if (Test-Path '.\mvnw.cmd') {
    Write-Host 'Maven Wrapper 已就绪，正在启动后端...' -ForegroundColor Green
    & .\mvnw.cmd spring-boot:run
    exit $LASTEXITCODE
}

Write-Host ''
Write-Host '自动下载未成功。请任选其一：' -ForegroundColor Red
Write-Host '1. 安装 Maven 并将 bin 加入 PATH：https://maven.apache.org/download.cgi'
Write-Host '2. 用 IDE（IntelliJ / Eclipse / VS Code）打开 backend，运行主类 com.haomei.haomei.HaomeiApplication'
exit 1
