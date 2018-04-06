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

### Custom Named Entity Recognition

StanfordNLP allows to [train](https://nlp.stanford.edu/software/crf-faq.shtml) and use custom NE models. That requires large training dataset (in .tsv format) which is tokenized and labeled, for example:
```
The O
name    O
Apollo  MISSION
1   MISSION
,   O
chosen  O
by  O
the O
crew    O
,   O
was O
officially  O
retired O
by  O
NASA    O
in  O
commemoration   O
of  O
them    O
on  O
April   O
24  O
,   O
1967    O
.   O
```
(`O` is used as "no category", `MISSION` is a custom named entity that we want to learn to recognize)

Start with defining working directory (it contains train & test files and the model is going to be serialized to this location):
```
CALL ga.nlp.config.model.workdir("/Users/DrWho/workdir/data/nasa")
```

Model can be trained (and subsequently tested) by:
```
// first train a model
CALL ga.nlp.processor.train({textProcessor: "com.graphaware.nlp.processor.stanford.StanfordTextProcessor", alg: "ner", modelIdentifier: "nasa-missions", inputFile: "ner-missions.train.tsv"})

// after that you can test it
CALL ga.nlp.processor.test({textProcessor: "com.graphaware.nlp.processor.stanford.StanfordTextProcessor", alg: "ner", modelIdentifier: "nasa-missions", inputFile: "ner-missions.test.tsv"})
```
Parameters:
* `textProcessor`
* `alg`: algorith to train ("ner")
* `modelIdentifier`: choose a unique identifier which is going to be used to refer to the model
* `inputFile`: input train or test .tsv file (must be located in the working directory set by procedure `ga.nlp.config.model.workdir()`)

You can see the progress of the training in the neo4j log. It runs until convergence (there's no fixed number of iterations).

To test the model, prepare an independent test file of the same structure as the training file. The test procedure will return an overall precision, recall and F1 score. Details can be found in the neo4j log file. The testing procedure outputs the test data plus a third column representing predicted labels. It also prints out detailed statistics per class (yes, a single training file can define multiple NE classes).

Finally, to use the new model during annotation, define a custom pipeline with `customNER` processing step (you can provide multiple custom models separated by `,`, for example: `nasa-missions,nasa-chemicals`):
```
CALL ga.nlp.processor.addPipeline({name: 'customNER', textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', processingSteps: {tokenize: true, ner: true, sentiment: false, dependency: true, customNER: 'nasa-missions'}})
```
