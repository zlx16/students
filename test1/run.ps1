$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

$pidFile = Join-Path $PSScriptRoot ".sms.pid"

function Stop-SmsIfRunning {
  if (Test-Path $pidFile) {
    $oldPid = (Get-Content $pidFile -Raw).Trim()
    if ($oldPid -match '^\d+$') {
      $p = Get-Process -Id ([int]$oldPid) -ErrorAction SilentlyContinue
      if ($p) {
        Write-Host "检测到残留进程 PID=$oldPid，正在停止..."
        Stop-Process -Id ([int]$oldPid) -Force -ErrorAction SilentlyContinue
        Start-Sleep -Milliseconds 300
      }
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
  }
}

Stop-SmsIfRunning

if (!(Test-Path ".\.venv\Scripts\python.exe")) {
  Write-Host "未找到虚拟环境 .venv，正在创建..."
  & python -m venv .venv
}

& .\.venv\Scripts\python.exe -m pip install -r requirements.txt | Out-Host

Write-Host "启动学生管理系统..."
& .\.venv\Scripts\python.exe app.py --pid-file $pidFile
