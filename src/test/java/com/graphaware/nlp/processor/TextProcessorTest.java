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

import com.graphaware.nlp.domain.*;

import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.processor.stanford.StanfordTextProcessor;
import com.graphaware.nlp.util.TestAnnotatedText;

import java.util.*;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static com.graphaware.nlp.util.TagUtils.newTag;

public class TextProcessorTest {

    private static TextProcessor textProcessor;
    private static PipelineSpecification PIPELINE_DEFAULT;

    @BeforeClass
    public static void init() {
        textProcessor = new StanfordTextProcessor();
        textProcessor.init();
        Map<String, Object> processingSteps = new HashMap<>();
        processingSteps.put(AbstractTextProcessor.STEP_TOKENIZE, true);
        processingSteps.put(AbstractTextProcessor.STEP_NER, true);
        PipelineSpecification pipelineSpecification = new PipelineSpecification("default", "en", StanfordTextProcessor.class.getName(), processingSteps, null, 1L, Collections.emptyList(), Collections.emptyList());
        PIPELINE_DEFAULT = pipelineSpecification;
        textProcessor.createPipeline(PIPELINE_DEFAULT);
    }

    @Test
    public void testAnnotatedText() {
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
                + "the outcome of the election was exactly as we predicted.", PIPELINE_DEFAULT);

        TestAnnotatedText test = new TestAnnotatedText(annotatedText);
        test.assertSentencesCount(4);
        test.assertTagsCountInSentence(17, 0);
        test.assertTagsCountInSentence(11, 1);
        test.assertTagsCountInSentence(25, 2);
        test.assertTagsCountInSentence(8, 3);

