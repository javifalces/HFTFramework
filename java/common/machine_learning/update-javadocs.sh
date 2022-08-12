#!/bin/bash
set -x

TMP_FOLDER="../temp_jdoc"

# Build the docs
./gradlew clean javaDoc
# Folder to copy docs into
mkdir $TMP_FOLDER
# Copy them out of the repo
cp -r build/docs/javadoc/* $TMP_FOLDER
# Switch to correct branch
git checkout gh-pages
# Replace old docs with newly built ones
rm -rf *
cp -r $TMP_FOLDER/* .
# Commit to the gh-pages branch
git add .
git commit -m 'Update javadoc'
git push

# Clean up
git checkout master
rm -rf $TMP_FOLDER

echo "JavaDocs update was successful!"
