# PowerShell Script to perform cluster failover testing

$baseDir = "D:\RAFT"
$benchmarkJar = "$baseDir\atlaskv-benchmarks\target\atlaskv-benchmarks-0.1.0-SNAPSHOT.jar"

Write-Host "=============================================" -ForegroundColor Green
Write-Host "Starting Cluster Failover Stress Test" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

# 1. Stop any existing cluster
& "$baseDir\benchmarks\scripts\stop-cluster.ps1"

# 2. Start fresh cluster
& "$baseDir\benchmarks\scripts\start-cluster.ps1"

# 3. Find the current leader
Write-Host "Querying cluster status to find current leader..." -ForegroundColor Cyan
$leader = $null
$attempts = 0
$maxAttempts = 15

while (-not $leader -and $attempts -lt $maxAttempts) {
    $attempts++
    Write-Host "Checking leader status (attempt $attempts/$maxAttempts)..." -ForegroundColor Gray
    Start-Sleep -Seconds 2
    
    foreach ($port in @(8081, 8082, 8083)) {
        try {
            $status = Invoke-RestMethod -Uri "http://localhost:$port/api/v1/cluster/status"
            if ($status.currentLeader) {
                $leader = $status.currentLeader
                break
            }
        } catch {
            # Ignore and try next port/attempt
        }
    }
}

if (-not $leader) {
    Write-Host "Failed to determine current leader after $maxAttempts attempts. Aborting failover test." -ForegroundColor Red
    & "$baseDir\benchmarks\scripts\stop-cluster.ps1"
    exit 1
}

Write-Host "Current leader is: $leader" -ForegroundColor Green

# 4. Start concurrent background workload
Write-Host "Starting continuous background load (MIXED_RW, Concurrency=30)..." -ForegroundColor Cyan
$benchProcess = Start-Process java -ArgumentList "-jar", $benchmarkJar, "--host=localhost", "--port=8081", "--duration=15", "--loads=30", "--scenario=mixed_rw" -PassThru -WindowStyle Hidden

# 5. Wait 4 seconds, then crash the leader node
Write-Host "Waiting 4 seconds before crashing the leader..." -ForegroundColor Yellow
Start-Sleep -Seconds 4

Write-Host "Crashing leader node [$leader]..." -ForegroundColor Red
$portMap = @{
    "node1" = 8081
    "node2" = 8082
    "node3" = 8083
}
$leaderPort = $portMap[$leader]

if ($leaderPort) {
    # Find the process owning the port
    $conn = Get-NetTCPConnection -LocalPort $leaderPort -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        $leaderPid = $conn[0].OwningProcess
        if ($leaderPid) {
            Stop-Process -Id $leaderPid -Force
            Write-Host "Successfully crashed leader node [$leader] (PID: $leaderPid) listening on port $leaderPort" -ForegroundColor Green
        } else {
            Write-Host "Could not retrieve PID for port $leaderPort." -ForegroundColor Red
        }
    } else {
        Write-Host "No process found listening on port $leaderPort." -ForegroundColor Red
    }
} else {
    Write-Host "Unknown leader: $leader" -ForegroundColor Red
}

# 6. Wait for workload to complete
Write-Host "Waiting for background workload to complete..." -ForegroundColor Yellow
$benchProcess | Wait-Process

# 7. Check if cluster recovered and elected a new leader
Write-Host "Checking remaining nodes to verify failover recovery..." -ForegroundColor Cyan
Start-Sleep -Seconds 3 # Allow election to settle if still in progress

$recovered = $false
foreach ($port in @(8081, 8082, 8083)) {
    try {
        $status = Invoke-RestMethod -Uri "http://localhost:$port/api/v1/cluster/status"
        Write-Host "Node at port $port Status: Role=$($status.role), Leader=$($status.currentLeader), Healthy=$($status.healthy)" -ForegroundColor Yellow
        if ($status.currentLeader -and $status.currentLeader -ne $leader) {
            $recovered = $true
            $newLeader = $status.currentLeader
        }
    } catch {
        # Node might be the one we crashed
        Write-Host "Node at port $port is unreachable (expected for crashed node)." -ForegroundColor Gray
    }
}

if ($recovered) {
    Write-Host "FAILOVER SUCCESSFUL! New leader elected: $newLeader" -ForegroundColor Green
} else {
    Write-Host "FAILOVER FAILED! No new leader was elected within the window." -ForegroundColor Red
}

# 8. Clean up
& "$baseDir\benchmarks\scripts\stop-cluster.ps1"
Write-Host "=============================================" -ForegroundColor Green
Write-Host "Failover Stress Test Completed" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
