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


VERSION="1.0.3"
DIR="./build/dist/$VERSION/emrick-designer"
NAME="emrick-designer"

./gradlew clean
./gradlew jpackage

mkdir -p $DIR
cp -r ./src/main/resources "$DIR/res/"
echo "File Copied"
jpackage --type deb --app-image "$DIR" --name "$NAME" --app-version "$VERSION" -d "./build/dist" --file-associations "FAemrick.properties" --file-associations "FApacket.properties" --linux-package-name "$NAME"
