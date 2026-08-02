$ports = @(3002, 8081)
foreach ($p in $ports) {
  try {
    $client = New-Object System.Net.Sockets.TcpClient
    $iar = $client.BeginConnect('localhost', $p, $null, $null)
    $ok = $iar.AsyncWaitHandle.WaitOne(1500, $false)
    if ($ok -and $client.Connected) {
      Write-Host "Port $p : UP"
    } else {
      Write-Host "Port $p : DOWN"
    }
    $client.Close()
  } catch {
    Write-Host "Port $p : DOWN ($($_.Exception.Message))"
  }
}
