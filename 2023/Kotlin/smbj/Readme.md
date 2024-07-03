## Build
```shell
gradlew shadowJar
```

## Example
```shell
set PASSWORD=secret
java -jar build/libs/smbj-1.0-SNAPSHOT-all.jar hostName userId "" sharedName folderName
```