# Run after all 4 services are started:  powershell -ExecutionPolicy Bypass -File .\test-flow.ps1
$gw = "http://localhost:9090"

function Show($title, $method, $path, $token, $body) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $p = @{ Method = $method; Uri = "$gw$path"; Headers = $headers; UseBasicParsing = $true }
    if ($body) { $p.Body = ($body | ConvertTo-Json); $p.ContentType = "application/json" }
    try {
        $r = Invoke-WebRequest @p
        Write-Host ("[{0}] {1} {2} -> {3}" -f $title, $method, $path, [int]$r.StatusCode) -ForegroundColor Green
        Write-Host $r.Content
    } catch {
        $code = 0
        try { $code = [int]$_.Exception.Response.StatusCode } catch {}
        Write-Host ("[{0}] {1} {2} -> {3}" -f $title, $method, $path, $code) -ForegroundColor Yellow
    }
    Write-Host ""
}

$admin = (Invoke-RestMethod -Method Post -Uri "$gw/auth/login" -ContentType "application/json" -Body (@{username="admin";password="admin123"} | ConvertTo-Json)).token
$user  = (Invoke-RestMethod -Method Post -Uri "$gw/auth/login" -ContentType "application/json" -Body (@{username="user";password="user123"} | ConvertTo-Json)).token
Write-Host "Got tokens for admin and user via gateway`n"

$bad = $user.Substring(0, $user.Length - 3) + "abc"

Show "1 no token (expect 401)"            GET  "/books" $null $null
Show "2 tampered token (expect 401)"      GET  "/books" $bad  $null
Show "3 user lists books (expect 200)"    GET  "/books" $user $null
Show "4 forwarded token validated by book-service" GET "/books/whoami" $user $null
Show "5 user creates book (expect 403)"   POST "/books" $user @{title="DDD";author="Eric Evans";isbn="9780321125217"}
Show "6 admin creates book (expect 201)"  POST "/books" $admin @{title="DDD";author="Eric Evans";isbn="9780321125217"}
Show "7 user orders book 1 - token forwarded order->book (expect 201)" POST "/orders" $user @{bookId=1;quantity=2}
Show "8 order unknown book (expect 404)"  POST "/orders" $user @{bookId=99;quantity=1}
Show "9 user lists own orders (expect 200)" GET "/orders" $user $null
Show "10 order without token (expect 401)" POST "/orders" $null @{bookId=1;quantity=1}
