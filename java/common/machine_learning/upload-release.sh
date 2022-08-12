#!/usr/bin/env bash

if [ -z "$1" ]
  then
    echo "No release tag supplied - please supply as argument, e.g., ./upload-release 1.7.1"
    exit 1
fi

TAG="v$1"
# clobber overwrites existing assets of the same name
BASE_COMMAND="gh release upload ${TAG} --clobber"

echo "Uploading to release tag ${TAG}..."

# TODO change to parsing the version tag to get the package name, below won't work once we get to package version 2.x
echo "Uploading main package file"
$BASE_COMMAND release-files/wekaDeeplearning4j-1*

echo "Uploading CUDA libs"
$BASE_COMMAND release-files/wekaDeeplearning4j-cuda*

echo "Uploading CUDA install & uninstall scripts"
$BASE_COMMAND release-files/install-cuda-libs*
$BASE_COMMAND release-files/uninstall-cuda-libs*


