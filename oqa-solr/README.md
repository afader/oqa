OQA Solr Components
===================
OQA uses [Apache Solr](http://lucene.apache.org/solr/) for storing, indexing,
and querying data. Three components of OQA are stored in Solr:

1. The knowledge base (called `triplestore`)
2. The query rewrite templates (called `relsyn`)
3. The paraphrase templates (called `paraphrase`)

This sub-project has code for starting/stopping solr instances and adding data
to the indexes. All of the steps below assume that the OQA data has already
been downloaded.

For simplicity, I have checked in the configured Solr instances for the above
three components in this repository. It is possible to scale this out using 
SolrCloud, but I have not included any code for doing so. The commands below 
will start one Solr server for each component.

# Starting the Solr Instances
To start the Solr instances, run `./src/main/scripts/start-all.sh`. This
will start the following Solr servers:

* [http://localhost:8983/solr/#/triplestore](http://localhost:8983/solr/#/triplestore)
* [http://localhost:8984/solr/#/relsyn](http://localhost:8984/solr/#/relsyn)
* [http://localhost:8985/solr/#/paraphrase](http://localhost:8985/solr/#/paraphrase)

**Gotcha:** Sometimes it takes a few minutes to start the Solr instances. Tail the `{triplestore, relsyn, paraphrase}.out` files in each Solr subdirectory for more information. If you see a message like `Waiting until we see more replicas up` then give it a bit. Also see [this stackoverflow post](http://stackoverflow.com/questions/15674529/solrcloud-replica-waiting-time-configuration) for more information.

# Stopping the Solr Instances
To stop the Solr instances, run `./src/main/scripts/stop-all.sh`.

# Indexing the Data
To index the data, first start the Solr instances. Due to the size of the data,
it is helpful to index only a part of the data first. Run the following script
to index a subset of the data: `./src/main/scripts/create-indexes-small.sh`.

If that completes, run this script to index the full data: 
`./src/main/scripts/create-indexes.sh`.
