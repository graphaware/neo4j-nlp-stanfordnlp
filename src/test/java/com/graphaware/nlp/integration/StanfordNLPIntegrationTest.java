package com.graphaware.nlp.integration;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.configuration.SettingsConstants;
import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.stanford.StanfordTextProcessor;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.impl.proc.Procedures;
import org.reflections.Reflections;
import java.util.Set;

import static org.junit.Assert.*;

public class StanfordNLPIntegrationTest extends NLPIntegrationTest {

    @Override
    protected void registerProceduresAndFunctions(Procedures procedures) throws Exception {
        super.registerProceduresAndFunctions(procedures);
        Reflections reflections = new Reflections("com.graphaware.nlp.dsl");
        Set<Class<? extends AbstractDSL>> cls = reflections.getSubTypesOf(AbstractDSL.class);
        for (Class c : cls) {
            procedures.registerProcedure(c);
            procedures.registerFunction(c);
        }
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
        getNLPManager().getConfiguration().updateInternalSetting(SettingsConstants.DEFAULT_PIPELINE, TextProcessor.DEFAULT_PIPELINE);
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:\"customie\", stopWords:\"hello,build\", textProcessor:'"+ StanfordTextProcessor.class.getName() +"', processingSteps:{tokenize:true, dependency:true, coref: false}})", (result -> {
            assertTrue(result.hasNext());
        }));
        String text = "Neo4j is built from the ground up to be a graph database.";
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().annotateTextAndPersist(text, "test", StanfordTextProcessor.class.getName(), null, false, false);
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
        tester.assertNodesCount("NER_O", 0);
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
        tester.assertNodesCount("NER_Location", 1);
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

}
