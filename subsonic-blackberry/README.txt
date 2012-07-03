http://javaandjava.blogspot.com/2010/05/ant-script-for-blackberry-development.html

Downloaded Ant 1.8.1
Downloaded bb-ant-tools 1.2.9
Use simultors in plugin: c:\progs\Eclipse\plugins\net.rim.ejde.componentpack5.0.0_5.0.0.25\components\simulator\9550.bat
### Downloaded BlackBerry Device Simulators v5.0.0.419 (9530)

Enable IntelliJ plugin "Ant support"
Created module subsonic-blackberry in IntelliJ.
Set module language level to 1.3
Added c:\progs\Eclipse\plugins\net.rim.ejde.componentpack5.0.0_5.0.0.25\components\lib\net_rim_api.jar as library in IntelliJ.
Attach javadoc c:\progs\Eclipse\plugins\net.rim.ejde.componentpack5.0.0_5.0.0.25\components\docs


To start sim:
fledge.exe /app=Jvm.dll /handheld=9550 /app-param=JvmDebugFile:log.txt

In IntelliJ: Add dummy run config which executes ant goal and tails log.txt