#!/bin/bash
set -u
set -e
name=$1
port=$2
__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
path="${__dir}/../../../${name}"
cd $path
nohup java -Xmx4G -Dbootstrap_confdir=$path/solr/$name/conf -Dcollection.configName=$name -DzkRun -DnumShards=1 -Djetty.home=$path -Djetty.port=$port -Dsolr.solr.home=$path/solr -jar start.jar > ${name}.out 2> ${name}.err &
EXIT_CODE=$?
ps -p $! > /dev/null
if [ $? -eq 0 ]; then
  echo $! > "${name}.pid"
  echo "Process forked, pid: $!"
else
  echo "Failed to start $name"
  exit 1
fi
exit $EXIT_CODE
