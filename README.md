Where do I get the data?
===

## Knowledge Base (KB)
You can download the raw KB data at http://knowitall.cs.washington.edu/oqa/data/triplestore-raw/. The KB is separated into 20 LZO-compressed files. Each file contains one KB record per line. Each record is a tab-separated list of tab-separated (field name, field value) pairs. Each record is required to have field names `arg1`, `rel`, `arg2`, `namespace`, and `id`. For example, here is a record from Freebase:

    arg1\t5620 Jasonwheeler\trel\tNotable types\targ2\tAstronomical Discovery\targ1_fbid_s\t043r0w9\tid\tfb-60078571\tnamespace\tfreebase
    
## WikiAnswers Paraphrase Corpus
