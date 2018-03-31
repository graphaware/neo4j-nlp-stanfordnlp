package com.graphaware.nlp.integration;

import com.graphaware.nlp.StanfordNLPIntegrationTest;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SocialNetworkNERTest extends StanfordNLPIntegrationTest {

    @Test
    public void testCustomNER() {
        String modelsPath = getClass().getClassLoader().getResource("").getPath();
        executeInTransaction("CALL ga.nlp.config.model.workdir({p0})", buildSeqParameters(modelsPath), emptyConsumer());
        String text = "But then came the fake news, News Feed addiction, violence on Facebook  Live, cyberbullying, abusive ad targeting, election interference and, most recently, the Cambridge Analytica app data privacy scandals. All the while, Facebook either willfully believed the worst case scenarios could never come true, was naive to their existence or calculated the benefits and growth outweighed the risks. And when finally confronted, Facebook often dragged its feet before admitting the extent of the issues.\n" +
                "\n" +
                "Inside the social network’s offices, the bonds began to fray. An ethics problem metastisized into a morale problem. Slogans took on sinister second meanings. The Kool-Aid tasted different.";

        String q = "CALL ga.nlp.processor.train({textProcessor: \"com.graphaware.nlp.processor.stanford.StanfordTextProcessor\", modelIdentifier: \"test-ner\", alg: \"ner\", inputFile: 'social-network-train.tsv', trainingParameters: {iter: 10}})";
        executeInTransaction(q, emptyConsumer());
        String t = "CALL ga.nlp.processor.test({textProcessor: \"com.graphaware.nlp.processor.stanford.StanfordTextProcessor\", modelIdentifier: \"test-ner\", alg: \"ner\", inputFile: 'nasa-test.tsv', trainingParameters: {iter: 10}})";
//        executeInTransaction(t, emptyConsumer());

        // Create pipeline
        String addPipelineQuery = "CALL ga.nlp.processor.addPipeline({textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', name: 'customNER', processingSteps: {tokenize: true, ner: true, sentiment: false, dependency: true, customNER: \"test-ner\"}})";
        executeInTransaction(addPipelineQuery, emptyConsumer());

        // Import some text
        executeInTransaction("CREATE (n:Document) SET n.text = {text}", Collections.singletonMap("text", text), emptyConsumer());

        String text2 = "Even Twitter’s massive growth excitement for years was characterized by the challenge of finding mass sustained appeal — “Why say what I had for lunch?” Now, Snapchat’s massive growth with millions of passionate users is characterized by some persistent user misunderstanding of Snapchat use cases — some ask, “Why show something I did if it gets deleted?” And there are unexplored adaptations to broaden the appeal of Snapchat Stories and strengthen chat.";
        executeInTransaction("CREATE (n:Document) SET n.text = {text}", Collections.singletonMap("text", text2), emptyConsumer());

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
        TestNLPGraph testNLPGraph = new TestNLPGraph(getDatabase());
        testNLPGraph.assertTagWithValueHasNE("Facebook", "SOCIALNETWORK");
        testNLPGraph.assertTagWithValueHasNE("Snapchat", "SOCIALNETWORK");
        testNLPGraph.assertTagWithValueHasNE("Twitter", "SOCIALNETWORK");


//        // Check if some labels are there
//        executeInTransaction("MATCH (n:NER_Mission) RETURN count(n) AS c", (result -> {
//            assertTrue((long) result.next().get("c") > 0);
//        }));
    }
}
