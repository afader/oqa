#!/usr/bin/env bash
# Bash3 Boilerplate. Copyright (c) 2014, kvz.io

set -o errexit
set -o pipefail
set -o nounset
# set -o xtrace

# Set magic variables for current file & dir
__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if !(hash wget 2>/dev/null); then
  echo "Could not find wget."
  exit 1
fi

if !(hash python 2>/dev/null); then
  echo "Could not find python."
  exit 1
fi

kenlm_url="http://kheafield.com/code/kenlm.tar.gz"
lm_dir="${__dir}/../../../"

if [ ! -f "${lm_dir}/kenlm.tar.gz" ]; then
  wget -O "${lm_dir}/kenlm.tar.gz" "${kenlm_url}"
fi
cd ${lm_dir}
tar xvfz kenlm.tar.gz
cd kenlm
./bjam -j4
python setup.py install
