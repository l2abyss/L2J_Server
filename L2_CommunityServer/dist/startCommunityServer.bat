@echo off
title Community Server Console
echo Starting L2J Community Server.
echo.
java -Xms128m -Xmx128m -cp ./../libs/*;l2jcommunity.jar com.l2jserver.communityserver.L2CommunityServer
pause
