Add-Type -AssemblyName System.IO.Compression.FileSystem
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# Get all necessary variables
$platform = "windows"
$version = "1.7.2"
$nvcc_output = $( nvcc --version )
$cuda_version = (($nvcc_output -split ",")[4] -split " ")[2]
$zip_name = "wekaDeeplearning4j-cuda-$cuda_version-$version-$platform-x86_64.zip"
$url = "https://github.com/Waikato/wekaDeeplearning4j/releases/download/v$version/$zip_name"
$output = $( Join-Path $PSScriptRoot $zip_name )

# Check if cuda_version could be detected
if ($cuda_version -ne "10.0" -and $cuda_version -ne "10.1" -and $cuda_version -ne "10.2")
{
    Write-Output "Could not detect CUDA version. Is CUDA installed?"
    exit
}

# Set weka_home
if (-not(Test-Path env:WEKA_HOME))
{
    $weka_home = Join-Path $( Get-Item Env:HOMEPATH ).Value "wekafiles"
}
else
{
    $weka_home = $( Get-Item Env:WEKA_HOME ).Value
}

# Check if package is installed
if (-not(Test-Path ([io.path]::combine($weka_home, "packages", "wekaDeeplearning4j"))))
{
    Write-Output "Could not find $weka_home/packages/wekaDeeplearning4j. Is the wekaDeeplearning4j package installed?"
    exit
}

# Check if zip is already downloaded
if ($args.length > 0)
{
    $zip_name = $args[0]
    Write-Output "Installing libraries from $zip_name. Skipping download ..."
}
elseif (Test-Path $zip_name)
{
    Write-Output "The file $zip_name already exists. Skipping download ..."
}
else
{
    # Download zip
    Write-Output "Downloading $zip_name now ..."
    $start_time = Get-Date
    $wc = New-Object System.Net.WebClient
    Invoke-WebRequest -Uri $url -OutFile $output
    Write-Output "Time taken: $( (Get-Date).Subtract($start_time).Seconds ) second(s)"
}


$out_dir = $( Join-Path $PSScriptRoot "out" )
# Unzip file
Write-Output "Extracting the CUDA libraries ..."
[System.IO.Compression.ZipFile]::ExtractToDirectory($output, $out_dir)
$copy_from = [io.path]::combine($out_dir, "lib", "*")
$copy_to = [io.path]::combine($weka_home, "packages", "wekaDeeplearning4j", "lib")
Copy-Item -Path $copy_from -Destination $copy_to
Remove-Item $out_dir -recurse
Write-Output "Successfully installed the CUDA libraries to the wekaDeeplearning4j package!"
Write-Output "To remove the CUDA libraries, run the 'uninstall-cuda-libs.sh' script."
