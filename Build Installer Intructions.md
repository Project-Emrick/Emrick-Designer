<h1>Build Installer Instructions</h1>

1) Find the ```build.gradle``` and ```build.bat``` files in the main directory of the project.
2) Change / Set the version in both files to the newest. Make sure they match in order to be displayed correctly everywhere.
3) Open powershell and cd into wherever the main directory is located. Ex: "C:\Purdue\Emrick\Emrick-Designer"
4) Run ```.\build.bat```
5) Wait until you see the green "BUILD SUCCESSFUL". This should take around 15 - 20 minutes depending on your PC.
6) The now compiled ```.exe``` file is located at: "Emrick-Designer\build\dist" open this location. It should be named Emrick Designer-[Version]
7) Go to the Emrick Designer homepage on GitHub.
8) Click "Releases" in the right panel.
9) Click "Draft a new release" near the top right of this screen.
10) Click "Choose a tag" and make a new one with the new Version Number.
11) Edit the "Release title" to be [Version]-beta
12) Drag and drop the Emrick Designer.exe file into the "Attach binaries by dropping them here or selecting them" box.
13) Ensure that the "Set as the latest release" box is checked.
14) Double check everything, then click the green "Publish Release" box.
15) All Done!
