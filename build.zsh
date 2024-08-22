#@echo off
#SET VERSION="6.17.0"
#SET DIR=".\build\dist\\%VERSION%\Emrick Designer"
#SET NAME="Emrick Designer"
#call .\gradlew.bat clean
#call .\gradlew.bat jpackage
#call xcopy /s/e .\src ""%DIR%"\src\" > nul
#echo Files Copied
#call jpackage --type exe --app-image %DIR% --name %NAME% --app-version "%VERSION%" -d ".\build\dist" --file-associations "FAemrick.properties" --file-associations "FApacket.properties" --win-per-user-install --win-dir-chooser --win-shortcut
#echo BUILD COMPLETE


VERSION="8.22.0"
DIR="./build/dist/$VERSION/Emrick Designer.app"
NAME="Emrick Designer"

./gradlew clean
./gradlew jpackage

cp -r ./src/main/resources "$DIR/res/"
echo "File Copied"
jpackage --type dmg --app-image "$DIR" --name "$NAME" --app-version "$VERSION" -d "./build/dist" --file-associations "FAemrick.properties" --file-associations "FApacket.properties" --mac-package-name "$NAME"
