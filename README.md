This is a repository for the code and data from the paper _Open Question
Answering Over Curated and Extracted Knowledge Bases_ from KDD 2014. If you use
any of these resources in a published paper, please use the following citation:

    @inproceedings{Fader14,
        author    = {Anthony Fader and Luke Zettlemoyer and Oren Etzioni},
        title     = {{Open Question Answering Over Curated and Extracted
                    Knowledge Bases}},
        booktitle = {KDD},
        year      = {2014}
    }


Code
===
Warning: This project has lots of moving parts. It will probably take quite a
bit of effort to get it running.

## Dependencies
Below are the dependencies used for OQA. Version numbers are what I have used,
but other versions may be compatible.

* sbt (0.13)
* java (1.8.0)
* scala (2.10)
* Boost C++ libraries (1.5.7)
* Python (2.7.8)
* wget (1.15)

## Code Structure
OQA consists of the following components:

* Solr indexes (used for storing triples, paraphrases, and query rewrites).
* Language model (used for scoring answer derivation steps)
* Question answering code (used for inference and learning)

Getting the code running involves completing these steps in order:

1. [Downloading the data](oqa-data/) in `oqa-data/`
2. [Creating the indexes](oqa-solr/) in `oqa-solr/`
3. [Building the language model](oqa-lm/) in `oqa-lm/`
4. [Running the code](oqa-core/) in `oqa-core/`

Please follow the above links to the individual README files. Each README
will walk you through the steps.

Data
===
Below is a description of the data included with OQA.

## Knowledge Base (KB) Data
You can download the KB data at this url: 
http://knowitall.cs.washington.edu/oqa/data/kb. The KB is divided into 20
gzip-compressed files. The total compressed filesize is approximately 20GB; the
total decompressed filesize is approximately 50GB. 

Each file contains a newline-separated list of KB
records. Each record is a tab-separated list of (field name, field value) pairs.
For example, here is a record corresponding to a Freebase assertion (with tabs
replaced by newlines):

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

There is a total of 930 million records in the data. The distribution the 
different `namespace` values is:

| Namespace         | Count     |
|-------------------|----------:|
| Total             |930,143,872|
| ReVerb            |391,345,565|
| Freebase          |299,370,817|
| Probase           |170,278,429|
| Open IE 4.0       | 67,221,551|
| NELL              |  1,927,510|

## WikiAnswers Corpus
The WikiAnswers corpus contains clusters of questions tagged by WikiAnswers
users as paraphrases. Each cluster optionally contains an answer provided by
WikiAnswers users. There are 30,370,994 clusters containing an average of 25 
questions per cluster. 3,386,256 (11%) of the clusters have an answer.

The data can be downloaded from:
http://knowitall.cs.washington.edu/oqa/data/wikianswers/. The corpus is split
into 40 gzip-compressed files. The total compressed filesize is 8GB; the total
decompressed filesize is 40GB. Each file contains one cluster per line. Each
cluster is a tab-separated list of questions and answers. Questions are prefixed
by `q:` and answers are prefixed by `a:`. Here is an example cluster (tabs
replaced with newlines):

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

