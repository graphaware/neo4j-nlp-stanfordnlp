GraphAware NLP Using StanfordNLP
==========================================

Getting the Software
---------------------

### Server Mode
When using Neo4j in the standalone <a href="http://docs.neo4j.org/chunked/stable/server-installation.html" target="_blank">standalone server</a> mode, you will need the <a href="https://github.com/graphaware/neo4j-framework" target="_blank">GraphAware Neo4j Framework</a> and <a href="https://github.com/graphaware/neo4j-nlp" target="_blank">GraphAware NLP</a>.jar files (both of which you can download here) dropped into the plugins directory of your Neo4j installation. Finally, the following needs to be appended to the `neo4j.conf` file in the `config/` directory:

```
  dbms.unmanaged_extension_classes=com.graphaware.server=/graphaware
  com.graphaware.runtime.enabled=true

  com.graphaware.module.NLP.2=com.graphaware.nlp.module.NLPBootstrapper
```

### For Developers
This package is an extention of the <a href="https://github.com/graphaware/neo4j-nlp" target="_blank">GraphAware NLP</a>, which therefore needs to be packaged and installed beforehand. No other dependencies required.

```
  cd neo4j-nlp
  mvn clean install

  cd ../neo4j-nlp-stanfordnlp
  mvn clean package
```


Introduction and How-To
-------------------------

The Stanford NLP library provides basic features for processing natural language text: sentence segmentation, tokenization, lemmatization, part-of-speach tagging, named entities identification, chunking, parsing and sentiment analysis. It is implemented by extending the general <a href="https://github.com/graphaware/neo4j-nlp" target="_blank">GraphAware NLP</a> package. A TextProcessor could be explicitly set to Stanford NLP, but it's not necessary as it's currently the default option.

### Tag Extraction / Annotations
```
#Annotate the news
MATCH (n:News)
CALL ga.nlp.annotate({text:n.text, id: n.uuid}) YIELD result
MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)
RETURN n, result
```

Available pipelines (through 'pipeline' argument of `ga.nlp.annotate()`:
  * `tokenizer` - tokenization, lemmatization, stop-words removal, part-of-speach tagging
  * `sentiment` - tokenization, sentiment analysis
  * `tokenizerAndSentiment` - tokenization, lemmatization, stop-words removal, part-of-speach tagging, sentiment analysis
  * `phrase` - tokenization, stop-words removal, relations, sentiment analysis

### Sentiment Analysis
The sentiment analysis can be run either as part of the annotation (see paragraph above) or as an independent procedure (see command below) which takes in AnnotatedText nodes, analyzes all attached sentences and adds to them a label corresponding to its sentiment.

```
MATCH (a:AnnotatedText {id: {id}})
CALL ga.nlp.sentiment({node:a}) YIELD result
MATCH (result)-[:CONTAINS_SENTENCE]->(s:Sentence)
RETURN labels(s) as labels
```

