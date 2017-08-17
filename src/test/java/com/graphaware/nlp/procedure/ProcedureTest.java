/*
 * Copyright (c) 2013-2016 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.procedure;

import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.module.NLPModule;
import com.graphaware.nlp.processor.TextProcessorProcedure;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import com.graphaware.test.integration.GraphAwareIntegrationTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class ProcedureTest extends GraphAwareIntegrationTest {

    private static final String TEXT = "On 8 May 2013, "
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

    private static final String TEXT_IT = "Questo è un semplice testo in italiano";
    private static final String TEXT_FR = "Ceci est un texte simple en français";

    private static final String SHORT_TEXT_1 = "You knew China's cities were growing. But the real numbers are stunning http://wef.ch/29IxY7w  #China";
    private static final String SHORT_TEXT_2 = "Globalization for the 99%: can we make it work for all?";
    private static final String SHORT_TEXT_3 = "This organisation increased productivity, happiness and trust with just one change http://wef.ch/29PeKxF ";
    private static final String SHORT_TEXT_4 = "In pictures: The high-tech villages that live off the grid http://wef.ch/29xuRh8 ";
    private static final String SHORT_TEXT_5 = "The 10 countries best prepared for the new digital economy http://wef.ch/2a8DNug ";
    private static final String SHORT_TEXT_6 = "This is how to limit damage to the #euro after #Brexit, say economists http://wef.ch/29GGVzG ";
    private static final String SHORT_TEXT_7 = "The office jobs that could see you earning nearly 50% less than some of your co-workers http://wef.ch/29P9biE ";
    private static final String SHORT_TEXT_8 = "Which nationalities have the best quality of life? http://wef.ch/29uDfwV";
    private static final String SHORT_TEXT_9 = "It’s 9,000km away, but #Brexit has hit #Japan hard http://wef.ch/29P92eQ  #economics";
    private static final String SHORT_TEXT_10 = "Which is the world’s fastest-growing large economy? Clue: it’s not #China http://wef.ch/29xuXFd  #economics";

    @Test
    public void overallTest() {
        GraphAwareRuntime gaRuntime = GraphAwareRuntimeFactory.createRuntime(getDatabase());
        gaRuntime.registerModule(new NLPModule("NLP", NLPConfiguration.defaultConfiguration(), getDatabase()));
        gaRuntime.start();
        gaRuntime.waitUntilStarted();
//        testAnnotatedText();
//        clean();
//        testAnnotatedTextWithSentiment();
//        clean();
//        testAnnotatedTextAndSentiment();
//        clean();
//        testAnnotatedTextOnMultiple();
//        clean();
//        testConceptText();
//        clean();
//        testLanguageDetection();
//        clean();
//        testSupportedLanguage();
//        clean();
//        testFilter();
//        clean();
//        testGetProceduresManagement();
//        clean();
//        testStopWords();
//        testTextRank();
        testGetPipelineInfos();
        clean();
    }

    private void clean() {
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute("MATCH (n) DETACH DELETE n");
            tx.success();
        }
    }

    public void testAnnotatedText() {
        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", TEXT);
            params.put("id", id);
            Result news = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}}) YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Node resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);
            params.clear();
            params.put("id", id);
            Result tags = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:CONTAINS_SENTENCE]->(s:Sentence)-[:HAS_TAG]->(result:Tag) RETURN result", params);
            rowIterator = tags.columnAs("result");
            assertTrue(rowIterator.hasNext());

            Result sentences = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:CONTAINS_SENTENCE]->(s:Sentence) RETURN labels(s) as result", params);
            rowIterator = sentences.columnAs("result");
            assertTrue(rowIterator.hasNext());
            int countSentence = 0;
            while (rowIterator.hasNext()) {
                List<Object> next = (List) rowIterator.next();
                assertEquals(next.size(), 1);
                countSentence++;
            }

            sentences = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:FIRST_SENTENCE|NEXT_SENTENCE*..]->(s:Sentence) RETURN labels(s) as result", params);
            rowIterator = sentences.columnAs("result");
            assertTrue(rowIterator.hasNext());
            int newCountSentence = 0;
            while (rowIterator.hasNext()) {
                List<Object> next = (List) rowIterator.next();
                assertEquals(next.size(), 1);
                newCountSentence++;
            }
            assertEquals(countSentence, newCountSentence);
            tx.success();
        }
    }

    public void testAnnotatedTextWithSentiment() {
        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", TEXT);
            params.put("id", id);
            Result news = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}, pipeline: \"tokenizerAndSentiment\"}) YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Node resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);
            params.clear();
            params.put("id", id);
            Result sentences = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:CONTAINS_SENTENCE]->(s:Sentence) RETURN labels(s) as result", params);
            rowIterator = sentences.columnAs("result");
            assertTrue(rowIterator.hasNext());
            while (rowIterator.hasNext()) {
                List<Object> next = (List) rowIterator.next();
                assertEquals(next.size(), 2);
            }
            tx.success();
        }
    }

    public void testAnnotatedTextAndSentiment() {
        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", TEXT);
            params.put("id", id);
            Result news = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}, store: true}) YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Node resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);
            params.clear();
            params.put("id", id);
            Result sentences = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}}) WITH a "
                    + "CALL ga.nlp.sentiment({node:a}) YIELD result "
                    + "MATCH (result)-[:CONTAINS_SENTENCE]->(s:Sentence) "
                    + "return labels(s) as labels", params);
            rowIterator = sentences.columnAs("labels");
            assertTrue(rowIterator.hasNext());
            int i = 0;
            while (rowIterator.hasNext()) {
                List<Object> next = (List) rowIterator.next();
                assertEquals(next.size(), 2);
                i++;
            }
            assertEquals(4, i);
            //Execute again for checking the number of senteces
            sentences = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}}) WITH a "
                    + "CALL ga.nlp.sentiment({node:a}) YIELD result "
                    + "MATCH (result)-[:CONTAINS_SENTENCE]->(s:Sentence) "
                    + "return labels(s) as labels", params);
            rowIterator = sentences.columnAs("labels");
            assertTrue(rowIterator.hasNext());
            i = 0;
            while (rowIterator.hasNext()) {
                List<Object> next = (List) rowIterator.next();
                assertEquals(next.size(), 2);
                i++;
            }
            assertEquals(4, i);
            tx.success();
        }
    }

    public void testAnnotatedTextOnMultiple() {
        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", SHORT_TEXT_1);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_2);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_3);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_4);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_5);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_6);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_7);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_8);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_9);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_10);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            getDatabase().execute("MERGE (n:Tweet {id:1})", params);

            //Test for filter based on language
            params.put("value", TEXT_IT);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            Result sentences = getDatabase().execute("MATCH (a:Tweet) WITH a\n"
                    + "WITH collect(a) AS aa\n"
                    + "UNWIND aa AS a\n"
                    + "CALL ga.nlp.annotate({text:a.text, id: id(a)}) YIELD result WITH result as at "
                    + "MERGE (a)-[:HAS_ANNOTATED_TEXT]->(at) WITH at "
                    + "MATCH (at)-[:CONTAINS_SENTENCE]->(result) "
                    + "RETURN result", params);
            ResourceIterator<Object> rowIterator = sentences.columnAs("result");
            assertTrue(rowIterator.hasNext());
            int i = 0;
            while (rowIterator.hasNext()) {
                rowIterator.next();
                i++;
            }
            assertEquals(13, i);
            tx.success();
        }
    }

    public void testConceptText() {
        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", TEXT);
            params.put("id", id);
            Result news = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}}) YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Node resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);
            params.clear();
            params.put("id", id);
            Result tags = getDatabase().execute(
                    "MATCH (a:AnnotatedText) "
                    + "CALL ga.nlp.concept({node:a, depth: 2}) YIELD result\n"
                    + "return result;", params);
            rowIterator = tags.columnAs("result");
            //assertTrue(rowIterator.hasNext());
            tx.success();
        }
    }

    public void testLanguageDetection() {
        try (Transaction tx = getDatabase().beginTx()) {
            Map<String, Object> params = new HashMap<>();
            params.put("value", TEXT);
            Result result = getDatabase().execute("CALL ga.nlp.language({text:{value}}) YIELD result\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = result.columnAs("result");
            assertTrue(rowIterator.hasNext());
            String resultNode = (String) rowIterator.next();
            assertEquals("en", resultNode);

            params.put("value", TEXT_IT);
            result = getDatabase().execute("CALL ga.nlp.language({text:{value}}) YIELD result\n"
                    + "return result", params);
            rowIterator = result.columnAs("result");
            assertTrue(rowIterator.hasNext());
            resultNode = (String) rowIterator.next();
            assertEquals("it", resultNode);

            params.put("value", TEXT_FR);
            result = getDatabase().execute("CALL ga.nlp.language({text:{value}}) YIELD result\n"
                    + "return result", params);
            rowIterator = result.columnAs("result");
            assertTrue(rowIterator.hasNext());
            resultNode = (String) rowIterator.next();
            assertEquals("fr", resultNode);

            tx.success();
        }
    }

    public void testSupportedLanguage() {
        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", TEXT_IT);
            params.put("id", id);
            Result news = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}}) YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertFalse(rowIterator.hasNext());
            tx.success();
        }
    }

    public void testFilter() {
        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", TEXT);
            params.put("filter", "Owen Bennett Jones/PERSON");
            Result news = getDatabase().execute("CALL ga.nlp.filter({text:{value}, filter: {filter}}) YIELD result\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Boolean resultNode = (Boolean) rowIterator.next();
            assertEquals(true, resultNode);

            params.clear();
            params.put("value", SHORT_TEXT_1);
            params.put("filter", "China/PERSON");
            news = getDatabase().execute("CALL ga.nlp.filter({text:{value}, filter: {filter}}) YIELD result\n"
                    + "return result", params);
            rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            resultNode = (Boolean) rowIterator.next();
            assertEquals(false, resultNode);

            params.clear();
            params.put("value", TEXT);
            params.put("filter", "Owen Bennett Jones/PERSON, BBC, Pakistan/LOCATION");
            news = getDatabase().execute("CALL ga.nlp.filter({text:{value}, filter: {filter}}) YIELD result\n"
                    + "return result", params);
            rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            resultNode = (Boolean) rowIterator.next();
            assertEquals(true, resultNode);
            tx.success();
        }
    }

    public void testGetProceduresManagement() {
        try (Transaction tx = getDatabase().beginTx()) {
            Result res = getDatabase().execute("CALL ga.nlp.getProcessors() YIELD class\n"
                    + "return class");
            ResourceIterator<Object> rowIterator = res.columnAs("class");
            assertTrue(rowIterator.hasNext());
            String resultNode = (String) rowIterator.next();
            assertEquals("com.graphaware.nlp.processor.stanford.StanfordTextProcessor", resultNode);
            tx.success();
        }
        try (Transaction tx = getDatabase().beginTx()) {
            Result res = getDatabase().execute("CALL ga.nlp.getPipelines({textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor'}) YIELD result\n"
                    + "return result");
            ResourceIterator<Object> rowIterator = res.columnAs("result");
            assertTrue(rowIterator.hasNext());
//            String resultNode = (String) rowIterator.next();
//            assertEquals("com.graphaware.nlp.processor.stanford.StanfordTextProcessor", resultNode);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            Result res = getDatabase().execute("CALL ga.nlp.addPipeline({"
                    + "textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', "
                    + "name: 'testPipe', "
                    + "stopWords: 'class,instance,issue', "
                    + "threadNumber: 5}) "
                    + "YIELD result\n"
                    + "return result");
            ResourceIterator<Object> rowIterator = res.columnAs("result");
            assertTrue(rowIterator.hasNext());
            String resultNode = (String) rowIterator.next();
//            assertEquals(TextProcessorProcedure.SUCCESS, resultNode);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            Result res = getDatabase().execute("CALL ga.nlp.getPipelines({textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor'}) YIELD result\n"
                    + "return result");
            ResourceIterator<Object> rowIterator = res.columnAs("result");

            boolean found = false;
            while (rowIterator.hasNext()) {
                String resultNode = (String) rowIterator.next();
                if (resultNode.equalsIgnoreCase("testPipe")) {
                    found = true;
                }
            }
            assertTrue(found);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", TEXT);
            params.put("id", id);
            Result res = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}, textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', pipeline: 'testPipe'}) YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = res.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Node resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);
            params.clear();
            params.put("id", id);
            Result tags = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:CONTAINS_SENTENCE]->(s:Sentence)-[:HAS_TAG]->(result:Tag) RETURN result", params);
            rowIterator = tags.columnAs("result");
            assertTrue(rowIterator.hasNext());

            Result sentences = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:CONTAINS_SENTENCE]->(s:Sentence) RETURN labels(s) as result", params);
            rowIterator = sentences.columnAs("result");
            assertTrue(rowIterator.hasNext());
            int countSentence = 0;
            while (rowIterator.hasNext()) {
                List<Object> next = (List) rowIterator.next();
                assertEquals(1, next.size());
                countSentence++;
            }

            sentences = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:FIRST_SENTENCE|NEXT_SENTENCE*..]->(s:Sentence) RETURN labels(s) as result", params);
            rowIterator = sentences.columnAs("result");
            assertTrue(rowIterator.hasNext());
            int newCountSentence = 0;
            while (rowIterator.hasNext()) {
                List<Object> next = (List) rowIterator.next();
                assertEquals(next.size(), 1);
                newCountSentence++;
            }
            assertEquals(countSentence, newCountSentence);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            Result res = getDatabase().execute("CALL ga.nlp.removePipeline({"
                    + "textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', "
                    + "pipeline: 'testPipe'}) "
                    + "YIELD result\n"
                    + "return result");
            ResourceIterator<Object> rowIterator = res.columnAs("result");
            assertTrue(rowIterator.hasNext());
            String resultNode = (String) rowIterator.next();
//            assertEquals(TextProcessorProcedure.SUCCESS, resultNode);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            Result res = getDatabase().execute("CALL ga.nlp.getPipelines({textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor'}) YIELD result\n"
                    + "return result");
            ResourceIterator<Object> rowIterator = res.columnAs("result");

            boolean found = false;
            while (rowIterator.hasNext()) {
                String resultNode = (String) rowIterator.next();
                if (resultNode.equalsIgnoreCase("testPipe")) {
                    found = true;
                }
            }
            assertTrue(!found);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            Result res = getDatabase().execute("CALL ga.nlp.addPipeline({"
                    + "textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', "
                    + "name: 'testPipe', "
                    + "stopWords: 'class,instance,issue', "
                    + "threadNumber: 5}) "
                    + "YIELD result\n"
                    + "return result");
            ResourceIterator<Object> rowIterator = res.columnAs("result");
            assertTrue(rowIterator.hasNext());
            String resultNode = (String) rowIterator.next();
//            assertEquals(TextProcessorProcedure.SUCCESS, resultNode);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            Result res = getDatabase().execute("CALL ga.nlp.getPipelines({textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor'}) YIELD result\n"
                    + "return result");
            ResourceIterator<Object> rowIterator = res.columnAs("result");

            boolean found = false;
            while (rowIterator.hasNext()) {
                String resultNode = (String) rowIterator.next();
                if (resultNode.equalsIgnoreCase("testPipe")) {
                    found = true;
                }
            }
            assertTrue(found);
            tx.success();
        }
    }

    public void testGetPipelineInfos() {
        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute("CALL ga.nlp.getPipelineInfos({textProcessor:'com.graphaware.nlp.processor.stanford.StanfordTextProcessor'})");
            assertTrue(result.hasNext());
            while (result.hasNext()) {
                System.out.println(result.next());
            }
            tx.success();
        }
    }

    public void testStopWords() {
        try (Transaction tx = getDatabase().beginTx()) {
            Result res = getDatabase().execute(
                    "CALL ga.nlp.addPipeline({"
                    + "textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', "
                    + "name: 'customStopWords', "
                    + "stopWords: '+,would,have,i,I,wish,nasa', "
                    + "checkLemmaIsStopWord: true, "
                    + "threadNumber: 20})\n"
                    + "YIELD result\n"
                    + "return result");
            ResourceIterator<Object> rowIterator = res.columnAs("result");
            assertTrue(rowIterator.hasNext());
            String resultNode = (String) rowIterator.next();
//            assertEquals(TextProcessorProcedure.SUCCESS, resultNode);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", "I wish i would do this at least one time, but still I Would not");
            params.put("id", id);
            Result news = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}, textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', pipeline:'customStopWords'}) YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Node resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);
            params.clear();
            params.put("id", id);
            Result tags = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:CONTAINS_SENTENCE]->(s:Sentence)-[:HAS_TAG]->(result:Tag) RETURN result.value as tag", params);
            rowIterator = tags.columnAs("tag");
            assertTrue(rowIterator.hasNext());
            int tagsCount = 0;
            while (rowIterator.hasNext()) {
                tagsCount++;
                String tagValue = (String) rowIterator.next();
                assertTrue(!tagValue.equalsIgnoreCase("would"));
                assertTrue(!tagValue.equalsIgnoreCase("have"));
                assertTrue(!tagValue.equalsIgnoreCase("i"));
                assertTrue(!tagValue.equalsIgnoreCase("wish"));
            }
            assertEquals(tagsCount, 5);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id2";
            Map<String, Object> params = new HashMap<>();
            params.put("value", "CIPSEA requires all federal agencies "
                    + "to protect data or information acquired by the "
                    + "agency under a pledge of confidentiality for exclusively "
                    + "statistical purposes from being disclosed in identifiable "
                    + "form.");
            params.put("id", id);
            Result news = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}, "
                    + "textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', "
                    + "pipeline:'customStopWords'}) "
                    + "YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Node resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);
            params.clear();
            params.put("id", id);
            Result tags = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:CONTAINS_SENTENCE]->(s:Sentence)-[:HAS_TAG]->(result:Tag) RETURN result.value as tag", params);
            rowIterator = tags.columnAs("tag");
            assertTrue(rowIterator.hasNext());
            int tagsCount = 0;
            while (rowIterator.hasNext()) {
                tagsCount++;
                String tagValue = (String) rowIterator.next();
                assertTrue(!tagValue.equalsIgnoreCase("be"));
            }
            assertEquals(tagsCount, 19);
            tx.success();
        }
    }

    public void testTextRank() {
        String myText = "Compatibility of systems of linear constraints over "
                + "the set of natural numbers. "
                + "Criteria of compatibility of a system of linear Diophantine "
                + "equations, strict inequations, and nonstrict inequations are "
                + "considered. Upper bounds for components of a minimal set of solutions "
                + "and algorithms of construction of minimal generating sets of solutions "
                + "for all types of systems are given. These criteria and the corresponding "
                + "algorithms for constructing a minimal supporting set of solutions "
                + "can be used in solving all the considered types systems and systems of mixed types.";

        List<String> expectedKeywords = Arrays.asList("linear constraints", "linear diophantine equations", "natural numbers", "nonstrict inequations", "strict inequations", "upper bounds");

        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", myText);
            params.put("id", id);

            Result news = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}}) YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Node resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);

            params.clear();
            params.put("id", id);

            Result tags = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:CONTAINS_SENTENCE]->(s:Sentence)-[:HAS_TAG]->(result:Tag) RETURN result", params);
            rowIterator = tags.columnAs("result");
            assertTrue(rowIterator.hasNext());

            tags = getDatabase().execute(
                    "MATCH (a:AnnotatedText) with a\n"
                    + "CALL ga.nlp.ml.textrank.compute({annotatedText:a}) YIELD result\n"
                    + "return result;", params);
            rowIterator = tags.columnAs("result");
            assertTrue(rowIterator.hasNext());
            tx.success();

            // evaluate results
            Result result = getDatabase().execute(
                "MATCH (k:Keyword)-[:DESCRIBES]->(a:AnnotatedText)\n"
                + "WHERE a.id = {id}\n"
                + "RETURN k.id AS id, k.value AS value\n", params);
            int totCount  = 0;
            int trueCount = 0;
            while (result!=null && result.hasNext()) {
                Map<String, Object> next = result.next();
                String tag = (String) next.get("value");
                totCount++;
                if (expectedKeywords.contains(tag))
                    trueCount++;
                assertTrue("Found unexpected keyword: " + tag, expectedKeywords.contains(tag));
            }
            assertEquals("Some keywords are missing.", expectedKeywords.size(), trueCount);
        }
    }
}
