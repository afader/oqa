# Running OQA
Create a file called `questions.txt` that has one question per line. Running
the following command will run OQA on the questions and write the output to
the directory `output/`:

    sbt 'run-main edu.knowitall.eval.qa.QASystemRunner questions.txt output'

Upon completion, `output/` will contain three files: `config.txt`, `output.txt`,
and `name.txt`. `config.txt` is a dump of the OQA settings used for this 
execution. `name.txt` contains the input file name. `output.txt` contains the
output of the system, with one line per prediction. Each line is a tab-separated
record with the following fields:

1. The input question.
2. The predicted answer.
3. The score of the highest-scoring derivation from question to predicted answer.
4. A string representation of the higest-scoring derivation.

By default, OQA uses the feature weights in `models/full.txt`. 

The configuration settings can be changed by editing 
`src/main/resources/application.conf`.
