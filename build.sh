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


VERSION="6.26.1"
DIR="./build/dist/$VERSION/Emrick-Designer"
NAME="Emrick-Designer"

./gradlew clean
./gradlew jpackage

mkdir -p $DIR
cp -r ./src "$DIR/src/"
echo "File Copied"
jpackage --type rpm --app-image "$DIR" --name "$NAME" --app-version "$VERSION" -d "./build/dist" --file-associations "FAemrick.properties" --file-associations "FApacket.properties" --linux-package-name "$NAME"
cd