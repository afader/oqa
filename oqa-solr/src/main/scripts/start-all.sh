#!/bin/bash
set -u
set -e
dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
$dir/start.sh triplestore 8983
$dir/start.sh relsyn 8984
$dir/start.sh paraphrase 8985
