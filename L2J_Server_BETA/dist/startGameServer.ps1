$OutputEncoding = New-Object -typename System.Text.UTF8Encoding
$a = (Get-Host).UI.RawUI
$a.WindowTitle = "L2J - Game Server Console"

do
{
    switch ($LASTEXITCODE)
    {
        -1 { cls; "Starting L2J Game Server."; break; }
        2 { cls; "Restarting L2J Game Server."; break; }
    }
    ""
    # -------------------------------------
    # Default parameters for a basic server.
    java "-Djava.util.logging.manager=com.l2jserver.util.L2LogManager" -Xms1024m -Xmx1024m -cp "./../libs/*;l2jserver.jar" com.l2jserver.gameserver.GameServer
    #
    # If you have a big server and lots of memory, you could experiment for example with
    # java -server -Xmx1536m -Xms1024m -Xmn512m -XX:PermSize=256m -XX:SurvivorRatio=8 -Xnoclassgc -XX:+AggressiveOpts
    # If you are having troubles on server shutdown (saving data),
    # add this to startup paramethers: "-Djava.util.logging.manager=com.l2jserver.L2LogManager". Example:
    # REM java "-Djava.util.logging.manager=com.l2jserver.util.L2LogManager" -Xmx1024m -cp "./../libs/*;l2jserver.jar" com.l2jserver.gameserver.GameServer
    # -------------------------------------
}
while ($LASTEXITCODE -like 2)

if ($LASTEXITCODE -like 1)
{
    "Server Terminated Abnormally";
}
else 
{
    "Server Terminated";
}