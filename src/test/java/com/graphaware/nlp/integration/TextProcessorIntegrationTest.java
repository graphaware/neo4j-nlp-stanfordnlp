package com.graphaware.nlp.integration;

import com.graphaware.nlp.StanfordNLPIntegrationTest;
import com.graphaware.nlp.processor.stanford.StanfordTextProcessor;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TextProcessorIntegrationTest extends StanfordNLPIntegrationTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:'tokenizer',textProcessor:{processor}, processingSteps:{tokenize:true}})", Collections.singletonMap("processor", StanfordTextProcessor.class.getName()), emptyConsumer());
    }

    @Test
    public void testStanfordAnnotationViaProcedure() {
        clearDb();
        executeInTransaction("CALL ga.nlp.annotate({text:'Barack Obama is born in Hawaii. He is our president.', textProcessor:'"+StanfordTextProcessor.class.getName()+"' " +
                ", checkLanguage: true, pipeline: 'tokenizer', id:'test'}) YIELD result RETURN result", (result -> {
            assertTrue(result.hasNext());
        }) );
    }

    @Test
    public void testCustomPipelineWithoutPhraseStepShouldNotExtractPhrases() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:\"customie\", stopWords:\"hello,build\", textProcessor:'"+ StanfordTextProcessor.class.getName() +"', processingSteps:{tokenize:true, dependency:true, coref: true}})", (result -> {
            assertTrue(result.hasNext());
        }));
        String text = "Neo4j is built from the ground up to be a graph database.";
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().annotateTextAndPersist(text, "test", StanfordTextProcessor.class.getName(), "customie", false, false);
            tx.success();
        }
        executeInTransaction("MATCH (n:Phrase) RETURN n", (result -> {
            assertFalse(result.hasNext());
        }));
    }

    @Test
    public void testAnnotationWithPipelineFromUserConfig() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:\"customie\", stopWords:\"hello,build\", textProcessor:'"+ StanfordTextProcessor.class.getName() +"', processingSteps:{tokenize:true, dependency:true, coref: false}})", (result -> {
            assertTrue(result.hasNext());
        }));
        String text = "Neo4j is built from the ground up to be a graph database.";
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().annotateTextAndPersist(text, "test", StanfordTextProcessor.class.getName(), "customie", false, false);
            tx.success();
        }
        executeInTransaction("MATCH (n:Phrase) RETURN n", (result -> {
            assertFalse(result.hasNext());
        }));
    }

    @Test
    public void testAnnotationWithPipelineWithoutNERProcessingStep() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:\"customie\", stopWords:\"hello,build\", textProcessor:'"+ StanfordTextProcessor.class.getName() +"', processingSteps:{tokenize:true, ner: false}})", (result -> {
            assertTrue(result.hasNext());
        }));
        String text = "My name is John Doe and I work in Switzerland";
        executeInTransaction("CALL ga.nlp.annotate({text:'"+text+"', pipeline:'customie', id:'test', checkLanguage:false})", (result -> {
            assertTrue(result.hasNext());
        }));
        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertNodesCount("NER_Person", 0);
        tester.assertNodesCount("NER_Location", 0);
        tester.assertNodesCount("NER_O", 10);
    }

    @Test
    public void testAnnotationWithPipelineWithNERProcessingStep() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:\"customie\", stopWords:\"hello,build\", textProcessor:'"+ StanfordTextProcessor.class.getName() +"', processingSteps:{tokenize:true, ner: true}})", (result -> {
            assertTrue(result.hasNext());
        }));
        String text = "My name is John Doe and I work in Switzerland";
        executeInTransaction("CALL ga.nlp.annotate({text:'"+text+"', pipeline:'customie', id:'test', checkLanguage:false})", (result -> {
            assertTrue(result.hasNext());
        }));
        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertNodesCount("NER_Person", 1);
    }

    @Test
    public void testAnnotationWithPipelineWithExcludedNER() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:\"customie\", stopWords:\"hello,build\", textProcessor:'"+ StanfordTextProcessor.class.getName() +"', processingSteps:{tokenize:true, ner: true}, excludedNER:['LOCATION']})", (result -> {
            assertTrue(result.hasNext());
        }));
        String text = "My name is John Doe and I work in Switzerland";
        executeInTransaction("CALL ga.nlp.annotate({text:'"+text+"', pipeline:'customie', id:'test', checkLanguage:false})", (result -> {
            assertTrue(result.hasNext());
        }));
        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertNodesCount("NER_Person", 1);
        tester.assertNodesCount("NER_Location", 0);
    }

    // https://github.com/graphaware/neo4j-nlp/issues/53
    @Test
    public void testAnnotationWithCustomStopwords() {
        clearDb();
        executeInTransaction("CALL ga.nlp.annotate({text:'it starts with all but one',id:'test',pipeline:'tokenizer',checkLanguage:false})", emptyConsumer());
        String text = "det, of, vad, eller, sin, efter, i, varje, sådan, de, ditt, han, dessa, vi, med, då, den, mig, denna, ingen, under, henne, sådant, du, hade, vilken,".replaceAll(",", "");
        String stopwords = "sådan,själv, eller, dig, från, vilkas, dem, ett, varit, varför, att, era, som";
        String query = "CALL ga.nlp.processor.addPipeline({textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', name: 'customStopwords', stopWords: {stopwords} })";
        executeInTransaction(query, Collections.singletonMap("stopwords", stopwords), emptyConsumer());
        executeInTransaction("CALL ga.nlp.annotate({text: {text}, id:'issue-53', pipeline:'customStopwords', checkLanguage:false})", Collections.singletonMap("text", text), (result -> {
            assertTrue(result.hasNext());
        }));
        List<String> blacklist = Arrays.asList(stopwords.split(","));
        executeInTransaction("MATCH (n:Tag) RETURN n.value AS v", (result -> {
            while(result.hasNext()) {
                String v = result.next().get("v").toString();
                System.out.println(v);
                assertFalse(blacklist.contains(v));
            }
        }));
    }

    @Test
    public void testAnnotationWithRelationStep() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name: 'relationsXYZ', textProcessor: {p0}, processingSteps:{tokenize:true, ner:true, dependency: true, coref:true, phrase: true, relations:true}})", buildSeqParameters(StanfordTextProcessor.class.getName()), emptyConsumer());
        String text = "Barack Obama is an american politician. He was Born in Hawaï.";
        executeInTransaction("CALL ga.nlp.annotate({text: {p0}, checkLanguage: false, pipeline: 'relationsXYZ', id: 'rel-test', checkLanguage: false}) YIELD result RETURN result", buildSeqParameters(text), emptyConsumer());
        executeInTransaction("MATCH (n:Phrase) RETURN n", (result -> {
            assertTrue(result.hasNext());
            System.out.println(((Node) result.next().get("n")).getAllProperties());
        }));
    }

    @Test
    public void testAnnotationWithWhitelist() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name: 'whitelist', whitelist:'i,be,john,hello,ibm', textProcessor: {p0}, processingSteps:{tokenize:true, ner:true}})", buildSeqParameters(StanfordTextProcessor.class.getName()), emptyConsumer());
        String text = "Hello, my name is John and I worked at IBM.";
        executeInTransaction("CALL ga.nlp.annotate({text: {p0}, checkLanguage: false, pipeline: 'whitelist', id: 'rel-test', checkLanguage: false}) YIELD result RETURN result", buildSeqParameters(text), emptyConsumer());
        executeInTransaction("MATCH (n:Tag) RETURN n", (result -> {
            assertTrue(result.hasNext());
        }));
        TestNLPGraph nlpGraph = new TestNLPGraph(getDatabase());
        nlpGraph.assertTagWithValueExist("IBM");
        nlpGraph.assertTagWithValueDoesNotExist("work");
        nlpGraph.assertTagOccurrenceWithValueDoesNotExist("worked");
    }

    @Test
    public void testAnnotationWithVersionNumbers() {
        clearDb();
        executeInTransaction("CALL ga.nlp.annotate({text:'My machine run MacOS 10.13.5, when I upgraded Atom 1.28, restart atom get error.',id:'test',pipeline:'tokenizer',checkLanguage:false})", emptyConsumer());
        executeInTransaction("MATCH (n:Tag {value:'10.13.5'}) RETURN n", (result -> {
            assertTrue(result.hasNext());
        }));
    }
}
