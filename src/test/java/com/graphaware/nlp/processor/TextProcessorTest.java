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
package com.graphaware.nlp.processor;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.domain.*;

import static com.graphaware.nlp.processor.stanford.StanfordTextProcessor.TOKENIZER;
import static org.junit.Assert.*;

import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.processor.stanford.StanfordTextProcessor;
import com.graphaware.nlp.util.ServiceLoader;
import com.graphaware.nlp.util.TestNLPGraph;
import com.graphaware.test.integration.EmbeddedDatabaseIntegrationTest;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;

public class TextProcessorTest extends EmbeddedDatabaseIntegrationTest {

    private NLPManager manager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.manager = new NLPManager(getDatabase(), NLPConfiguration.defaultConfiguration());
    }

    @Test
    public void testAnnotatedText() {
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        AnnotatedText annotatedText = textProcessor.annotateText("On 8 May 2013, "
                + "one week before the Pakistani election, the third author, "
                + "in his keynote address at the Sentiment Analysis Symposium, "
                + "forecast the winner of the Pakistani election. The chart "
                + "in Figure 1 shows varying sentiment on the candidates for "
                + "prime minister of Pakistan in that election. The next day, "
                + "the BBC’s Owen Bennett Jones, reporting from Islamabad, wrote "
                + "an article titled “Pakistan Elections: Five Reasons Why the "
                + "Vote is Unpredictable,”1 in which he claimed that the election "
                + "was too close to call. It was not, and despite his being in Pakistan, "
                + "the outcome of the election was exactly as we predicted.", "tokenizer", "en", null);

        assertEquals(4, annotatedText.getSentences().size());
        assertEquals(15, annotatedText.getSentences().get(0).getTags().size());
        assertEquals(11, annotatedText.getSentences().get(1).getTags().size());
        assertEquals(24, annotatedText.getSentences().get(2).getTags().size());
        assertEquals(9, annotatedText.getSentences().get(3).getTags().size());

        persistAnnotatedText(annotatedText, "test-1");
        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertTagWithValueHasNERLabel("Pakistan", "NER_Location");
        tester.assertTagWithValueHasNE("Pakistan", "LOCATION");
        tester.assertTagWithValueHasPos("show", "VBZ");
    }
    
    @Test
    public void testLemmaLowerCasing() {
        String testText = "Collibra’s Data Governance Innovation: Enabling Data as a Strategic Asset";
        
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        AnnotatedText annotateText = textProcessor.annotateText(testText, TOKENIZER, "en", null);

        assertEquals(1, annotateText.getSentences().size());
        assertEquals("governance", annotateText.getSentences().get(0).getTagOccurrence(16).getLemma());

        PipelineSpecification pipelineSpecification = new PipelineSpecification("tokenizeWithTrueCase", StanfordTextProcessor.class.getName());
        pipelineSpecification.addProcessingStep("truecase");
        textProcessor.createPipeline(pipelineSpecification);
        
        annotateText = textProcessor.annotateText(testText, "tokenizeWithTrueCase", "en", null);

        assertEquals(1, annotateText.getSentences().size());
        assertEquals("governance", annotateText.getSentences().get(0).getTagOccurrence(16).getLemma());
        
    }
    
    @Test
    public void testLemmaSprittingByPunctuation() {
        String testText = "Ser Emmon Cuy, Ser Robar Royce, Ser Parmen Crane, they'd sworn as well.";
        
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        AnnotatedText annotateText = textProcessor.annotateText(testText, TOKENIZER, "en", null);

        assertEquals(1, annotateText.getSentences().size());
        assertEquals(6, annotateText.getSentences().get(0).getTags().size());
    }

    private ResourceIterator<Object> getTagsIterator(String value) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("value", value);
        Result pakistan = getDatabase().execute("MATCH (n:Tag {value: {value}}) return n", params);
        ResourceIterator<Object> rowIterator = pakistan.columnAs("n");
        return rowIterator;
    }

    @Test
    public void testAnnotatedTag() {
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        Tag annotateTag = textProcessor.annotateTag("winners", "en");
        assertEquals(annotateTag.getLemma(), "winner");
    }

    @Test
    public void testAnnotationAndConcept() {
        // ConceptNet5Importer.Builder() - arguments need fixing
        /*TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        ConceptNet5Importer conceptnet5Importer = new ConceptNet5Importer.Builder("http://conceptnet5.media.mit.edu/data/5.4", textProcessor)
                .build();
        String text = "Say hi to Christophe";
        AnnotatedText annotateText = textProcessor.annotateText(text, 1, 0, "en", false);
        List<Node> nodes = new ArrayList<>();
        try (Transaction beginTx = getDatabase().beginTx()) {
            Node annotatedNode = annotateText.storeOnGraph(getDatabase(), false);
            Map<String, Object> params = new HashMap<>();
            params.put("id", annotatedNode.getId());
            Result queryRes = getDatabase().execute("MATCH (n:AnnotatedText)-[*..2]->(t:Tag) where id(n) = {id} return t", params);
            ResourceIterator<Node> tags = queryRes.columnAs("t");
            while (tags.hasNext()) {
                Node tag = tags.next();
                nodes.add(tag);
                List<Tag> conceptTags = conceptnet5Importer.importHierarchy(Tag.createTag(tag), "en");
                conceptTags.stream().forEach((newTag) -> {
                    nodes.add(newTag.storeOnGraph(getDatabase(), false));
                });
            }
            beginTx.success();
        }*/
    }

