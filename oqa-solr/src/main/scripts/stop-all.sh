#!/bin/bash                                                                     
set -u                                                                          
set -e                                                                          
dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
$dir/stop.sh triplestore
$dir/stop.sh relsyn
$dir/stop.sh paraphrase
