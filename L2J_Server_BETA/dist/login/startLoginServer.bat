@echo off
title Login Server Console

:start
echo Starting L2J Login Server.
echo.

java -Xms128m -Xmx128m -cp ./../libs/*;l2jlogin.jar com.l2jserver.loginserver.L2LoginServer

if ERRORLEVEL 1 goto error
if ERRORLEVEL 2 goto restart
goto end

:error
echo.
echo Login Server terminated abnormally!
echo.
goto end

:restart
echo.
echo Admin Restarted Login Server.
echo.
goto start

:end
echo.
echo Login Server Terminated.
echo.
pause