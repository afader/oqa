#!/bin/bash
dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
name=$1
echo cat ${dir}/../../../${name}/${name}.pid
kill `cat ${dir}/../../../${name}/${name}.pid`
