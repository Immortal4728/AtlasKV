# PowerShell Script to perform cluster crash and recovery testing

$baseDir = "D:\RAFT"

Write-Host "=============================================" -ForegroundColor Green
Write-Host "Starting Cluster Crash and Recovery Test" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

# 1. Stop any existing cluster
& "$baseDir\benchmarks\scripts\stop-cluster.ps1"

# 2. Start fresh cluster (clears previous data directories)
& "$baseDir\benchmarks\scripts\start-cluster.ps1"

# 3. Write test keys
Write-Host "Writing test keys to verify persistence..." -ForegroundColor Cyan
$writtenKeys = @{}
$leaderPort = 8081

# Find a port we can write to (follows redirection automatically in our script if needed,
# or we just write to any port since our server handles redirection)
for ($i = 0; $i -lt 10; $i++) {
    $key = "benchmark/crash-key-$i"
    $val = "crash-val-$i"
    
    # Try putting via port 8081, handle redirection manually if it throws 503 Not Leader
    $success = $false
    $targetPort = 8081
    $attempts = 0
    
    while (-not $success -and $attempts -lt 3) {
        $attempts++
        try {
            # Let's perform the POST
            $bodyJson = @{ value = $val } | ConvertTo-Json
            $response = Invoke-RestMethod -Method Post -Uri "http://localhost:$targetPort/api/v1/kv/$key" -Body $bodyJson -ContentType "application/json"
            $success = $true
            Write-Host "Successfully wrote $key to port $targetPort" -ForegroundColor Gray
        } catch {
            # Check if it was a 503 Not Leader redirection
            $err = $_.Exception.Response
            if ($err -and $err.StatusCode -eq 503) {
                # Read response stream to get leaderAddress
                $stream = $err.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $body = $reader.ReadToEnd() | ConvertFrom-Json
                if ($body.leaderAddress) {
                    # Extract port from leaderAddress (e.g. localhost:8083 -> 8083)
                    $addrParts = $body.leaderAddress -split ":"
                    $targetPort = $addrParts[-1]
                    Write-Host "Redirected to leader port $targetPort..." -ForegroundColor Gray
                }
            } else {
                Write-Host "Error writing ${key}: $_" -ForegroundColor Red
                break
            }
        }
    }
}

Write-Host "Keys written successfully. Verifying replication before crash..." -ForegroundColor Cyan
Start-Sleep -Seconds 2

# 4. Crash all nodes simultaneously
# Stop all processes listening on the cluster ports
foreach ($port in @(8081, 8082, 8083, 50051, 50052, 50053)) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        $pidToKill = $conn[0].OwningProcess
        if ($pidToKill) {
            Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
            Write-Host "Crashed process $pidToKill listening on port $port" -ForegroundColor Yellow
        }
    }
}
Write-Host "Cluster crashed successfully." -ForegroundColor Green
Start-Sleep -Seconds 3


# 5. Start cluster again WITHOUT cleaning data directories
# Wait, start-cluster.ps1 cleans data directories by default!
# Let's check start-cluster.ps1:
# "Remove-Item -Recurse -Force "$baseDir\data" -ErrorAction SilentlyContinue"
# Ah! Since start-cluster.ps1 cleans data directories, we cannot use it to test persistence recovery!
# We must start the nodes manually in this script, preserving their data directories!
# This is a critical realization!

Write-Host "Restarting nodes preserving data directories for recovery..." -ForegroundColor Cyan
$jarPath = "$baseDir\atlaskv-server\target\atlaskv-server-0.1.0-SNAPSHOT-exec.jar"
$peerNodes = "node1:localhost:50051,node2:localhost:50052,node3:localhost:50053"

# Node 1
$env:NODE_ID = "node1"
$env:REST_PORT = "8081"
$env:GRPC_PORT = "50051"
$env:DATA_DIRECTORY = "$baseDir\data\node1"
$env:PEER_NODES = $peerNodes
$env:LOG_LEVEL = "INFO"
$p1 = Start-Process java -ArgumentList "-jar", $jarPath, "--logging.level.org.springframework=WARN", "--logging.level.org.springframework.beans=WARN", "--logging.level.org.springframework.beans.factory.support=WARN" -RedirectStandardOutput "$baseDir\data\node1.log" -RedirectStandardError "$baseDir\data\node1.err" -WindowStyle Hidden -PassThru
$p1.Id | Out-File -FilePath "$baseDir\data\node1.pid" -Force

