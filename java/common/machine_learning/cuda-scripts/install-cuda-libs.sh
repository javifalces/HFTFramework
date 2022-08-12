#!/usr/bin/env bash
cuda_version=$(nvcc --version | grep release | awk '{print($5)}' | cut -d',' -f1)
platform=''
# Get platform
unameOut="$(uname -s)"
case "${unameOut}" in
    Linux*)     platform=linux;;
    Darwin*)    platform=macosx;;
    *)          platform="UNSUPPORTED:${unameOut}"
esac

version='1.7.2'
zip_name="wekaDeeplearning4j-cuda-$cuda_version-$version-$platform-x86_64.zip"
selected_download=$1


if [[ ${cuda_version} != "10.0"  && ${cuda_version} != "10.1" && ${cuda_version} != "10.2" ]]; then
  echo -e "Could not detect CUDA version. Is CUDA installed?"
  exit 1
fi

if [[ -z "${WEKA_HOME}" ]]; then
  weka_home="${HOME}/wekafiles"
else
  weka_home="${WEKA_HOME}"
fi

if [[ ! -d "$weka_home/packages/wekaDeeplearning4j" ]]; then
  echo -e "Could not find $weka_home/packages/wekaDeeplearning4j. Is the wekaDeeplearning4j package installed?"
  exit 1
fi

# Check if user points to file
if [[ -f "$selected_download" ]]; then
  echo -e "Installing libraries from $selected_download. Skipping download ..."
  zip_name=${selected_download}
elif [[ -f "$zip_name" ]]; then
  echo -e "The file $zip_name already exists. Skipping download ..."
else
  # Download zip
  echo -e "Downloading $zip_name ..."
  wget -q --show-progress "https://github.com/Waikato/wekaDeeplearning4j/releases/download/v$version/$zip_name"
fi


echo -e "Extracting the CUDA libraries ..."
unzip -q ${zip_name} -d out
cp out/lib/* ${weka_home}/packages/wekaDeeplearning4j/lib/
rm -r out
echo -e "Successfully installed the CUDA libraries to the wekaDeeplearning4j package!"
echo -e "To remove the CUDA libraries, run the 'uninstall-cuda-libs.sh' script."
