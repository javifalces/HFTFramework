#!/bin/bash
cd "$(dirname "$0")"
nohup java -Duser.timezone=GMT -jar MetatraderEngine.jar>stdout.log&
