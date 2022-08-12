

if (-not(Test-Path env:WEKA_HOME)) {
    $weka_home = Join-Path $(Get-Item Env:HOMEPATH).Value "wekafiles"
}
else
{
    $weka_home = $(Get-Item Env:WEKA_HOME).Value
}

$package_path=$(Join-Path $weka_home "packages" "wekaDeeplearning4j")
if (-not(Test-Path $package_path)) {
    Write-Output "Could not find $package_path. Is the wekaDeeplearning4j package installed?"
}
else
{
    Remove-Item $(Join-Path $package_path "lib" "*cuda-*")
}

Write-Output "Successfully removed the CUDA libraries from the wekaDeeplearning4j package!"

