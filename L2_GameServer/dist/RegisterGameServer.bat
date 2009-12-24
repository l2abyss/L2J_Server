@echo off
color 17
cls
java -Djava.util.logging.config.file=console.cfg -cp ./../libs/*;l2jserver.jar com.l2jserver.gsregistering.BaseGameServerRegister -c
exit