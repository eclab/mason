#!/bin/bash
# Author : Haoling

source aws-config

make -C $MASON_DIR

jar -cf $JAR_NAME -C $MASON_DIR .

parallel-scp --hosts $HF_PUBLIC -x "-i $identity -o StrictHostKeyChecking=no" $JAR_NAME $REMOTE_HOME

rm $JAR_NAME