//    @Test
//    public void testSentiment() {
//        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
//
//        AnnotatedText annotateText = textProcessor.annotateText("I really hate to study at Stanford, it was a waste of time, I'll never be there again", 1, 1, "en", false);
//        assertEquals(1, annotateText.getSentences().size());
//        assertEquals(0, annotateText.getSentences().get(0).getSentiment());
//
//        annotateText = textProcessor.annotateText("It was really horrible to study at Stanford", 1, 1, "en", false);
//        assertEquals(1, annotateText.getSentences().size());
//        assertEquals(1, annotateText.getSentences().get(0).getSentiment());
//
//        annotateText = textProcessor.annotateText("I studied at Stanford", 1, 1, "en", false);
//        assertEquals(1, annotateText.getSentences().size());
//        assertEquals(2, annotateText.getSentences().get(0).getSentiment());
//
//        annotateText = textProcessor.annotateText("I liked to study at Stanford", 1, 1, "en", false);
//        assertEquals(1, annotateText.getSentences().size());
//        assertEquals(3, annotateText.getSentences().get(0).getSentiment());
//
//        annotateText = textProcessor.annotateText("I liked so much to study at Stanford, I enjoyed my time there, I would recommend every body", 1, 1, "en", false);
//        assertEquals(1, annotateText.getSentences().size());
//        assertEquals(4, annotateText.getSentences().get(0).getSentiment());
//    }
    
    @Test
    public void testAnnotatedTextWithPosition() {
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        PipelineSpecification specification = new PipelineSpecification("tokenizeWithTrueCase", StanfordTextProcessor.class.getName());
        specification.addProcessingStep("truecase");
        specification.addProcessingStep("sentiment");
        specification.addProcessingStep("coref");
        specification.addProcessingStep("relations");
        textProcessor.createPipeline(specification);
        AnnotatedText annotateText = textProcessor.annotateText("On 8 May 2013, "
                + "one week before the Pakistani election, the third author, "
                + "in his keynote address at the Sentiment Analysis Symposium, "
                + "forecast the winner of the Pakistani election. The chart "
                + "in Figure 1 shows varying sentiment on the candidates for "
                + "prime minister of Pakistan in that election. The next day, "
                + "the BBC’s Owen Bennett Jones, reporting from Islamabad, wrote "
                + "an article titled “Pakistan Elections: Five Reasons Why the "
                + "Vote is Unpredictable,”1 in which he claimed that the election "
                + "was too close to call. It was not, and despite his being in Pakistan, "
                + "the outcome of the election was exactly as we predicted.", "tokenizeWithTrueCase", "en", null);

        assertEquals(4, annotateText.getSentences().size());
        Sentence sentence1 = annotateText.getSentences().get(0);
        assertEquals(15, sentence1.getTags().size());
        
        assertNull(sentence1.getTagOccurrence(0));
        assertEquals("8 May 2013", sentence1.getTagOccurrence(3).getLemma());
        assertEquals("one week", sentence1.getTagOccurrence(15).getLemma());
        assertEquals("before", sentence1.getTagOccurrence(24).getLemma());
        assertEquals("third", sentence1.getTagOccurrence(59).getLemma());
        assertEquals("sentiment", sentence1.getTagOccurrence(103).getLemma());
        assertEquals("forecast", sentence1.getTagOccurrence(133).getLemma());
        assertNull(sentence1.getTagOccurrence(184));
        assertTrue(sentence1.getPhraseOccurrence(99).contains(new Phrase("the Sentiment Analysis Symposium")));
        assertTrue(sentence1.getPhraseOccurrence(103).contains(new Phrase("Sentiment")));
        assertTrue(sentence1.getPhraseOccurrence(113).contains(new Phrase("Analysis")));
        
        //his(76)-> the third author(54)
        assertTrue(sentence1.getPhraseOccurrence(55).get(1).getContent().equalsIgnoreCase("the third author"));
        Sentence sentence2 = annotateText.getSentences().get(1);
        assertEquals("chart", sentence2.getTagOccurrence(184).getLemma());
        assertEquals("figure", sentence2.getTagOccurrence(193).getLemma());
    }
    
    @Test
    public void testAnnotatedShortText() {
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        AnnotatedText annotateText = textProcessor.annotateText("Fixing Batch Endpoint Logging Problem", "tokenizer", "en", null);

        assertEquals(1, annotateText.getSentences().size());
        persistAnnotatedText(annotateText, "test-10");
        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertSentenceNodesCount(1);
        tester.assertTagNodesCount(5);
    }
    
    @Test
    public void testAnnotatedShortText2() {
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        AnnotatedText annotateText = textProcessor.annotateText("Importing CSV data does nothing", "tokenizer", "en", null);
        assertEquals(1, annotateText.getSentences().size());
    }

    @Test
    public void testAnnotatedQuestionWithNoStopwords() {
        String text = "What is in front of the Notre Dame Main Building?";
        PipelineSpecification specification = new PipelineSpecification("custom", StanfordTextProcessor.class.getName());
        specification.addProcessingStep("dependency");
        specification.setStopwords("start, starts");
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        textProcessor.createPipeline(specification);
        AnnotatedText annotatedText = textProcessor.annotateText(text, "custom", "en", null);

        assertEquals(1, annotatedText.getSentences().size());
        Sentence sentence = annotatedText.getSentences().get(0);
        assertEquals("be", sentence.getTagOccurrence(5).getLemma());
    }

    @Test
    public void testWithOneThousandthDollar() {
        String text = "monetary units of mill or one-thousandth of a dollar (symbol ₥)";
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        AnnotatedText annotatedText = textProcessor.annotateText(text, "tokenizer", "en", null);

        assertEquals(1, annotatedText.getSentences().size());
        Sentence sentence = annotatedText.getSentences().get(0);
        assertNotNull(sentence.getTagOccurrenceByTagValue("one-thousandth"));
    }

    @Test
    public void testPipelineWithCustomStopwordsDoNotAddNERDateToTheWord() {
        String text = "In addition to the dollar the coinage act officially established monetary units of mill or one-thousandth of a dollar (symbol ₥), cent or one-hundredth of a dollar (symbol ¢), dime or one-tenth of a dollar, and eagle or ten dollars, with prescribed weights and composition of gold, silver, or copper for each.";
        PipelineSpecification specification = new PipelineSpecification("custom", StanfordTextProcessor.class.getName());
        specification.addProcessingStep("dependency");
        specification.setStopwords("start, starts");
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        textProcessor.createPipeline(specification);
        AnnotatedText annotatedText = textProcessor.annotateText(text, "custom", "en", null);

        Sentence sentence = annotatedText.getSentences().get(0);
        Tag theTag = sentence.getTag("the");
        assertEquals("the", theTag.getLemma());
        assertFalse(theTag.getNeAsList().contains("Date"));

        persistAnnotatedText(annotatedText, "test-15");
        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.executeInTransaction("MATCH (n:Tag) WHERE n.value = 'the' RETURN n", (result -> {
            assertFalse( ((Node) result.next().get("n")).hasLabel(Label.label("NER_Date")));
        }));
    }

    @Test
    public void testTypedDependenciesAreFoundAndStored() {
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        AnnotatedText annotatedText = textProcessor.annotateText("Donald Trump flew yesterday to New York City", "tokenizer", "en", null);

    }
    
    @Test
    public void testIssueWithThe() {
        String text = "Unlike the Spanish milled dollar the U.S. dollar is based upon a decimal system of values.";
        PipelineSpecification specification = new PipelineSpecification("custom", StanfordTextProcessor.class.getName());
        specification.addProcessingStep("dependency");
        specification.setStopwords("start,starts");
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        textProcessor.createPipeline(specification);
        AnnotatedText annotatedText = textProcessor.annotateText(text, "custom", "en", null);

        assertEquals(1, annotatedText.getSentences().size());
        Sentence sentence = annotatedText.getSentences().get(0);
        assertEquals("be", sentence.getTagOccurrence(49).getLemma());
    }

    private void persistAnnotatedText(AnnotatedText annotatedText, String id) {
        try (Transaction tx = getDatabase().beginTx()) {
            manager.persistAnnotatedText(annotatedText, id, false);
            tx.success();
        }
    }
}
