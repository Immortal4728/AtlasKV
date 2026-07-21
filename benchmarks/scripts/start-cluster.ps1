# PowerShell Script to start a 3-node AtlasKV cluster locally

$baseDir = "D:\RAFT"
$jarPath = "$baseDir\atlaskv-server\target\atlaskv-server-0.1.0-SNAPSHOT-exec.jar"

# Clean up data directories from previous runs to start fresh
Remove-Item -Recurse -Force "$baseDir\data" -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path "$baseDir\data\node1" -Force | Out-Null
New-Item -ItemType Directory -Path "$baseDir\data\node2" -Force | Out-Null
New-Item -ItemType Directory -Path "$baseDir\data\node3" -Force | Out-Null

$peerNodes = "node1:localhost:50051,node2:localhost:50052,node3:localhost:50053"

Write-Host "Starting Node 1..." -ForegroundColor Green
$env:NODE_ID = "node1"
$env:REST_PORT = "8081"
$env:GRPC_PORT = "50051"
$env:DATA_DIRECTORY = "$baseDir\data\node1"
$env:PEER_NODES = $peerNodes
$env:LOG_LEVEL = "INFO"
$p1 = Start-Process java -ArgumentList "-jar", $jarPath, "--logging.level.org.springframework=WARN", "--logging.level.org.springframework.beans=WARN", "--logging.level.org.springframework.beans.factory.support=WARN" -RedirectStandardOutput "$baseDir\data\node1.log" -RedirectStandardError "$baseDir\data\node1.err" -WindowStyle Hidden -PassThru
$p1.Id | Out-File -FilePath "$baseDir\data\node1.pid" -Force

Write-Host "Starting Node 2..." -ForegroundColor Green
$env:NODE_ID = "node2"
$env:REST_PORT = "8082"
$env:GRPC_PORT = "50052"
$env:DATA_DIRECTORY = "$baseDir\data\node2"
$env:PEER_NODES = $peerNodes
$env:LOG_LEVEL = "INFO"
$p2 = Start-Process java -ArgumentList "-jar", $jarPath, "--logging.level.org.springframework=WARN", "--logging.level.org.springframework.beans=WARN", "--logging.level.org.springframework.beans.factory.support=WARN" -RedirectStandardOutput "$baseDir\data\node2.log" -RedirectStandardError "$baseDir\data\node2.err" -WindowStyle Hidden -PassThru
$p2.Id | Out-File -FilePath "$baseDir\data\node2.pid" -Force

Write-Host "Starting Node 3..." -ForegroundColor Green
$env:NODE_ID = "node3"
$env:REST_PORT = "8083"
$env:GRPC_PORT = "50053"
$env:DATA_DIRECTORY = "$baseDir\data\node3"
$env:PEER_NODES = $peerNodes
$env:LOG_LEVEL = "INFO"
$p3 = Start-Process java -ArgumentList "-jar", $jarPath, "--logging.level.org.springframework=WARN", "--logging.level.org.springframework.beans=WARN", "--logging.level.org.springframework.beans.factory.support=WARN" -RedirectStandardOutput "$baseDir\data\node3.log" -RedirectStandardError "$baseDir\data\node3.err" -WindowStyle Hidden -PassThru
$p3.Id | Out-File -FilePath "$baseDir\data\node3.pid" -Force

Write-Host "Waiting 10 seconds for cluster startup and leader election..." -ForegroundColor Yellow
Start-Sleep -Seconds 10
Write-Host "Cluster started." -ForegroundColor Green
