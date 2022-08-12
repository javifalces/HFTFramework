# Commands required as of https://www.deps.co/guides/travis-ci-latest-java/
JAVA_FILE="${JAVA_HOME}/lib/security/cacerts"

if [ -f "$JAVA_FILE" ]; then
  sudo rm -f "${JAVA_HOME}/lib/security/cacerts"
  sudo ln -s /etc/ssl/certs/java/cacerts "${JAVA_HOME}/lib/security/cacerts"
else
  echo "Couldn't find java certificates folder, ignoring..."
fi