<h1>Build Installer Instructions</h1>

1. Run ```make clean```
2. Execute gradle build command
3. Execute gradle jpackage command
4. Copy the src folder into ```build/dist/VERSION/Emrick-Designer```
4. In a terminal in the root directory of the project, run ```jpackage --type exe --app-image '.\build\dist\5.23\Emrick Designer' --name 'Emrick Designer' --app-version '5.23' -d '.\build\dist' --win-per-user-install --win-dir-chooser --win-shortcut```
5. The installer will be located at ```build/dist/Emrick-Designer-VERSION.exe```
