package com.graphaware.nlp.integration;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.StanfordNLPIntegrationTest;
import com.graphaware.nlp.processor.stanford.StanfordTextProcessor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class PipelineLifecycleTest extends StanfordNLPIntegrationTest {

    private static final String STANFORD_PROCESSOR = StanfordTextProcessor.class.getName();

    @Test
    public void testRemovingAndAddingPipelineWithSameNameRecreatesANewStanfordPipelineCorrectly() {
        List<String> stopwords = Arrays.asList("have,use,can,should,from,i,when,we,you,can,what".split(","));
        clearDb();
        // When I create a pipeline with no stopwords defined
        String addFirstQuery = "CALL ga.nlp.processor.addPipeline({language:'en', \n" +
                "name:\"tickets\",  \n" +
                "textProcessor:\""+ STANFORD_PROCESSOR + "\",\n" +
                "excludedNER:['CAUSE_OF_DEATH'],\n" +
                "processingSteps:{tokenize:true, ner:true, dependency:true}})";
        executeInTransaction(addFirstQuery, emptyConsumer());

        // And I annotate some text
        loadSomeData();
        annotate();

        // Then there should be tags for stopwords like
        assertTrue(checkSomeStopwordsExist(stopwords));

        // And I remove the pipeline
        String removeQuery = "CALL ga.nlp.processor.removePipeline(\"tickets\",\"" + STANFORD_PROCESSOR + "\")";
        executeInTransaction(removeQuery, emptyConsumer());

        // And I re-create a pipeline with the same name but with custom stopwords

        String query = "CALL ga.nlp.processor.addPipeline({language:'en', \n" +
                "name:\"tickets\",  \n" +
                "textProcessor:\""+ STANFORD_PROCESSOR + "\",\n" +
                "excludedNER:['CAUSE_OF_DEATH'],\n" +
                "stopWords: '+,have,use,can,should,from,i,when,we,you,can,what', \n" +
                "processingSteps:{tokenize:true, ner:true, dependency:true}})";
        executeInTransaction(query, emptyConsumer());

        // And I annotate the same text
        clearDb();
        loadSomeData();
        annotate();

        // Then my stopwords should not be in there
        assertFalse(checkSomeStopwordsExist(stopwords));

    }

    private boolean checkSomeStopwordsExist(List<String> words) {
        List<Boolean> exists = new ArrayList<>();
        executeInTransaction("MATCH (n:Tag) WHERE n.value IN $p0 RETURN n", buildSeqParameters(words), (result) -> {
            exists.add(result.hasNext());
        });

        return exists.get(0);
    }

    private void annotate() {
        String q = "MATCH (n:Issue)\n" +
                "CALL ga.nlp.annotate({text: n.text, id: id(n), pipeline: \"tickets\", checkLanguage: false})\n" +
                "YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)";
        executeInTransaction(q, emptyConsumer());
    }

    private void loadSomeData() {
        String query = "MERGE (a:Product {name:\"Atom\"})\n" +
                "MERGE (i1:Issue {id:17557})\n" +
                "SET i1.text=\"Atom 1.28 crashes on macOS with Shadowsocks proxy\",\n" +
                "    i1.opened=\"Jun 22\",\n" +
                "       i1.user=\"spring-bu\",\n" +
                "    i1.status=\"Open\"\n" +
                "MERGE (i1a:Issue {id:17557, comment:1})\n" +
                "SET i1a.text=\"My machine run MacOS 10.13.5, when I upgraded Atom 1.28, restart atom get error.\",\n" +
                "    i1a.user=\"spring-bu\",\n" +
                "    i1a.date=\"Jun 22\"\n" +
                "MERGE (i1b:Issue {id:17557, comment:2})\n" +
                "SET i1b.text=\"Any ideas what's going wrong? We're seeing numerous reports from people that Atom 1.28 (Electron 2.0.3) is crashing when they are behind a Proxy. Judging from the dump above, there's any error with thread spawning originating in the proxy resolver.\",\n" +
                "    i1b.user=\"thomasjo\",\n" +
                "    i1b.date=\"Jun 22\"\n" +
                "MERGE (a)-[:HAS_ISSUE]->(i1)\n" +
                "MERGE (i1)-[:START_COMMENT]->(i1a)\n" +
                "MERGE (i1a)-[:NEXT_COMMENT]->(i1b)";
        executeInTransaction(query, emptyConsumer());
    }
}