We also provide a hierarchical clustering of the lowercased tokens in the 
WikiAnswers corpus. We used [Percy Liang's implementation of the Brown 
Clustering Algorithm](https://github.com/percyliang/brown-cluster) with
1000 clusters (i.e. `--c 1000`). The raw output is available [here](https://s3-us-west-2.amazonaws.com/ai2-oqa/wikianswers-brown-clusters-c1000.txt.gz).
You can browse the clusters [here](https://s3-us-west-2.amazonaws.com/ai2-oqa/wikianswers-cluster-viewer/cluster_viewer.html).
We did not use these in the OQA system, but we probably should have.

## Paraphrase Template Data
The paraphrase templates used in OQA are available for download at
http://knowitall.cs.washington.edu/oqa/data/paraphrase-templates.txt.gz. The
file is 90M compressed and 900M decompressed. Each line in the file contains a
paraphrase template pair as a tab-separated list of (field name, field value)
pairs. Here is an example record (with tabs replaced with newlines):

    id  
    pair1718534
    template1
    how do people use $y ?
    template2 
    what be common use for $y ?
    typ
    anything
    count1
    0.518446
    count2
    0.335112
    typCount12
    0.195711
    count12
    0.195711
    typPmi
    0.707756
    pmi
    0.687842

Each template in a record is a space-delimited list of lowercased, lemmatized
tokens. The token `$y` is a variable representing the argument slot position. 
The numeric values in the records are scaled to be in [0, 1].

| Field         | Description                                               |
|---------------|-----------------------------------------------------------|
| `id`          | The unique identifier for the pair of templates           |
| `template1`   | The first template                                        |
| `template2`   | The second template                                       |
| `typ`         | Unusued field, ignore                                     |
| `count1`      | Log count of the first template                           |
| `count2`      | Log count of the second template                          |
| `typCount12`  | Unused field, ignore                                      |
| `count12`     | Log joint-count of the template pair                      |
| `typPmi`      | Unused field, ignore                                      |
| `pmi`         | Log pointwise mutual information of the template pair     |

There are a total of 5,137,558 records in the file.

## Query Rewrite Data
The query rewrite operators are available for download at
http://knowitall.cs.washington.edu/oqa/data/query-rewrites.txt.gz. The file is
1G compressed and 8G decompressed. Each line in the file is a tab-separated
list of (field name, field value) pairs. Here is an example record (with tabs
replaced with newlines):

    inverted
    0
    joint_count
    18
    marg_count1
    263
    marg_count2
    102
    pmi
    -7.30675508757
    rel1
    be the language of the country
    rel2
    be widely speak in

Each record has statistics computed over a pair of relation phrases `rel1` and
`rel2`. The relation phrases are lowercased and lemmatized. 

| Field         | Description                                               |
|---------------|-----------------------------------------------------------|
| `inverted`    | 1 if the rule inverts arg. order, 0 otherwise             |
| `joint_count` | The number of shared argument pairs in the KB             |
| `marg_count1` | The number of argument pairs `rel1` takes in the KB       |
| `marg_count2` | The number of argument pairs `rel2` takes in the KB       |
| `pmi`         | Log pointwise mutual information of `rel1` and `rel2`     |
| `rel1`        | Lemmatized, lowercased relation phrase 1                  |
| `rel2`        | Lemmatized, lowercased relation phrase 2                  |

There are a total of 74,461,831 records in the file.

## Labeled Question-Answer Pairs
The questions and answers used for the evaluation are available at
http://knowitall.cs.washington.edu/oqa/data/questions/. 

The questions are available in their own files:

* WebQuestions [train](http://knowitall.cs.washington.edu/oqa/data/questions/webquestions.train.txt) [devtest](http://knowitall.cs.washington.edu/oqa/data/questions/webquestions.devtest.txt) [test](http://knowitall.cs.washington.edu/oqa/data/questions/webquestions.test.txt) 
* TREC [train](http://knowitall.cs.washington.edu/oqa/data/questions/trec.train.txt) [devtest](http://knowitall.cs.washington.edu/oqa/data/questions/trec.devtest.txt) [test](http://knowitall.cs.washington.edu/oqa/data/questions/trec.test.txt)
* WikiAnswers [train](http://knowitall.cs.washington.edu/oqa/data/questions/wikianswers.train.txt) [devtest](http://knowitall.cs.washington.edu/oqa/data/questions/wikianswers.devtest.txt) [test](http://knowitall.cs.washington.edu/oqa/data/questions/wikianswers.test.txt)

I labeled the top predictions for each system as correct or incorrect if they
the predicted answer was not found in the label sets provided with WebQuestions,
TREC, and WikiAnswers. These labels can be found at
http://knowitall.cs.washington.edu/oqa/data/questions/labels.txt. The format of
this file is a newline-separated list of tab-separated (`LABEL`, truth value, 
question, answer) records. The questions and answers may be lowercased and
lemmatized.
