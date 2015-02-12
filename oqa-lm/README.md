Language Model
==============
OQA uses a language model for scoring during inference. It uses the
[KenLM](https://kheafield.com/code/kenlm/) software for language modeling.
This sub-project has code for downloading KenLM, building KenLM, and using KenLM
to construct a language model from WikiAnswers. The OQA code accesses the KenLM
language model by querying an HTTP server started via a python script.

To build KenLM, you will need Boost installed and the corresponding environment 
variables set to the Boost `lib` and `include` paths. Using Homebrew on a Mac,
I had to set these environment variables:

    export LDFLAGS=-L/usr/local/Cellar/boost/1.57.0/lib
    export LD_LIBRARY_PATH=/usr/local/Cellar/boost/1.57.0/lib
    export CPLUS_INCLUDE_PATH=/usr/local/Cellar/boost/1.57.0/include

Below are the steps needed to get the language model component of OQA started.
Please look at the script sources if you are interested in what is going on
in more detail. Each step below depends on the previous step.

# Installing KenLM
Run `./src/main/scripts/install-kenlm.sh` to download and build KenLM.

**Gotcha:** If you receive an error like this after KenLM builds:

    error: [Errno 13] Permission denied: '/usr/local/lib/python2.7/dist-packages/kenlm.so'
    
then you need to run `sudo` to install the KenLM python bindings. To do this, run these steps:

1. `cd kenlm`
2. `sudo python setup.py install`

# Building the Language Model
You will need to have downloaded the OQA data in order to complete this step.
Run `./src/main/scripts/build-lm.sh` to build the language model from the
WikiAnswers data.

# Starting the Language Model Server
Run `./src/main/scripts/start.sh` to start the server. This will start an 
HTTP server on `localhost:9090`.

Check the log in `lm.err` for any errors. You may need to install the `web.py` python module.

# Stopping the Language Model Server
Run `./src/main/scripts/stop.sh` to stop the server.
