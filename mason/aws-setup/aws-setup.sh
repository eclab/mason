#!/bin/bash
# Author: Haoling

source aws-config

aws_hosts=()
aws_hosts_private=()

ts=$(date '+%Y%m%d-%H:%M')

# --------------------------------------------------

# Usage
# aws.setup.sh [# of instances] [name of this run]

if [[ -n "$1" ]]; then
	count=$1
fi

if [[ -n "$2" ]]; then
	runId=$2
fi

instances=($(aws ec2 describe-instances \
				--filter "Name=tag:Name,Values=$runId" \
						"Name=instance-state-name,Values=running" \
				--query 'Reservations[*].Instances[*].[InstanceId]' \
				--output text | tr '\n' ' '))

# Check if the number of instances with the given runId is less than the expected count
while [[ "${#instances[@]}" -lt "$count" ]]; do
	need=$(( $count - ${#instances[@]} ))

	# need to create more instances
	rid=$(aws ec2 run-instances \
		--image-id $ami \
		--security-group-ids $security_gid \
		--count $need \
		--instance-type $instance \
		--key-name $key \
		--subnet-id $subnet_id \
		--tag-specifications \
			"ResourceType=instance,Tags=[{Key=Owner,Value=$USER},{Key=Timestamp,Value=$ts},{Key=Name,Value=$runId}]" \
			"ResourceType=volume,Tags=[{Key=Owner,Value=$USER},{Key=Timestamp,Value=$ts},{Key=Name,Value=$runId}]" \
		--query "ReservationId" \
		--output text)
	if [[ -z "$rid" ]]; then
		echo "Unable to create instances. Retry in $INST_CREATE_DELAY seconds..."
	else
		echo "Creating $need $instance instances for $runId... ReservationId $rid"
	fi

	# Wait for a while and re-check
	sleep $INST_CREATE_DELAY
	instances=($(aws ec2 describe-instances \
				--filter "Name=tag:Name,Values=$runId" \
						"Name=instance-state-name,Values=running" \
				--query 'Reservations[*].Instances[*].[InstanceId]' \
				--output text | tr '\n' ' '))
done

echo "In total $count $instance have been created for $runId"

# Query instance public and private IPs
aws_hosts=($(aws ec2 describe-instances \
			--instance-ids ${instances[@]} \
			--query 'Reservations[*].Instances[*].[PublicIpAddress]' \
			--output text | tr '\n' ' '))
aws_hosts_private=($(aws ec2 describe-instances \
					--instance-ids ${instances[@]} \
					--query 'Reservations[*].Instances[*].[PrivateIpAddress]' \
					--output text | tr '\n' ' '))

if [[ "${#aws_hosts[@]}" -lt "$count" ]]; then
	echo "Got ${#aws_hosts[@]} ip addresses while $count are expected"
	echo "Something must be wrong. Exiting..."
	exit -1;
elif [[ "${#aws_hosts[@]}" -gt "$count" ]]; then
	echo "Got ${#aws_hosts[@]} ip addresses while $count are expected"
	echo "Will use the first $count instances" 
	aws_hosts=(${aws_hosts[@]:0:$count})
	aws_hosts_private=(${aws_hosts_private[@]:0:$count})
fi

echo -e "\e[1m\e[34mPublic IPs: \e[97m\e[21m" ; echo ${aws_hosts[@]}
echo -e "\e[1m\e[94mPrivate IPs: \e[97m\e[21m"; echo ${aws_hosts_private[@]}

# Sleep with countdown
secs=$INST_UP_DELAY
while [ $secs -gt 0 ]; do
   echo -ne "Wait $secs seconds for instances to launch...\033[0K\r"
   sleep 1
   : $((secs--))
done
echo ""

# Send SSH command to each instance
echo "Start file setup..."
PIDs=()
for host in ${aws_hosts[@]}; do
	# check if file setup has already completed
	if ssh $ssh_opts $host "stat $file_done_flag > /dev/null 2>&1"; then
		echo " ├─ $host already setup."
	else
		ssh $ssh_opts $host "$commands_per_host" &> /dev/null &
		PIDs+=($!)
		echo " ├─ [$!] ssh into $host ..."
	fi
done

# Wait for all ssh sessions to complete. Exit if error happens
for pid in ${PIDs[@]}; do
	wait $pid
	ret=$?
	if [[ ! $ret -eq 0 ]]; then
		echo " ├─ [$pid] ssh returned error code [$ret]. Exiting..."
		exit $ret
	else
		echo " ├─ [$pid] Done"
	fi
done

echo "File setup done."

# Setup pubkey login at master for mpi
echo "Setup pubkey at master ${aws_hosts[0]}..."

pubkey=$(ssh $ssh_opts ${aws_hosts[0]} "yes | ssh-keygen -f $REMOTE_HOME/.ssh/id_rsa -t rsa -N '' > /dev/null; cat $REMOTE_HOME/.ssh/id_rsa.pub")

echo " ├─ pubkey generated at master."

for h in ${aws_hosts[@]}; do
	ssh $ssh_opts $h "echo $pubkey >> $REMOTE_HOME/.ssh/authorized_keys"
	echo " ├─ pubkey transferred to $h"
done

echo " ├─ Testing connection at master..."
ssh $ssh_opts ${aws_hosts[0]} 'for h in '${aws_hosts_private[@]}'; do ssh -o StrictHostKeyChecking=no $h \"uptime\" > /dev/null ; done'

echo "Pubkey setup done."

# Create a hostfile for MPI at master with private IPs
printf '%s slots=1\n' "${aws_hosts_private[@]}" | ssh $ssh_opts ${aws_hosts[0]} "cat > $HF_PRIVATE"
# Add custom function 'mpijava' to the master
echo "$MPIJAVA" | ssh $ssh_opts ${aws_hosts[0]} "cat >> $REMOTE_HOME/.bashrc; . $REMOTE_HOME/.bashrc"

# Create a hostfile locally for parallel-scp with public IPs
printf "%s $REMOTE_USERNAME\n" "${aws_hosts[@]}" > $HF_PUBLIC

echo -e "\e[32m\e[1mAll done. \e[21m\e[97m"
echo -e "Ready to connect to master node \e[1m${aws_hosts[0]} \e[21m"
