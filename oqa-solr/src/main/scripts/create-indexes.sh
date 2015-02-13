#!/usr/bin/env bash                                                             
# Bash3 Boilerplate. Copyright (c) 2014, kvz.io                                 
                                                                                
__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
data_dir="${__dir}/../../../../oqa-data/knowitall.cs.washington.edu/oqa/data"
relsyn_data="${data_dir}/query-rewrites.txt.gz"
paraphrase_data="${data_dir}/paraphrase-templates.txt.gz"
kb_data_dir="${data_dir}/kb"

index_script="${__dir}/index.py"
index_relsyn_script="${__dir}/index-relsyn.py"

echo "Indexing paraphrase data..."
gunzip -c "$paraphrase_data" | python $index_script localhost 8985 paraphrase

echo "Indexing relsyn data..."
gunzip -c "$relsyn_data" | python $index_relsyn_script localhost 8984 

echo "Indexing triplestore data..."
for file in `ls "$kb_data_dir" | grep part-  `; do
  gunzip -c "${kb_data_dir}/${file}" | python $index_script localhost 8983 triplestore
done
