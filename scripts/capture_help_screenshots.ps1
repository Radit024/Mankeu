# Regenerates the Pusat Bantuan screenshots (res/drawable-nodpi/help_*.webp) on a running emulator.
# Run from the repo root:  powershell -ExecutionPolicy Bypass -File scripts\capture_help_screenshots.ps1
# Needs one emulator or device on adb. The test uses an in-memory database and restores display
# preferences, so the device's own Mankeu data is not touched.
$ErrorActionPreference = "Stop"
$adb = if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME "platform-tools\adb.exe" } else { "adb" }

.\gradlew.bat assembleDebug assembleDebugAndroidTest --console=plain -q
& $adb install -r app\build\outputs\apk\debug\app-debug.apk | Out-Null
& $adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk | Out-Null
$result = & $adb shell am instrument -w -e class com.mankeu.presentation.HelpScreenshotTest -e helpScreenshots true com.mankeu.test/androidx.test.runner.AndroidJUnitRunner
$result | Select-Object -Last 3
if (-not ($result -match "OK \(1 test\)")) { throw "Screenshot test failed" }

$target = "app\src\main\res\drawable-nodpi"
New-Item -ItemType Directory -Force $target | Out-Null
$files = & $adb shell run-as com.mankeu ls files/help
foreach ($name in $files) {
    $name = $name.Trim()
    if (-not $name.EndsWith(".webp")) { continue }
    $out = Join-Path $target $name
    # exec-out keeps the bytes binary; PowerShell redirection would re-encode them.
    cmd /c "`"$adb`" exec-out run-as com.mankeu cat files/help/$name > `"$out`""
    "{0,-40} {1,8:N0} bytes" -f $name, (Get-Item $out).Length
}
