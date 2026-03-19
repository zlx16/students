$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

$pidFile = Join-Path $PSScriptRoot ".sms.pid"

if (!(Test-Path $pidFile)) {
  Write-Host "未找到 PID 文件：$pidFile（可能已经停止）"
  exit 0
}

$pidText = (Get-Content $pidFile -Raw).Trim()
if (!($pidText -match '^\d+$')) {
  Write-Host "PID 文件内容无效，已删除：$pidFile"
  Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
  exit 0
}

$procId = [int]$pidText
$p = Get-Process -Id $procId -ErrorAction SilentlyContinue
if ($p) {
  Write-Host "正在停止学生管理系统进程 PID=$procId ..."
  Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
  Start-Sleep -Milliseconds 300
  Write-Host "已停止。"
} else {
  Write-Host "进程 PID=$procId 不存在（可能已停止）。"
}

Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
