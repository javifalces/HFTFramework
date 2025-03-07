#!/bin/bash
#export LAMBDA_LOGS_PATH=/home/tradeasystems/lambda_data/logs
cd "$(dirname "$0")"
nohup java -Duser.timezone=GMT -Xmx1g -jar InteractiveBrokersEngine.jar&
