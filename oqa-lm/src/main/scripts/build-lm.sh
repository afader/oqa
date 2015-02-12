#!/usr/bin/env bash                                                             
# Bash3 Boilerplate. Copyright (c) 2014, kvz.io                                 
                                                                                
set -o errexit                                                                  
set -o pipefail                                                                 
set -o nounset
__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

base=${__dir}/../../../
kenlm=${base}/kenlm
data_dir=${base}/../oqa-data/
data=${data_dir}/knowitall.cs.washington.edu/oqa/data/wikianswers/questions-normalized.txt.gz
output=${base}/questions

$kenlm/bin/lmplz -o 5 < "$data" > ${output}.arpa
$kenlm/bin/build_binary ${output}.arpa ${output}.binary
