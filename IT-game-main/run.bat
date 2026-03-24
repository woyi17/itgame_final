@echo off
cd /d "%~dp0"
call sbt-dist\bin\sbt.bat run
