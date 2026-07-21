# PowerShell Script to stop the 3-node AtlasKV cluster

Write-Host "Stopping all AtlasKV Server processes..." -ForegroundColor Red

# 1. Kill by Port connection to ensure ports are freed
$ports = @(8081, 8082, 8083, 50051, 50052, 50053)
foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connections) {
        foreach ($conn in $connections) {
            $targetPid = $conn.OwningProcess
            if ($targetPid -and $targetPid -ne 0) {
                Stop-Process -Id $targetPid -Force -ErrorAction SilentlyContinue
                Write-Host "Killed process $targetPid listening on port $port" -ForegroundColor Yellow
            }
        }
    }
}

# 2. Kill by WMI CommandLine filter for atlaskv-server
try {
    $processes = Get-CimInstance Win32_Process -Filter "name='java.exe'" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*atlaskv-server*" }
    if ($processes) {
        foreach ($p in $processes) {
            Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
            Write-Host "Killed process $($p.ProcessId) running: $($p.CommandLine)" -ForegroundColor Yellow
        }
    }
} catch {
    # Fallback to Get-WmiObject if Get-CimInstance fails
    $processes = Get-WmiObject Win32_Process -Filter "name='java.exe'" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*atlaskv-server*" }
    if ($processes) {
        foreach ($p in $processes) {
            Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
            Write-Host "Killed process $($p.ProcessId) running: $($p.CommandLine)" -ForegroundColor Yellow
        }
    }
}

Write-Host "Cleanup completed." -ForegroundColor Green
