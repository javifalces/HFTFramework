#!/bin/bash
cd "$(dirname "$0")"
nohup java -Duser.timezone=GMT -jar BinanceEngine.jar>stdout.log&
