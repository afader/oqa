Data
===

# Knowledge Base (KB) Data
You can download the KB data at this url: 
http://knowitall.cs.washington.edu/oqa/data/kb. The KB is divided into 20
gzip-compressed files. The total compressed filesize is approximately 20GB; the
total uncompressed filesize is approximately 50GB. 

Each file contains a newline-separated list of KB
records. Each record is a tab-separated list of (field name, field value) pairs.
For example, here is a record corresponding to a Freebase assertion (with tabs
replaced by newlines for readability):

    arg1
    1,2-Benzoquinone
    rel
    Notable types
    arg2
    Chemical Compound
    arg1_fbid_s
    08s9rd
    id
    fb-179681780
    namespace
    freebase

The following fields names appear in the data:

| --------------------------|-------------------------------|-----------|
| Field Name                | Description                   | Required? |
| --------------------------|-------------------------------|-----------|
| `arg1`                    | Argument 1 of the triple      | Yes       |
| `rel`                     | Relation phrase of the triple | Yes       |
| `arg2`                    | Argument 1 of the triple      | Yes       |
| `id`                      | Unique ID for the triple      | Yes       |
| `namespace`               | The source of this triple     | Yes       |
| `arg1_fbid_s`             | Arg1 Freebase ID              | No        |
| `arg2_fbid_s`             | Arg2 Freebase ID              | No        |
| `num_extrs_i`             | Extraction redundancy         | No        |
| `conf_f`                  | Extractor confidence          | No        |
| `corpora_ss`              | Extractor corpus              | No        |
| `zipfSlope_f`             | Probase statistic             | No        |
| `entitySize_i`            | Probase statistic             | No        |
| `entityFrequency_i`       | Probase statistic             | No        |
| `popularity_i`            | Probase statistic             | No        |
| `freq_i`                  | Probase statistic             | No        |
| `zipfPearsonCoefficient_f`| Probase statistic             | No        |
| `conceptVagueness_f`      | Probase statistic             | No        |
| `prob_f`                  | Probase statistic             | No        |
| `conceptSize_i`           | Probase statistic             | No        |
| --------------------------|-------------------------------|-----------|

There is a total of 930 million records in the data. The distribution the 
different `namespace` values is:

|-------------------|-----------|
| `namespace` value | Count     |
|-------------------|-----------|
| Total             |930,143,872|
| Freebase          |299,370,817|
| ReVerb            |391,345,565|
| Probase           |170,278,429|
| Open IE 4.0       | 67,221,551|
| NELL              |  1,927,510|
|-------------------|-----------|

# WikiAnswers Corpus
The WikiAnswers corpus contains clusters of questions tagged by WikiAnswers
users as paraphrases. Each cluster optionally contains an answer provided by
WikiAnswers users. There are 30,370,994 clusters containing an average of 25 
questions per cluster. 3,386,256 (11%) of the clusters have an answer.

The data can be downloaded from:
http://knowitall.cs.washington.edu/oqa/data/wikianswers/. The corpus is split
into 40 gzip-compressed files. The total compressed filesize is 8GB; the total
uncompressed filesize is 40GB. Each file contains one cluster per line. Each
cluster is a tab-separated list of questions and answers. Questions are prefixed
by `q:` and answers are prefixed by `a:`. Here is an example cluster (tabs
replaced with newlines for readability):

    q:How many muslims make up indias 1 billion population?
    q:How many of india's population are muslim?
    q:How many populations of muslims in india?
    q:What is population of muslims in india?
    a:Over 160 million Muslims per Pew Forum Study as of October 2009.

This corpus is different than the data used in the Paralex system (see
http://knowitall.cs.washington.edu/paralex). First, it contains more questions
resulting from a longer crawl of WikiAnswers. Second, it groups questions into
clusters, instead of enumerating all pairs of paraphrases. Third, it contains
the answers, while the Paralex data does not.

# Paraphrase Templates Data

# Query Rewrite Data

# Labeled Question-Answer Pairs


