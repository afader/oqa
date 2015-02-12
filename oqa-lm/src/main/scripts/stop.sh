#!/bin/bash                                                                     
set -u                                                                          
set -e                                                                          
__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"                           
path="${__dir}/../../../"
pid=`cat $path/lm.pid`
kill $pid
