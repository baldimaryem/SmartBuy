@echo off
rem Lance l'API SmartBuy (Spring Boot) sur la base Oracle locale (localhost:1521/orcl).
rem Double-cliquer sur ce fichier, ou le lancer depuis cmd. Ctrl+C pour arreter.
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Java\jdk-17.0.8"
set "PATH=%JAVA_HOME%\bin;%PATH%"
rem Compte administrateur (adresse reelle, deja inscrite et confirmee) : il recoit le role ADMIN au demarrage.
set "ADMIN_EMAIL=maryemba98@gmail.com"
"%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.10-bin\53h08a94dg6djh6umvruv7q564\apache-maven-3.9.10\bin\mvn.cmd" spring-boot:run
pause
