@Echo off
echo Apply Script: COPY
echo F|xcopy /y /s /f /q "%1" "%2"
echo F|xcopy /y /s /f /q "lint/in.jar" "release/latest/Origin-%3.jar"
echo Starting the Washing Machine
cd lint

echo Powerwash Cycle
java -Xmx4g -Xms1m -jar obfuscator.jar --jarIn in.jar --jarOut out.jar --config obf.json
echo F|xcopy /y /f /q "out.jar" "in.jar"

echo ZKM Rinse Cycle
java -Xmx4g -Xms1m -jar ZKM.jar script.zkm
echo F|xcopy /y /f /q "out/in.jar" "out.jar"
echo F|xcopy /y /f /q "out/out.jar" "out.jar"
echo F|xcopy /y /f /q "out.jar" "in.jar"

echo Rinse Cycle
java -Xmx4g -Xms1m -jar proguard.jar @proguard.conf
cd ..
echo F|xcopy /y /s /f /q "lint/out.jar" "release/latest/Iris-%3.jar"
echo F|xcopy /y /s /f /q "lint/mapping.txt" "release/latest/mapping-%3.txt"
cd release
echo F|xcopy /y /s /f /q /E "latest" "%3/"
rmdir /Q/S latest