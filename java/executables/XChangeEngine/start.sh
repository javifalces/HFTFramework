#!/bin/bash
cd "$(dirname "$0")"
nohup java -Duser.timezone=GMT -jar XChangeEngine.jar>stdout.log&
