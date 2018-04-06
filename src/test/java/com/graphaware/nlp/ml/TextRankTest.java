package com.graphaware.nlp.ml;

import com.graphaware.nlp.StanfordNLPIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TextRankTest extends StanfordNLPIntegrationTest {

    private static final String TEXT1 = "On 8 May 2013, "
            + "one week before the Pakistani election, the third author, "
            + "in his keynote address at the Sentiment Analysis Symposium, "
            + "forecast the winner of the Pakistani election. The chart "
            + "in Figure 1 shows varying sentiment on the candidates for "
            + "prime minister of Pakistan in that election. The next day, "
            + "the BBC’s Owen Bennett Jones, reporting from Islamabad, wrote "
            + "an article titled “Pakistan Elections: Five Reasons Why the "
            + "Vote is Unpredictable,”1 in which he claimed that the election "
            + "was too close to call. It was not, and despite his being in Pakistan, "
            + "the outcome of the election was exactly as we predicted.";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        createDefaultStanfordPipeline();
    }

    @Test
    public void testTextRankProcedureWithStanford() {
        createDocumentAndAnnotate(TEXT1);
        executeInTransaction("MATCH (a:AnnotatedText) CALL ga.nlp.ml.textRank({annotatedText: a}) YIELD result RETURN count(*) AS c", (result -> {
            assertTrue(result.hasNext());
            assertTrue((long) result.next().get("c") > 0);
        }));
    }

}
