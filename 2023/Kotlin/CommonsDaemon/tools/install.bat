set EXEC_DIR=%~dp0
echo %EXEC_DIR%
set CLASSPATH_DIR=%EXEC_DIR%lib\
echo %CLASSPATH_DIR%
set CLASSPATH=%EXEC_DIR%HelloWorldServiceLauncher.jar;%CLASSPATH_DIR%commons-daemon-1.1.0.jar;%CLASSPATH_DIR%commons-logging-1.2.jar;%CLASSPATH_DIR%\*;
echo %CLASSPATH%
set JVM_PATH="C:\Program Files\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin\server\jvm.dll"

set INSTALL_PATH=%EXEC_DIR%HelloWorldServiceLauncher.exe
echo %INSTALL_PATH%

%EXEC_DIR%HelloWorldServiceLauncher //IS//HelloWorldService --DisplayName="Hello World Service" --Description="Demo for Hello World Service" ^
        --Install %INSTALL_PATH% --Startup auto --Jvm %JVM_PATH% --StartMode jvm --StopMode=jvm ^
        --Classpath=%CLASSPATH% --StartClass com.sample.service.HelloWorldServiceLauncher --StartMethod start --StartParams Hello#World#Start ^
        --StopClass com.sample.service.HelloWorldServiceLauncher --StopMethod stop --StopParams Hello#World#Stop ^
        --LogPath=%EXEC_DIR%logs --LogLevel=DEBUG ^
        --StdOutput=auto --StdError=auto ^
