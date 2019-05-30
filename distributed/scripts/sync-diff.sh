#!/bin/bash
# Author: Haoling

source aws-config

process() {
	rsync -azh --include '*.class' -e "ssh $ssh_opts" $MASON_DIR $1:$REMOTE_MASON_DIR
	ssh $ssh_opts $1 "jar -cf $JAR_NAME -C $REMOTE_MASON_DIR ."
}

make -C $MASON_DIR

echo "Sync start..."

while read line ; do
	host=($(echo $line))
	process $host &
	echo "-- [$!] rsync into $host"
done < $HF_PUBLIC

wait

echo "Sync done."
