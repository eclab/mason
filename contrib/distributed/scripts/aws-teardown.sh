#!/bin/bash
#Author: Haoling

runId="default"
if [[ -n "$1" ]]; then
	runId=$1
fi

instances=($(aws ec2 describe-instances \
		--filters "Name=tag:Owner,Values=$USER" \
			"Name=instance-state-name,Values=running" \
			"Name=tag:Name,Values=$runId" \
		--query 'Reservations[*].Instances[*].InstanceId' \
		--output text ))
if [[ -n $instances ]]; then
	aws ec2 terminate-instances --instance-ids ${instances[@]} --output text
fi

vids=($(aws ec2 describe-volumes \
		--filter "Name=tag:Name,Values=$runId" \
				"Name=tag:Owner,Values=$USER" \
				"Name=status,Values=available" \
		--query "Volumes[*].[VolumeId]" \
		--output text))
for vid in ${vids[@]}; do
	aws ec2 delete-volume --volume-id $vid
done