        test.assertTag(newTag("Pakistan", Collections.singletonList("LOCATION"), Collections.emptyList()));
        test.assertTag(newTag("show", Collections.emptyList(), Collections.singletonList("VBZ")));
    }
    
    @Test
    public void testLemmaLowerCasing() {
        String testText = "Collibra’s Data Governance Innovation: Enabling Data as a Strategic Asset";
        AnnotatedText annotatedText = textProcessor.annotateText(testText, PIPELINE_DEFAULT);
        TestAnnotatedText test = new TestAnnotatedText(annotatedText);

        test.assertSentencesCount(1);
        assertEquals("governance", test.getTagAtPosition(0, 16).getLemma());

        PipelineSpecification pipelineSpecification = new PipelineSpecification("tokenizeWithTrueCase", "en", StanfordTextProcessor.class.getName());
        pipelineSpecification.addProcessingStep("truecase");
        textProcessor.createPipeline(pipelineSpecification);
        annotatedText = textProcessor.annotateText(testText, pipelineSpecification);

        test = new TestAnnotatedText(annotatedText);
        test.assertSentencesCount(1);
        assertEquals("governance", test.getTagAtPosition(0, 16).getLemma());
        assertEquals("Governance", test.getTagOccurrenceAtPosition(0, 16).getValue());
        
    }
    
    @Test
    public void testLemmaSplittingByPunctuation() {
        String testText = "Ser Emmon Cuy, Ser Robar Royce, Ser Parmen Crane, they'd sworn as well.";
        AnnotatedText annotateText = textProcessor.annotateText(testText, PIPELINE_DEFAULT);

        assertEquals(1, annotateText.getSentences().size());
        assertEquals(6, annotateText.getSentences().get(0).getTags().size());
    }

    @Test
    public void testAnnotatedTag() {
        Tag annotateTag = textProcessor.annotateTag("winners", PIPELINE_DEFAULT);
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

    @Test
    public void testSentiment() {
        //@todo @alenegro81 check why failing and what is the logic behind the values
//
//        AnnotatedText annotateText = textProcessor.annotateText("I really hate to study at Stanford, it was a waste of time, I'll never be there again", "tokenizer", "en", null);
//        assertEquals(1, annotateText.getSentences().size());
//        assertEquals(0, annotateText.getSentences().get(0).getSentiment());
//
//        annotateText = textProcessor.annotateText("It was really horrible to study at Stanford", "tokenizer", "en", null);
//        assertEquals(1, annotateText.getSentences().size());
//        assertEquals(1, annotateText.getSentences().get(0).getSentiment());
//
//        annotateText = textProcessor.annotateText("I studied at Stanford", "tokenizer", "en", null);
//        assertEquals(1, annotateText.getSentences().size());
//        assertEquals(2, annotateText.getSentences().get(0).getSentiment());
//
//        annotateText = textProcessor.annotateText("I liked to study at Stanford", "tokenizer", "en", null);
//        assertEquals(1, annotateText.getSentences().size());
//        assertEquals(3, annotateText.getSentences().get(0).getSentiment());
//
//        annotateText = textProcessor.annotateText("I liked so much to study at Stanford, I enjoyed my time there, I would recommend every body", "tokenizer", "en", null);
//        assertEquals(1, annotateText.getSentences().size());
//        assertEquals(4, annotateText.getSentences().get(0).getSentiment());
    }
    
    @Test
    public void testAnnotatedTextWithPosition() {
        PipelineSpecification specification = new PipelineSpecification("positionTest", "en", StanfordTextProcessor.class.getName());
        specification.addProcessingStep("truecase");
        specification.addProcessingStep("sentiment");
        specification.addProcessingStep("coref");
//        specification.addProcessingStep("relations");
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
                + "the outcome of the election was exactly as we predicted.", specification);

        assertEquals(4, annotateText.getSentences().size());
        Sentence sentence1 = annotateText.getSentences().get(0);
        assertEquals(17, sentence1.getTags().size());
        
        assertNull(sentence1.getTagOccurrence(0));
        assertEquals("8", sentence1.getTagOccurrence(3).getLemma());
        assertEquals("one", sentence1.getTagOccurrence(15).getLemma());
        assertEquals("before", sentence1.getTagOccurrence(24).getLemma());
        assertEquals("third", sentence1.getTagOccurrence(59).getLemma());
        assertEquals("sentiment", sentence1.getTagOccurrence(103).getLemma());
        assertEquals("forecast", sentence1.getTagOccurrence(133).getLemma());
        assertNull(sentence1.getTagOccurrence(184));
        System.out.println(" >>> n_phrases = " + sentence1.getPhraseOccurrences().size());
        //assertTrue(sentence1.getPhraseOccurrence(99).contains(new Phrase("the Sentiment Analysis Symposium")));
        //assertTrue(sentence1.getPhraseOccurrence(103).contains(new Phrase("Sentiment")));
        //assertTrue(sentence1.getPhraseOccurrence(113).contains(new Phrase("Analysis")));
        
        //his(76)-> the third author(54)
        //assertTrue(sentence1.getPhraseOccurrence(55).get(1).getContent().equalsIgnoreCase("the third author"));
        Sentence sentence2 = annotateText.getSentences().get(1);
        assertEquals("chart", sentence2.getTagOccurrence(184).getLemma());
        assertEquals("figure", sentence2.getTagOccurrence(193).getLemma());
    }
    
    @Test
    public void testAnnotatedShortText() {
        AnnotatedText annotatedText = textProcessor.annotateText("Fixing Batch Endpoint Logging Problem", PIPELINE_DEFAULT);

        assertEquals(1, annotatedText.getSentences().size());
        TestAnnotatedText test = new TestAnnotatedText(annotatedText);
        test.assertSentencesCount(1);
        test.assertTagsCountInSentence(5, 0);
    }
    
    @Test
    public void testAnnotatedShortText2() {
        AnnotatedText annotateText = textProcessor.annotateText("Importing CSV data does nothing", PIPELINE_DEFAULT);
        assertEquals(1, annotateText.getSentences().size());
    }

    @Test
    public void testAnnotatedQuestionWithNoStopwords() {
        PipelineSpecification specification = new PipelineSpecification("question-no-sw", "en", StanfordTextProcessor.class.getName());
        specification.addProcessingStep("dependency");
        specification.setStopWords("start, starts");
        textProcessor.createPipeline(specification);
        String text = "What is in front of the Notre Dame Main Building?";
        AnnotatedText annotatedText = textProcessor.annotateText(text, specification);

        assertEquals(1, annotatedText.getSentences().size());
        Sentence sentence = annotatedText.getSentences().get(0);
        assertEquals("be", sentence.getTagOccurrence(5).getLemma());
    }

    @Test
    public void testWithOneThousandthDollar() {
        String text = "monetary units of mill or one-thousandth of a dollar (symbol ₥)";
        AnnotatedText annotatedText = textProcessor.annotateText(text, PIPELINE_DEFAULT);

        assertEquals(1, annotatedText.getSentences().size());
        Sentence sentence = annotatedText.getSentences().get(0);
        assertNotNull(sentence.getTagOccurrenceByTagValue("one-thousandth"));
    }

    @Test
    public void testPipelineWithCustomStopwordsDoNotAddNERDateToTheWord() {
        String text = "In addition to the dollar the coinage act officially established monetary units of mill or one-thousandth of a dollar (symbol ₥), cent or one-hundredth of a dollar (symbol ¢), dime or one-tenth of a dollar, and eagle or ten dollars, with prescribed weights and composition of gold, silver, or copper for each.";
        PipelineSpecification specification = new PipelineSpecification("customx", "en", StanfordTextProcessor.class.getName());
        specification.addProcessingStep("dependency");
        specification.setStopWords("start, starts");
        textProcessor.createPipeline(specification);
        AnnotatedText annotatedText = textProcessor.annotateText(text, specification);

        TestAnnotatedText test = new TestAnnotatedText(annotatedText);
        test.assertTagWithLemma("the");
        test.assertNotTag(newTag("the", Collections.singletonList("DATE"), Collections.emptyList()));
    }

    @Test
    public void testTypedDependenciesAreFound() {
        AnnotatedText annotatedText = textProcessor.annotateText("Donald Trump flew yesterday to New York City", PIPELINE_DEFAULT);
        // @todo add some test here

    }
    
    @Test
    public void testIssueWithBe() {
        PipelineSpecification specification = new PipelineSpecification("issue-be", "en", StanfordTextProcessor.class.getName());
        specification.addProcessingStep("dependency");
        specification.setStopWords("start, starts");
        textProcessor.createPipeline(specification);
        String text = "Unlike the Spanish milled dollar the U.S. dollar is based upon a decimal system of values.";
        AnnotatedText annotatedText = textProcessor.annotateText(text, specification);

        assertEquals(1, annotatedText.getSentences().size());
        Sentence sentence = annotatedText.getSentences().get(0);
        assertEquals("be", sentence.getTagOccurrence(49).getLemma());
    }

    @Test
    public void testAnnotateTextWithSpecification() {
        PipelineSpecification specification = new PipelineSpecification("custom", "en", StanfordTextProcessor.class.getName());
        specification.getProcessingSteps().put("tokenize", true);
        specification.getProcessingSteps().put("ner", true);
        specification.getExcludedNER().add("LOCATION");
        String text = "My name is John Doe and I work in Switzerland";
        AnnotatedText annotatedText = textProcessor.annotateText(text, specification);
        assertEquals(1, annotatedText.getSentences().size());
        int numberOfLocationEntities = 0;
        for (Sentence sentence : annotatedText.getSentences()) {
            for (List<TagOccurrence> olist : sentence.getTagOccurrences().values()) {
                for (TagOccurrence occurrence : olist ) {
                    if (occurrence.getElement().getNe().contains("LOCATION")) {
                        numberOfLocationEntities++;
                    }
                }
            }
        }
        assertEquals(0, numberOfLocationEntities);
    }

    @Test
    public void testAddPipelineTakesStopwordsIntoAccount() {
        String text = "det, vad, eller, sin, efter, i, varje, sådan, de, ditt, han, dessa, vi, med, då, den, mig, denna, ingen, under, henne, sådant, du, hade, vilken,".replaceAll(",", "");
        PipelineSpecification specification = new PipelineSpecification("customsw", "en", StanfordTextProcessor.class.getName());
        specification.getProcessingSteps().put("tokenize", true);
        String stopwords = "sådan,själv, dig, från, vilkas, dem, ett, varit, varför, att, era, som";
        specification.setStopWords("sådan,själv, dig, från, vilkas, dem, ett, varit, varför, att, era, som");
        AnnotatedText annotatedText = textProcessor.annotateText(text, specification);
        List<String> blacklist = Arrays.asList(stopwords.split(","));
        annotatedText.getTags().forEach(tag -> {
            assertFalse(blacklist.contains(tag.getLemma()));
        });
    }

    @Test
    public void testAddPipelineTakesStopwordsIntoAccountAfterNormalAnnotation() {
        String text = "det, vad, eller, sin, efter, i, varje, sådan, de, ditt, han, dessa, vi, med, då, den, mig, denna, ingen, under, henne, sådant, du, hade, vilken,".replaceAll(",", "");
        PipelineSpecification specification = new PipelineSpecification("customsw2", "en", StanfordTextProcessor.class.getName());
        specification.getProcessingSteps().put("tokenize", true);
        String stopwords = "sådan, själv, dig, från, vilkas, dem, ett, varit, varför, att, era, som";
        specification.setStopWords(stopwords);
        AnnotatedText annotatedText = textProcessor.annotateText(text, specification);
        List<String> blacklist = Arrays.asList(stopwords.split(","));
        annotatedText.getTags().forEach(tag -> {
            assertFalse(blacklist.contains(tag.getLemma()));
        });

        PipelineSpecification specification2 = new PipelineSpecification("customsw3", "en", StanfordTextProcessor.class.getName());
        specification.getProcessingSteps().put("tokenize", true);
        String stopwords2 = "eller, sådan,själv, dig, från, vilkas, dem, ett, varit, varför, att, era, som";
        specification2.setStopWords(stopwords2);
        AnnotatedText annotatedText2 = textProcessor.annotateText(text, specification2);
        List<String> blacklist2 = Arrays.asList(stopwords2.split(","));
        annotatedText2.getTags().forEach(tag -> {
            assertFalse(blacklist2.contains(tag.getLemma()));
        });
    }
//
//    @Test
//    public void testAnnotationOnSmallText() throws Exception {
//        String text = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("textFile6k.txt").getPath())));
//        PipelineSpecification specification = new PipelineSpecification("perf6k", "en", StanfordTextProcessor.class.getName());
//        specification.addProcessingStep("tokenize");
//        specification.addProcessingStep("ner");
//        specification.addProcessingStep("dependency");
////        specification.addProcessingStep("fineGrainedNER");
//        textProcessor.createPipeline(specification);
//
//        for (int i = 0; i < 20; ++i) {
//            Long start = System.currentTimeMillis();
//            AnnotatedText annotatedText = textProcessor.annotateText(text, "en", specification);
//            System.out.println(System.currentTimeMillis() - start);
//        }
//    }
}
