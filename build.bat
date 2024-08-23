@echo off
SET VERSION="8.23.1"
SET DIR=".\build\dist\\%VERSION%\Emrick Designer"
SET NAME="Emrick Designer"
call .\gradlew.bat clean
call .\gradlew.bat jpackage
call xcopy /s/e .\src\main\resources ""%DIR%"\res\" > nul
echo Files Copied
call jpackage --type exe --app-image %DIR% --name %NAME% --app-version "%VERSION%" -d ".\build\dist" --file-associations "FAemrick.properties" --file-associations "FApacket.properties" --win-dir-chooser --win-shortcut
echo BUILD COMPLETE
