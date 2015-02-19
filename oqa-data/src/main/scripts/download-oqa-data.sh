#!/usr/bin/env bash
# Bash3 Boilerplate. Copyright (c) 2014, kvz.io

set -o errexit
set -o pipefail
set -o nounset
# set -o xtrace

# Set magic variables for current file & dir
__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

url="http://knowitall.cs.washington.edu/oqa/data"

output_prefix=${__dir}/../../../
wget_opts="-r --no-parent --accept '*.txt,*.gz' --directory-prefix ${output_prefix}"

if !(hash wget 2>/dev/null); then
  echo "Could not find wget."
  exit 1
fi

wget \
  --recursive \
  --no-parent \
  --accept '*.txt,*.gz' \
  --directory-prefix ${output_prefix} \
  "$url"

wget \
  -O ${output_prefix}/knowitall.cs.washington.edu/oqa/data/wikianswers/questions-normalized.txt.gz \
  'https://s3-us-west-2.amazonaws.com/ai2-oqa/questions-normalized.txt.gz' 

wget \
  -O ${output_prefix}/knowitall.cs.washington.edu/oqa/data/wikianswers/wikianswers-brown-clusters-c1000.txt.gz \
  'https://s3-us-west-2.amazonaws.com/ai2-oqa/wikianswers-brown-clusters-c1000.txt.gz'