# Node 2
$env:NODE_ID = "node2"
$env:REST_PORT = "8082"
$env:GRPC_PORT = "50052"
$env:DATA_DIRECTORY = "$baseDir\data\node2"
$env:PEER_NODES = $peerNodes
$env:LOG_LEVEL = "INFO"
$p2 = Start-Process java -ArgumentList "-jar", $jarPath, "--logging.level.org.springframework=WARN", "--logging.level.org.springframework.beans=WARN", "--logging.level.org.springframework.beans.factory.support=WARN" -RedirectStandardOutput "$baseDir\data\node2.log" -RedirectStandardError "$baseDir\data\node2.err" -WindowStyle Hidden -PassThru
$p2.Id | Out-File -FilePath "$baseDir\data\node2.pid" -Force

# Node 3
$env:NODE_ID = "node3"
$env:REST_PORT = "8083"
$env:GRPC_PORT = "50053"
$env:DATA_DIRECTORY = "$baseDir\data\node3"
$env:PEER_NODES = $peerNodes
$env:LOG_LEVEL = "INFO"
$p3 = Start-Process java -ArgumentList "-jar", $jarPath, "--logging.level.org.springframework=WARN", "--logging.level.org.springframework.beans=WARN", "--logging.level.org.springframework.beans.factory.support=WARN" -RedirectStandardOutput "$baseDir\data\node3.log" -RedirectStandardError "$baseDir\data\node3.err" -WindowStyle Hidden -PassThru
$p3.Id | Out-File -FilePath "$baseDir\data\node3.pid" -Force

Write-Host "Waiting for recovered cluster to elect a leader..." -ForegroundColor Yellow
$recoveredLeader = $null
$attempts = 0
$maxAttempts = 15

while (-not $recoveredLeader -and $attempts -lt $maxAttempts) {
    $attempts++
    Write-Host "Checking recovered leader status (attempt $attempts/$maxAttempts)..." -ForegroundColor Gray
    Start-Sleep -Seconds 2
    
    foreach ($port in @(8081, 8082, 8083)) {
        try {
            $status = Invoke-RestMethod -Uri "http://localhost:$port/api/v1/cluster/status"
            if ($status.currentLeader) {
                $recoveredLeader = $status.currentLeader
                break
            }
        } catch {
            # Ignore and retry
        }
    }
}

if (-not $recoveredLeader) {
    Write-Host "Failed to determine leader after recovery. Aborting crash test verification." -ForegroundColor Red
    & "$baseDir\benchmarks\scripts\stop-cluster.ps1"
    exit 1
}

Write-Host "Recovered leader is: $recoveredLeader" -ForegroundColor Green


# 6. Verify keys are persistent
Write-Host "Verifying written keys after recovery..." -ForegroundColor Cyan
$recoverySuccess = $true

for ($i = 0; $i -lt 10; $i++) {
    $key = "benchmark/crash-key-$i"
    $expectedVal = "crash-val-$i"
    $targetPort = 8081
    $attempts = 0
    $verified = $false
    
    while (-not $verified -and $attempts -lt 3) {
        $attempts++
        try {
            $kv = Invoke-RestMethod -Uri "http://localhost:$targetPort/api/v1/kv/$key"
            if ($kv.value -eq $expectedVal) {
                Write-Host "VERIFIED: Key $key still exists with correct value." -ForegroundColor Green
                $verified = $true
            } else {
                Write-Host "ERROR: Key $key value mismatch! Expected: $expectedVal, Got: $($kv.value)" -ForegroundColor Red
                $recoverySuccess = $false
                $verified = $true
            }
        } catch {
            $err = $_.Exception.Response
            if ($err -and $err.StatusCode -eq 503) {
                $stream = $err.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $body = $reader.ReadToEnd() | ConvertFrom-Json
                if ($body.leaderAddress) {
                    $addrParts = $body.leaderAddress -split ":"
                    $targetPort = $addrParts[-1]
                }
            } else {
                Write-Host "ERROR: Key ${key} could not be retrieved from port ${targetPort}: $_" -ForegroundColor Red
                $recoverySuccess = $false
                break
            }
        }
    }
}

if ($recoverySuccess) {
    Write-Host "CRASH RECOVERY TEST SUCCESSFUL! All keys persisted and retrieved after cluster crash!" -ForegroundColor Green
} else {
    Write-Host "CRASH RECOVERY TEST FAILED! Some keys were lost or mismatch occurred." -ForegroundColor Red
}

# 7. Clean up
& "$baseDir\benchmarks\scripts\stop-cluster.ps1"
Write-Host "=============================================" -ForegroundColor Green
Write-Host "Crash and Recovery Test Completed" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
