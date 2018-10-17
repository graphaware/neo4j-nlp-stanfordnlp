package com.graphaware.nlp.integration;

import com.graphaware.nlp.StanfordNLPIntegrationTest;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MultipleCustomNERTest extends StanfordNLPIntegrationTest {

    @Test
    public void testMultipleCustomNERCanBeLoaded() {
        String modelNasa = getClass().getClassLoader().getResource("nasa-model.gz").getPath();
        String modelCar = getClass().getClassLoader().getResource("car-model.gz").getPath();

        executeInTransaction("CALL ga.nlp.config.model.add('nasa-ner', $p0)", buildSeqParameters(modelNasa), emptyConsumer());
        executeInTransaction("CALL ga.nlp.config.model.add('car-ner', $p0)", buildSeqParameters(modelCar), emptyConsumer());

        // Create pipeline
        String addPipelineQuery = "CALL ga.nlp.processor.addPipeline({language:'en', textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', name: 'customNER', processingSteps: {tokenize: true, ner: true, sentiment: false, dependency: true, customNER: \"nasa-ner,car-ner\"}})";
        executeInTransaction(addPipelineQuery, emptyConsumer());

        // Import some texts
        String text = "Apollo 1, initially designated AS-204, was the first manned mission of the United States Apollo program, which had as its ultimate goal a manned lunar landing.";
        executeInTransaction("CREATE (n:Document) SET n.text = {text}", Collections.singletonMap("text", text), emptyConsumer());
        String text2 = "Opel will be the name under which Vivaro will be placed, but if reception goes well then hopefully Chevy will carry the name in the states. The van has 5 cubic meteres of cargo space and can hold payloads up to 750 kg–meaning a great delivery van in the city. Lithium ion battery packs power the van and are mounted in the floor–similar to the Volt. Travel is expected to last up to 60 miles on electricity alone; after the gas-powered motor will extend the range to 250 miles.";
        executeInTransaction("CREATE (n:Document) SET n.text = {text}", Collections.singletonMap("text", text2), emptyConsumer());
        String text3 = "We are convinced that we will get a fantastic reaction from the people who use such vehicles on a daily basis: Electric mobility will allow them to travel in city areas which are now off-limits to petrol and diesel-powered vehicles and the range-extender technology makes it possible to use an electric van for normal routine business,” says Chris Lacey, Executive Director, International Operations Opel/Vauxhall Commercial Vehicles.";
        executeInTransaction("CREATE (n:Document) SET n.text = {text}", Collections.singletonMap("text", text3), emptyConsumer());

        // Annotate
        executeInTransaction("MATCH (n:Document) CALL ga.nlp.annotate({text: n.text, id:id(n), pipeline: 'customNER', checkLanguage:false}) YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)", emptyConsumer());

        TestNLPGraph testNLPGraph = new TestNLPGraph(getDatabase());
        testNLPGraph.assertTagWithValueHasNE("Apollo 1", "MISSION");
        testNLPGraph.assertTagWithValueHasNE("Opel", "CAR_MARQUE");
    }

}
