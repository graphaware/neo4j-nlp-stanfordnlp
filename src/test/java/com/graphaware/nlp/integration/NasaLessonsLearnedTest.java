package com.graphaware.nlp.integration;

import com.graphaware.nlp.StanfordNLPIntegrationTest;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Ignore;
import org.junit.Test;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;

public class NasaLessonsLearnedTest extends StanfordNLPIntegrationTest {

    @Test
    @Ignore
    public void testNASACustomNER() {
        String modelsPath = getClass().getClassLoader().getResource("").getPath();
        executeInTransaction("CALL ga.nlp.config.model.workdir({p0})", buildSeqParameters(modelsPath), emptyConsumer());
        String text = "Apollo 1, initially designated AS-204, was the first manned mission of the United States Apollo program, which had as its ultimate goal a manned lunar landing.";

        String q = "CALL ga.nlp.processor.train({textProcessor: \"com.graphaware.nlp.processor.stanford.StanfordTextProcessor\", modelIdentifier: \"test-nasa-ner\", alg: \"ner\", inputFile: 'nasa-train.tsv', trainingParameters: {iter: 10}})";
        executeInTransaction(q, emptyConsumer());
        String t = "CALL ga.nlp.processor.test({textProcessor: \"com.graphaware.nlp.processor.stanford.StanfordTextProcessor\", modelIdentifier: \"test-nasa-ner\", alg: \"ner\", inputFile: 'nasa-test.tsv', trainingParameters: {iter: 10}})";
        executeInTransaction(t, emptyConsumer());

        // Create pipeline
        String addPipelineQuery = "CALL ga.nlp.processor.addPipeline({language:'en', textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', name: 'customNER', processingSteps: {tokenize: true, ner: true, sentiment: false, dependency: true, customNER: \"test-nasa-ner\"}})";
        executeInTransaction(addPipelineQuery, emptyConsumer());

        // Import some text
        executeInTransaction("CREATE (n:Document) SET n.text = {text}", Collections.singletonMap("text", text), emptyConsumer());

        // Annotate
        executeInTransaction("MATCH (n:Document) CALL ga.nlp.annotate({text: n.text, id:id(n), pipeline: 'customNER', checkLanguage:false}) YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)", emptyConsumer());

        executeInTransaction("MATCH (n:Tag) RETURN [x IN labels(n) | x] AS labels, n.value AS val", (result -> {
            while (result.hasNext()) {
                Map<String, Object> record = result.next();
                List<String> labels = (List<String>) record.get("labels");
                System.out.println(labels);
                System.out.println(record.get("val").toString());
            }
        }));
        // Check if some labels are there
        executeInTransaction("MATCH (n:NER_Mission) RETURN count(n) AS c", (result -> {
            assertTrue((long) result.next().get("c") > 0);
        }));
        TestNLPGraph testNLPGraph = new TestNLPGraph(getDatabase());
        testNLPGraph.assertTagWithValueHasNE("Apollo 1", "MISSION");


        // Check if some labels are there
        executeInTransaction("MATCH (n:NER_Mission) RETURN count(n) AS c", (result -> {
            assertTrue((long) result.next().get("c") > 0);
        }));
    }
}
