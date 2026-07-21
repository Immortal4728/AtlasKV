# PowerShell Script to run the complete load testing suite

$baseDir = "D:\RAFT"
$benchmarkJar = "$baseDir\atlaskv-benchmarks\target\atlaskv-benchmarks-0.1.0-SNAPSHOT.jar"

Write-Host "=============================================" -ForegroundColor Green
Write-Host "Starting Performance Load Benchmark Suite" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

# 1. Stop any existing cluster
& "$baseDir\benchmarks\scripts\stop-cluster.ps1"

# 2. Start fresh cluster
& "$baseDir\benchmarks\scripts\start-cluster.ps1"

# Wait for a leader to be elected
Write-Host "Waiting for cluster leader election..." -ForegroundColor Yellow
$leader = $null
$attempts = 0
$maxAttempts = 15
while (-not $leader -and $attempts -lt $maxAttempts) {
    $attempts++
    Write-Host "Checking cluster leader status (attempt $attempts/$maxAttempts)..." -ForegroundColor Gray
    Start-Sleep -Seconds 2
    foreach ($port in @(8081, 8082, 8083)) {
        try {
            $status = Invoke-RestMethod -Uri "http://localhost:$port/api/v1/cluster/status"
            if ($status.currentLeader) {
                $leader = $status.currentLeader
                break
            }
        } catch {}
    }
}

if (-not $leader) {
    Write-Host "Failed to determine leader after startup. Aborting benchmarks." -ForegroundColor Red
    & "$baseDir\benchmarks\scripts\stop-cluster.ps1"
    exit 1
}
Write-Host "Cluster leader elected: $leader" -ForegroundColor Green

# 3. Run Benchmark Runner
# We run all scenarios with concurrency levels: 10, 50, 100, 250, 500
Write-Host "Running Benchmark Runner..." -ForegroundColor Cyan
java -jar $benchmarkJar --host=localhost --port=8081 --duration=3 --loads="10,50,100,250,500" --scenario=all

# 4. Stop cluster
& "$baseDir\benchmarks\scripts\stop-cluster.ps1"

Write-Host "=============================================" -ForegroundColor Green
Write-Host "Performance Load Benchmark Suite Completed" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
