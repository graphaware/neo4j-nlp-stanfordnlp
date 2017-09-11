package com.graphaware.nlp.integration;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.processor.PipelineInfo;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class StanfordNLPIntegrationTest extends NLPIntegrationTest {

    @Test
    public void testStanfordAnnotationViaProcedure() {
        clearDb();
        executeInTransaction("CALL ga.nlp.annotate({text:'Barack Obama is born in Hawaii. He is our president.', textProcessor:'stanford' " +
                ", checkLanguage: true, pipeline: 'tokenizer', id:'test'}) YIELD result RETURN result", (result -> {
                    assertTrue(result.hasNext());
        }) );
    }

    @Test
    public void testCustomPipelineIsRegistered() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:\"customie\", stopWords:\"hello,build\", textProcessor:\"stanford\", processingSteps:{tokenize:true, dependency:true, coref: true}})", (result -> {
            assertTrue(result.hasNext());
        }));
        final AtomicBoolean found = new AtomicBoolean(false);
        List<PipelineInfo> info = getNLPManager().getTextProcessorsManager().getTextProcessor("stanford").getPipelineInfos();
        info.forEach(pipelineInfo -> {
            if (pipelineInfo.getName().equals("customie")) {
                found.set(true);
            }
        });
        assertTrue(found.get());
    }

    @Test
    public void testCustomPipelineWithoutPhraseStepShouldNotExtractPhrases() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:\"customie\", stopWords:\"hello,build\", textProcessor:\"stanford\", processingSteps:{tokenize:true, dependency:true, coref: true}})", (result -> {
            assertTrue(result.hasNext());
        }));
        String text = "Neo4j is built from the ground up to be a graph database.";
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().annotateTextAndPersist(text, "test", "stanford", "customie", false, false);
            tx.success();
        }
        executeInTransaction("MATCH (n:Phrase) RETURN n", (result -> {
            assertFalse(result.hasNext());
        }));
    }

}
