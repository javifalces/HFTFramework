#!/usr/bin/env bash

if [[ -z "${WEKA_HOME}" ]]; then
  weka_home="${HOME}/wekafiles"
else
  weka_home="${WEKA_HOME}"
fi

if [[ ! -d "$weka_home/packages/wekaDeeplearning4j" ]]; then
  echo -e "Could not find $weka_home/packages/wekaDeeplearning4j. Is the wekaDeeplearning4j package installed?"
  exit 1
fi

rm $weka_home/packages/wekaDeeplearning4j/lib/*cuda-*
echo -e "Successfully removed the CUDA libraries from the wekaDeeplearning4j package!"
