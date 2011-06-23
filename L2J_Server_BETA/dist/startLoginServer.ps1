$OutputEncoding = New-Object -typename System.Text.UTF8Encoding
$a = (Get-Host).UI.RawUI
$a.WindowTitle = "L2J - Login Server Console"
$LASTEXITCODE = -1
do
{
    switch ($LASTEXITCODE)
    {
        -1 { cls; "Starting L2J Login Server."; break; }
        2 { cls; "Restarting L2J Login Server."; break; }
    }
    java -Xms128m -Xmx128m  -cp "./../libs/*;l2jlogin.jar" com.l2jserver.loginserver.L2LoginServer
}
while ($LASTEXITCODE -like 2)

if ($LASTEXITCODE -like 1)
{
    "Server Terminate Abnormally";
}
else 
{
    "Server Terminated";
}