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
package com.graphaware.nlp.processor.stanford;

import com.graphaware.nlp.annotation.NLPTextProcessor;
import com.graphaware.nlp.domain.*;
import com.graphaware.nlp.processor.AbstractTextProcessor;
import com.graphaware.nlp.processor.PipelineInfo;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.processor.stanford.model.NERModelTool;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import static edu.stanford.nlp.sequences.SeqClassifierFlags.DEFAULT_BACKGROUND_SYMBOL;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NLPTextProcessor(name = "StanfordTextProcessor")
public class StanfordTextProcessor extends  AbstractTextProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(StanfordTextProcessor.class);
    protected static final String CORE_PIPELINE_NAME = "StanfordNLP.CORE";
    private static final String STEP_RELATIONS = "relations";

    public static final String TOKENIZER = "tokenizer";
    public static final String XML_TOKENIZER = "tokenizer";
    public static final String SENTIMENT = "sentiment";
    public static final String TOKENIZER_AND_SENTIMENT = "tokenizerAndSentiment";
    public static final String PHRASE = "phrase";
    public static final String DEPENDENCY_GRAPH = "tokenizerAndDependency";
    public static final String IE = "ie";

    protected String backgroundSymbol = DEFAULT_BACKGROUND_SYMBOL;
    protected final Map<String, StanfordCoreNLP> pipelines = new HashMap<>();
    protected boolean initiated = false;

    @Override
    public void init() {
        if (initiated) {
            return;
        }

        createCorePipelines();
        initiated = true;
    }

    @Override
    public String getAlias() {
        return "stanford";
    }

    @Override
    public String override() {
        return null;
    }

    protected void createCorePipelines() {
        createFullPipeline();
        createTokenizerPipeline();
        createSentimentPipeline();
        createTokenizerAndSentimentPipeline();
        createPhrasePipeline();
        createDependencyGraphPipeline();
        createIEPipeline();
    }

    protected void createFullPipeline() {
        StanfordCoreNLP pipeline = new PipelineBuilder(CORE_PIPELINE_NAME)
                .tokenize()
                .extractNEs()
                .extractSentiment()
                .extractCoref()
                .extractRelations()
                .openIE()
                .threadNumber(6)
                .build();
        pipelines.put(CORE_PIPELINE_NAME, pipeline);
        pipelineInfos.put(
                CORE_PIPELINE_NAME,
                createPipelineInfo(CORE_PIPELINE_NAME, pipeline, Arrays.asList("tokenize", "ner", "coref", "relations", "sentiment", "dependency", "phrase", "natlog", "openie")));
    }

    private void createTokenizerPipeline() {
        StanfordCoreNLP pipeline = pipelines.get(CORE_PIPELINE_NAME);
        pipelines.put(TOKENIZER, pipeline);
        pipelineInfos.put(TOKENIZER, createPipelineInfo(TOKENIZER, pipeline, Arrays.asList("tokenize", "ner")));
    }

    private void createSentimentPipeline() {
        StanfordCoreNLP pipeline = pipelines.get(CORE_PIPELINE_NAME);
        pipelines.put(SENTIMENT, pipeline);
        pipelineInfos.put(SENTIMENT, createPipelineInfo(SENTIMENT, pipeline, Arrays.asList("sentiment")));
    }

    private void createTokenizerAndSentimentPipeline() {
        StanfordCoreNLP pipeline = pipelines.get(CORE_PIPELINE_NAME);
        pipelines.put(TOKENIZER_AND_SENTIMENT, pipeline);
        pipelineInfos.put(TOKENIZER_AND_SENTIMENT, createPipelineInfo(TOKENIZER_AND_SENTIMENT, pipeline, Arrays.asList("tokenize", "ner", "sentiment")));
    }

    private void createPhrasePipeline() {
        StanfordCoreNLP pipeline = pipelines.get(CORE_PIPELINE_NAME);
        pipelines.put(PHRASE, pipeline);
        pipelineInfos.put(PHRASE, createPipelineInfo(PHRASE, pipeline, Arrays.asList("tokenize", "ner", "coref", "relations", "sentiment", PHRASE)));
    }

    private void createDependencyGraphPipeline() {
        StanfordCoreNLP pipeline = pipelines.get(CORE_PIPELINE_NAME);
        pipelines.put(DEPENDENCY_GRAPH, pipeline);
        pipelineInfos.put(DEPENDENCY_GRAPH, createPipelineInfo(DEPENDENCY_GRAPH, pipeline, Arrays.asList("tokenize", "ner", "dependency")));
    }

    private void createIEPipeline() {
        StanfordCoreNLP pipeline = pipelines.get(CORE_PIPELINE_NAME);
        pipelines.put(IE, pipeline);
        pipelineInfos.put(IE, createPipelineInfo(IE, pipeline, Arrays.asList("tokenize", "ner", "dependency", "sentiment", "phrase", "natlog", "openie")));

    }

    protected PipelineInfo createPipelineInfo(String name, StanfordCoreNLP pipeline, List<String> actives) {
        List<String> stopwords = PipelineBuilder.getDefaultStopwords();
        PipelineInfo info = new PipelineInfo(name, this.getClass().getName(), getPipelineProperties(pipeline), buildSpecifications(actives), 6, stopwords);

        return info;
    }

    protected Map<String, Object> getPipelineProperties(StanfordCoreNLP pipeline) {
        Map<String, Object> options = new HashMap<>();
        for (Object o : pipeline.getProperties().keySet()) {
            if (o instanceof String) {
                options.put(o.toString(), pipeline.getProperties().getProperty(o.toString()));
            }
        }

        return options;
    }

    public StanfordCoreNLP getPipeline(String name) {
        if (name == null || name.isEmpty()) {
            name = TOKENIZER;
            LOG.debug("Using default pipeline: " + name);
        }
        StanfordCoreNLP pipeline = pipelines.get(name);
        if (pipeline == null) {
            throw new RuntimeException("Pipeline: " + name + " doesn't exist");
        }

        return pipeline;
    }

    @Override
    public AnnotatedText annotateText(String text, String lang, PipelineSpecification pipelineSpecification) {
        AnnotatedText result = new AnnotatedText();
        Annotation document = new Annotation(text);
        StanfordCoreNLP pipeline = getPipeline(CORE_PIPELINE_NAME);

        // Add custom NER models
        if (pipelineSpecification.hasProcessingStep("customNER")) {
            pipeline.getProperties().setProperty("ner.model", createModelFileName("ner", pipelineSpecification.getProcessingStepAsString("customNER")));
            LOG.info("Custom NER(s) set to: " + pipeline.getProperties().getProperty("ner.model"));
        }

        // Add stopwords list
        String stopWordList = AbstractTextProcessor.DEFAULT_STOP_WORD_LIST;
        if (pipelineSpecification.getStopWords() != null) {
            String customStopWordList = pipelineSpecification.getStopWords();
            if (customStopWordList.startsWith("+")) {
                stopWordList += "," + customStopWordList.replace("+,", "").replace("+", "");
            } else {
                stopWordList = customStopWordList;
            }
        }
        pipeline.getProperties().setProperty(StopwordAnnotator.STOPWORDS_LIST, stopWordList);
        String annotatorName = "customAnnotatorClass.stopword";
        pipeline.getProperties().setProperty(annotatorName, StopwordAnnotator.class.getName());
        pipeline.getProperties().setProperty(StopwordAnnotator.CHECK_LEMMA, String.valueOf(true));

        pipeline.addAnnotator(new StopwordAnnotator(getClass().getName(), pipeline.getProperties()));

        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        final AtomicInteger sentenceSequence = new AtomicInteger(0);
        sentences.forEach((sentence) -> {
            int sentenceNumber = sentenceSequence.getAndIncrement();
            final Sentence newSentence = new Sentence(sentence.toString(), sentenceNumber);

            if (pipelineSpecification.hasProcessingStep(STEP_NER, true) || pipelineSpecification.hasProcessingStep("customNER")) {
                extractTokens(lang, sentence, newSentence, pipelineSpecification.getExcludedNER());
            }

            if (pipelineSpecification.hasProcessingStep(STEP_SENTIMENT, false)) {
                extractSentiment(sentence, newSentence);
            }

            if (pipelineSpecification.hasProcessingStep(STEP_PHRASE, false)) {
                extractPhrases(sentence, newSentence);
            }

            if (pipelineSpecification.hasProcessingStep(STEP_DEPENDENCY, false)) {
                extractDependencies(sentence, newSentence);
            }
            if (pipelineSpecification.hasProcessingStep(STEP_RELATIONS, false)) {
                extractRelationship(result, sentences, document);
            }
            result.addSentence(newSentence);

        });

        return result;
    }

    @Override
    public AnnotatedText annotateText(String text, String pipelineName, String lang, Map<String, String> extraParameters) {
        AnnotatedText result = new AnnotatedText();
        Annotation document = new Annotation(text);
        StanfordCoreNLP pipeline = getPipeline(pipelineName);

        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        final AtomicInteger sentenceSequence = new AtomicInteger(0);
        sentences.forEach((sentence) -> {
            int sentenceNumber = sentenceSequence.getAndIncrement();
            final Sentence newSentence = new Sentence(sentence.toString(), sentenceNumber);
            extractTokens(lang, sentence, newSentence, pipelineName);
            if ((boolean) pipelineInfos.get(pipelineName).specifications.get(PHRASE)) {
                extractPhrases(sentence, newSentence, pipelineName);
            }
            if ((boolean) pipelineInfos.get(pipelineName).specifications.get(SENTIMENT)) {
                extractSentiment(sentence, newSentence);
            }
            if ((boolean) pipelineInfos.get(pipelineName).specifications.get("dependency")) {
                extractDependencies(sentence, newSentence);
            }
            if ( (boolean) pipelineInfos.get(pipelineName).specifications.get("relations")) {
                extractRelationship(result, sentences, document);
            }

            result.addSentence(newSentence);
        });
        return result;
    }

    protected void extractPhrases(CoreMap sentence, Sentence newSentence) {
        Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
        if (tree == null) {
            return;
        }
        Set<PhraseHolder> extractedPhrases = inspectSubTree(tree);
        extractedPhrases.stream().forEach((holder) -> {
            newSentence.addPhraseOccurrence(holder.getBeginPosition(), holder.getEndPosition(), new Phrase(holder.getPhrase()));
        });
    }

    protected void extractPhrases(CoreMap sentence, Sentence newSentence, String pipelineName) {
        PipelineInfo pipelineInfo = pipelineInfos.get(pipelineName);
        extractPhrases(sentence, newSentence);
    }

    protected void extractSentiment(CoreMap sentence, final Sentence newSentence) {
        int score = extractSentiment(sentence);
        newSentence.setSentiment(score);
    }

    protected void extractTokens(String lang, CoreMap sentence, final Sentence newSentence, List<String> excludedNER) {
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        TokenHolder currToken = new TokenHolder();
        currToken.setNe(backgroundSymbol);
        tokens.stream()
                .filter((token) -> (token != null))
                .map((token) -> {
                    //
                    String tokenId = newSentence.getId() + token.beginPosition() + token.endPosition() + token.lemma();
                    String currentNe = backgroundSymbol;
                    currentNe = StringUtils.getNotNullString(token.get(CoreAnnotations.NamedEntityTagAnnotation.class));

                    if (!checkLemmaIsValid(token.get(CoreAnnotations.LemmaAnnotation.class))) {
                        if (currToken.getToken().length() > 0) {
                            Tag newTag = new Tag(currToken.getToken(), lang);
                            if (!excludedNER.contains(currToken.getNe())) {
                                newTag.setNe(Arrays.asList(currToken.getNe()));
                            }
                            newSentence.addTagOccurrence(currToken.getBeginPosition(),
                                    currToken.getEndPosition(),
                                    currToken.getOriginalValue(),
                                    newSentence.addTag(newTag),
                                    getTokenIdsToUse(tokenId, currToken.getTokenIds()));
                            currToken.reset();
                        }
                    } else if (currentNe.equals(backgroundSymbol)
                            && currToken.getNe().equals(backgroundSymbol)) {
                        Tag tag = getTag(lang, token);
                        if (tag != null) {
                            newSentence.addTagOccurrence(token.beginPosition(),
                                    token.endPosition(),
                                    token.originalText(),
                                    newSentence.addTag(tag),
                                    Arrays.asList(tokenId));
                        } /* else { //>>>>>>>> This shouldn't be here, right? Because tokenId is NOT NamedEntity; moreover, getTag() returns null only if it didn't pass stopwords list.
                            if (!currToken.getTokenIds().contains(tokenId)) {
                                currToken.getTokenIds().add(tokenId);   //>>>>>>>> This is wrong: updateToken() should be used!
                            } else {
                                // debug
                            }
                        }*/
                    } else if (currentNe.equals(backgroundSymbol)
                            && !currToken.getNe().equals(backgroundSymbol)) {
                        if (currToken.getToken().length() > 0) {
                            Tag newTag = new Tag(currToken.getToken(), lang);
                            if (!excludedNER.contains(currToken.getNe())) {
                                newTag.setNe(Arrays.asList(currToken.getNe()));
                            }
                            newSentence.addTagOccurrence(currToken.getBeginPosition(),
                                    currToken.getEndPosition(),
                                    currToken.getOriginalValue(),
                                    newSentence.addTag(newTag),
                                    getTokenIdsToUse(tokenId, currToken.getTokenIds()));
                        }
                        currToken.reset();
                        Tag tag = getTag(lang, token);
                        if (tag != null) {
                            newSentence.addTagOccurrence(token.beginPosition(),
                                    token.endPosition(),
                                    token.originalText(),
                                    newSentence.addTag(tag),
                                    getTokenIdsToUse(tokenId, currToken.getTokenIds()));
                        }
                    } else if (!currentNe.equals(currToken.getNe())
                            && !currToken.getNe().equals(backgroundSymbol)) {
                        if (currToken.getToken().length() > 0) {
                            Tag tag = new Tag(currToken.getToken(), lang);
                            if (!excludedNER.contains(currToken.getNe())) {
                                tag.setNe(Arrays.asList(currToken.getNe()));
                            }
                            newSentence.addTagOccurrence(currToken.getBeginPosition(),
                                    currToken.getEndPosition(),
                                    currToken.getOriginalValue(),
                                    newSentence.addTag(tag),
                                    getTokenIdsToUse(tokenId, currToken.getTokenIds()));
                        }
                        currToken.reset();
                        currToken.updateTokenAndTokenId(
                                StringUtils.getNotNullString(token.get(CoreAnnotations.OriginalTextAnnotation.class)),
                                StringUtils.getNotNullString(token.originalText()),
                                tokenId);
                        currToken.setBeginPosition(token.beginPosition());
                        currToken.setEndPosition(token.endPosition());
                    } else if (!currentNe.equals(backgroundSymbol)
                            && currToken.getNe().equals(backgroundSymbol)) {
                        currToken.updateTokenAndTokenId(
                                StringUtils.getNotNullString(token.get(CoreAnnotations.OriginalTextAnnotation.class)),
                                StringUtils.getNotNullString(token.originalText()),
                                tokenId);
                        currToken.setBeginPosition(token.beginPosition());
                        currToken.setEndPosition(token.endPosition());
                    } else {
                        // happens for eg when there is a space before a Tag, hence the "Before"
                        String before = StringUtils.getNotNullString(token.get(CoreAnnotations.BeforeAnnotation.class));
                        String currentText = StringUtils.getNotNullString(token.get(CoreAnnotations.OriginalTextAnnotation.class));
                        currToken.updateToken(before, before);
                        currToken.updateTokenAndTokenId(currentText, token.originalText(), tokenId);
                        currToken.setBeginPosition(token.beginPosition());
                        currToken.setEndPosition(token.endPosition());
                    }

                    return currentNe;
                }).forEach((currentNe) -> {
            if (!excludedNER.contains(currentNe)) {
                currToken.setNe(currentNe);
            }
        });

        if (currToken.getToken().length() > 0) {
            Tag tag = new Tag(currToken.getToken(), lang);
            tag.setNe(Arrays.asList(currToken.getNe()));
            newSentence.addTagOccurrence(currToken.getBeginPosition(),
                    currToken.getEndPosition(),
                    currToken.getOriginalValue(),
                    newSentence.addTag(tag),
                    currToken.getTokenIds());
        }
    }

    protected void extractTokens(String lang, CoreMap sentence, final Sentence newSentence, String pipelineName) {
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        TokenHolder currToken = new TokenHolder();
        currToken.setNe(backgroundSymbol);
        tokens.stream()
                .filter((token) -> (token != null))
                .map((token) -> {
                    //
                    String tokenId = newSentence.getId() + token.beginPosition() + token.endPosition() + token.lemma();
                    String currentNe = backgroundSymbol;
                    if (pipelineInfos.get(pipelineName).getSpecifications().containsKey("ner") && pipelineInfos.get(pipelineName).getSpecifications().get("ner").equals(true))
                        currentNe = StringUtils.getNotNullString(token.get(CoreAnnotations.NamedEntityTagAnnotation.class));

                    if (!checkLemmaIsValid(token.get(CoreAnnotations.LemmaAnnotation.class))) {
                        if (currToken.getToken().length() > 0) {
                            Tag newTag = new Tag(currToken.getToken(), lang);
                            newTag.setNe(Arrays.asList(currToken.getNe()));
                            newSentence.addTagOccurrence(currToken.getBeginPosition(),
                                    currToken.getEndPosition(),
                                    currToken.getOriginalValue(),
                                    newSentence.addTag(newTag),
                                    getTokenIdsToUse(tokenId, currToken.getTokenIds()));
                            currToken.reset();
                        }
                    } else if (currentNe.equals(backgroundSymbol)
                            && currToken.getNe().equals(backgroundSymbol)) {
                        Tag tag = getTag(lang, token);
                        if (tag != null) {
                            newSentence.addTagOccurrence(token.beginPosition(),
                                    token.endPosition(),
                                    token.originalText(),
                                    newSentence.addTag(tag),
                                    Arrays.asList(tokenId));
                        } /* else { //>>>>>>>> This shouldn't be here, right? Because tokenId is NOT NamedEntity; moreover, getTag() returns null only if it didn't pass stopwords list.
                            if (!currToken.getTokenIds().contains(tokenId)) {
                                currToken.getTokenIds().add(tokenId);   //>>>>>>>> This is wrong: updateToken() should be used!
                            } else {
                                // debug
                            }
                        }*/
                    } else if (currentNe.equals(backgroundSymbol)
                            && !currToken.getNe().equals(backgroundSymbol)) {
                        if (currToken.getToken().length() > 0) {
                            Tag newTag = new Tag(currToken.getToken(), lang);
                            newTag.setNe(Arrays.asList(currToken.getNe()));
                            newSentence.addTagOccurrence(currToken.getBeginPosition(),
                                    currToken.getEndPosition(),
                                    currToken.getOriginalValue(),
                                    newSentence.addTag(newTag),
                                    getTokenIdsToUse(tokenId, currToken.getTokenIds()));
                        }
                        currToken.reset();
                        Tag tag = getTag(lang, token);
                        if (tag != null) {
                            newSentence.addTagOccurrence(token.beginPosition(),
                                    token.endPosition(),
                                    token.originalText(),
                                    newSentence.addTag(tag),
                                    getTokenIdsToUse(tokenId, currToken.getTokenIds()));
                        }
                    } else if (!currentNe.equals(currToken.getNe())
                            && !currToken.getNe().equals(backgroundSymbol)) {
                        if (currToken.getToken().length() > 0) {
                            Tag tag = new Tag(currToken.getToken(), lang);
                            tag.setNe(Arrays.asList(currToken.getNe()));
                            newSentence.addTagOccurrence(currToken.getBeginPosition(),
                                    currToken.getEndPosition(),
                                    currToken.getOriginalValue(),
                                    newSentence.addTag(tag),
                                    getTokenIdsToUse(tokenId, currToken.getTokenIds()));
                        }
                        currToken.reset();
                        currToken.updateTokenAndTokenId(
                                StringUtils.getNotNullString(token.get(CoreAnnotations.OriginalTextAnnotation.class)),
                                StringUtils.getNotNullString(token.originalText()),
                                tokenId);
                        currToken.setBeginPosition(token.beginPosition());
                        currToken.setEndPosition(token.endPosition());
                    } else if (!currentNe.equals(backgroundSymbol)
                            && currToken.getNe().equals(backgroundSymbol)) {
                        currToken.updateTokenAndTokenId(
                                StringUtils.getNotNullString(token.get(CoreAnnotations.OriginalTextAnnotation.class)),
                                StringUtils.getNotNullString(token.originalText()),
                                tokenId);
                        currToken.setBeginPosition(token.beginPosition());
                        currToken.setEndPosition(token.endPosition());
                    } else {
                        // happens for eg when there is a space before a Tag, hence the "Before"
                        String before = StringUtils.getNotNullString(token.get(CoreAnnotations.BeforeAnnotation.class));
                        String currentText = StringUtils.getNotNullString(token.get(CoreAnnotations.OriginalTextAnnotation.class));
                        currToken.updateToken(before, before);
                        currToken.updateTokenAndTokenId(currentText, token.originalText(), tokenId);
                        currToken.setBeginPosition(token.beginPosition());
                        currToken.setEndPosition(token.endPosition());
                    }

                    return currentNe;
                }).forEach((currentNe) -> {
            currToken.setNe(currentNe);
        });

        if (currToken.getToken().length() > 0) {
            Tag tag = new Tag(currToken.getToken(), lang);
            tag.setNe(Arrays.asList(currToken.getNe()));
            newSentence.addTagOccurrence(currToken.getBeginPosition(),
                    currToken.getEndPosition(),
                    currToken.getOriginalValue(),
                    newSentence.addTag(tag),
                    currToken.getTokenIds());
        }
    }

    protected void extractDependencies(CoreMap sentence, final Sentence newSentence) {
        SemanticGraph semanticGraph = sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);
        if (semanticGraph == null) {
            return;
        }

        semanticGraph.getRoots().forEach(root -> {
            String rootId = newSentence.getId() + root.beginPosition() + root.endPosition() + root.lemma();
            TypedDependency typedDependency = new TypedDependency(rootId, rootId, "ROOT", null);
            newSentence.addTypedDependency(typedDependency);
        });

        for (SemanticGraphEdge edge : semanticGraph.edgeListSorted()) {
            String sourceId = newSentence.getId() + edge.getSource().beginPosition() + edge.getSource().endPosition() + edge.getSource().lemma();
            String targetId = newSentence.getId() + edge.getTarget().beginPosition() + edge.getTarget().endPosition() + edge.getTarget().lemma();
            TypedDependency typedDependency = new TypedDependency(sourceId, targetId, edge.getRelation().getShortName(), edge.getRelation().getSpecific());
            newSentence.addTypedDependency(typedDependency);
        }
    }

    protected void extractRelationship(AnnotatedText annotatedText, List<CoreMap> sentences, Annotation document) {
        Map<Integer, CorefChain> corefChains = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
        if (corefChains != null) {
            for (CorefChain chain : corefChains.values()) {
                CorefChain.CorefMention representative = chain.getRepresentativeMention();
                int representativeSenteceNumber = representative.sentNum - 1;
                List<CoreLabel> representativeTokens = sentences.get(representativeSenteceNumber).get(CoreAnnotations.TokensAnnotation.class);
                System.out.println(representativeTokens.size() + " representative tokens");
                System.out.println("representative end index is : " + representative.endIndex);
                if (representative.endIndex - 1 > representativeTokens.size() ) {
                    continue;
                }
                int beginPosition = representativeTokens.get(representative.startIndex - 1).beginPosition();
                int endPosition = representativeTokens.get(representative.endIndex - 2).endPosition();
                Phrase representativePhraseOccurrence = annotatedText.getSentences().get(representativeSenteceNumber).getPhraseOccurrence(beginPosition, endPosition);
                if (representativePhraseOccurrence == null) {
                    LOG.warn("Representative Phrase not found: " + representative.mentionSpan);
                }
                for (CorefChain.CorefMention mention : chain.getMentionsInTextualOrder()) {
                    if (mention == representative) {
                        continue;
                    }
                    int mentionSentenceNumber = mention.sentNum - 1;

                    List<CoreLabel> mentionTokens = sentences.get(mentionSentenceNumber).get(CoreAnnotations.TokensAnnotation.class);
                    int beginPositionMention = mentionTokens.get(mention.startIndex - 1).beginPosition();
                    int endPositionMention = mentionTokens.get(mention.endIndex - 2).endPosition();
                    Phrase mentionPhraseOccurrence = annotatedText.getSentences().get(mentionSentenceNumber).getPhraseOccurrence(beginPositionMention, endPositionMention);
                    if (mentionPhraseOccurrence == null) {
                        LOG.warn("Mention Phrase not found: " + mention.mentionSpan);
                    }
                    if (representativePhraseOccurrence != null
                            && mentionPhraseOccurrence != null) {
                        mentionPhraseOccurrence.setReference(representativePhraseOccurrence);
                    }
                }
            }
        }
    }

    @Override
    public AnnotatedText sentiment(AnnotatedText annotatedText) {
        StanfordCoreNLP pipeline = pipelines.get(SENTIMENT);
        if (pipeline == null) {
            throw new RuntimeException("Pipeline: " + SENTIMENT + " doesn't exist");
        }
        annotatedText.getSentences().parallelStream().forEach((item) -> {
            Annotation document = new Annotation(item.getSentence());
            pipeline.annotate(document);
            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
            Optional<CoreMap> sentence = sentences.stream().findFirst();
            if (sentence != null && sentence.isPresent()) {
                extractSentiment(sentence.get(), item);
            }
        });
        return annotatedText;
    }

    protected int extractSentiment(CoreMap sentence) {
        Tree tree = sentence
                .get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
        if (tree == null) {
            return Sentence.NO_SENTIMENT;
        }
        int score = RNNCoreAnnotations.getPredictedClass(tree);
        return score;
    }

    @Override
    public Tag annotateSentence(String text, String lang) {
        Annotation document = new Annotation(text);
        pipelines.get(SENTIMENT).annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        Optional<CoreMap> sentence = sentences.stream().findFirst();
        if (sentence.isPresent()) {
            Optional<Tag> oTag = sentence.get().get(CoreAnnotations.TokensAnnotation.class).stream()
                    .map((token) -> getTag(lang, token))
                    .filter((tag) -> (tag != null) && checkLemmaIsValid(tag.getLemma()))
                    .findFirst();
            if (oTag.isPresent()) {
                return oTag.get();
            }
        }
        return null;
    }

    @Override
    public Tag annotateTag(String text, String lang) {
        Annotation document = new Annotation(text);
        pipelines.get(TOKENIZER).annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        Optional<CoreMap> sentence = sentences.stream().findFirst();
        if (sentence.isPresent()) {
            List<CoreLabel> tokens = sentence.get().get(CoreAnnotations.TokensAnnotation.class);
            if (tokens != null) {
                if (tokens.size() == 1) {
                    Optional<Tag> oTag = tokens.stream()
                            .map((token) -> getTag(lang, token))
                            .filter((tag) -> (tag != null) && checkLemmaIsValid(tag.getLemma()))
                            .findFirst();
                    if (oTag.isPresent()) {
                        return oTag.get();
                    }
                } else if (tokens.size() > 1) {
                    Tag tag = new Tag(text, lang);
                    tag.setPos(Arrays.asList());
                    tag.setNe(Arrays.asList());
                    LOG.info("POS: " + tag.getPosAsList() + " ne: " + tag.getNeAsList() + " lemma: " + tag.getLemma());
                    return tag;
                }
            }
        }
        return null;
    }

    protected Tag getTag(String lang, CoreLabel token) {
        Pair<Boolean, Boolean> stopword = token.get(StopwordAnnotator.class);
        if (stopword != null && (stopword.first() || stopword.second())) {
            return null;
        }
        String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
        if (ne == null)
            ne = backgroundSymbol;
        String lemma;

        if (ne.equals(backgroundSymbol)) {
            String lemmaValue = token.get(CoreAnnotations.LemmaAnnotation.class);
            String value = token.get(CoreAnnotations.TextAnnotation.class);
            if (lemmaValue != null && lemmaValue.equalsIgnoreCase(value)) {
                lemma = value;
            } else {
                lemma = lemmaValue;
            }
        } else {
            lemma = token.get(CoreAnnotations.OriginalTextAnnotation.class);
        }

        Tag tag = new Tag(lemma, lang);
        tag.setPos(Arrays.asList(pos));
        tag.setNe(Arrays.asList(ne));
        LOG.info("POS: " + pos + " ne: " + ne + " lemma: " + lemma);
        return tag;
    }

    @Override
    public boolean checkLemmaIsValid(String value) {
        Matcher match = patternCheck.matcher(value);

        //boolean found = patternCheck.matcher(value).find();

        return match.find();
    }

    protected Set<PhraseHolder> inspectSubTree(Tree subTree) {
        Set<PhraseHolder> result = new TreeSet<>();
        if (subTree.value().equalsIgnoreCase("NP") || subTree.value().equalsIgnoreCase("NP-TMP")) {// set your rule of defining Phrase here
            PhraseHolder pHolder = new PhraseHolder();
            List<Tree> leaves = subTree.getLeaves(); //leaves correspond to the tokens
            leaves.stream().map((leaf) -> leaf.yieldWords()).map((words) -> {
                pHolder.setBeginPosition(words.get(0).beginPosition());
                pHolder.setEndPosition(words.get(words.size() - 1).endPosition());
                return words;
            }).forEach((words) -> {
                words.stream().forEach((word) -> {
                    pHolder.updatePhrase(word.word());
                    pHolder.updatePhrase(" ");
                });
            });
            result.add(pHolder);
            subTree.getChildrenAsList().stream().filter((child) -> (!child.equals(subTree))).forEach((child) -> {
                result.addAll(inspectSubTree(child));
            });
        } else if (subTree.isLeaf()) {
            PhraseHolder pHolder = new PhraseHolder();
            ArrayList<Word> words = subTree.yieldWords();
            pHolder.setBeginPosition(words.get(0).beginPosition());
            pHolder.setEndPosition(words.get(words.size() - 1).endPosition());
            words.stream().forEach((word) -> {
                pHolder.updatePhrase(word.word());
                pHolder.updatePhrase(" ");
            });
            result.add(pHolder);
        } else {
            List<Tree> children = subTree.getChildrenAsList();
            children.stream().forEach((child) -> {
                result.addAll(inspectSubTree(child));
            });
        }
        return result;
    }

    @Override
    public List<Tag> annotateTags(String text, String lang) {
        List<Tag> result = new ArrayList<>();
        Annotation document = new Annotation(text);
        pipelines.get(TOKENIZER).annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        Optional<CoreMap> sentence = sentences.stream().findFirst();
        if (sentence.isPresent()) {
            Stream<Tag> oTags = sentence.get().get(CoreAnnotations.TokensAnnotation.class).stream()
                    .map((token) -> getTag(lang, token))
                    .filter((tag) -> (tag != null) && checkLemmaIsValid(tag.getLemma()));
            oTags.forEach((tag) -> result.add(tag));
        }
        return result;
    }

    class TokenHolder {

        private String ne;
        private StringBuilder sb;
        private StringBuilder sbOriginalValue;
        private int beginPosition;
        private int endPosition;
        private List<String> tokenIds = new ArrayList<>();

        public TokenHolder() {
            reset();
        }

        public String getNe() {
            return ne;
        }

        public String getToken() {
            return sb.toString();
        }
        
        public String getOriginalValue() {
            return sbOriginalValue.toString();
        }

        public int getBeginPosition() {
            return beginPosition;
        }

        public int getEndPosition() {
            return endPosition;
        }

        public void setNe(String ne) {
            this.ne = ne;
        }

        public void updateToken(String tknStr, String originalValue) {
            this.sb.append(tknStr);
            this.sbOriginalValue.append(originalValue);
        }

        public void updateTokenAndTokenId(String tknStr, String originalValue, String tokenId) {
            updateToken(tknStr, originalValue);
            tokenIds.add(tokenId);
        }

        public List<String> getTokenIds() {
            return tokenIds;
        }

        public void setBeginPosition(int beginPosition) {
            if (this.beginPosition < 0) {
                this.beginPosition = beginPosition;
            }
        }

        public void setEndPosition(int endPosition) {
            this.endPosition = endPosition;
        }

        public final void reset() {
            sb = new StringBuilder();
            sbOriginalValue = new StringBuilder();
            beginPosition = -1;
            endPosition = -1;
            tokenIds.clear();
        }
    }

    class PhraseHolder implements Comparable<PhraseHolder> {

        private StringBuilder sb;
        private int beginPosition;
        private int endPosition;

        public PhraseHolder() {
            reset();
        }

        public String getPhrase() {
            if (sb == null) {
                return " - ";
            }
            return sb.toString();
        }

        public int getBeginPosition() {
            return beginPosition;
        }

        public int getEndPosition() {
            return endPosition;
        }

        public void updatePhrase(String tknStr) {
            this.sb.append(tknStr);
        }

        public void setBeginPosition(int beginPosition) {
            if (this.beginPosition < 0) {
                this.beginPosition = beginPosition;
            }
        }

        public void setEndPosition(int endPosition) {
            this.endPosition = endPosition;
        }

        public final void reset() {
            sb = new StringBuilder();
            beginPosition = -1;
            endPosition = -1;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PhraseHolder)) {
                return false;
            }
            PhraseHolder otherObject = (PhraseHolder) o;
            if (this.sb != null
                    && otherObject.sb != null
                    && this.sb.toString().equals(otherObject.sb.toString())
                    && this.beginPosition == otherObject.beginPosition
                    && this.endPosition == otherObject.endPosition) {
                return true;
            }
            return false;
        }

        @Override
        public int compareTo(PhraseHolder o) {
            if (o == null) {
                return 1;
            }
            if (this.equals(o)) {
                return 0;
            } else if (this.beginPosition > o.beginPosition) {
                return 1;
            } else if (this.beginPosition == o.beginPosition) {
                if (this.endPosition > o.endPosition) {
                    return 1;
                }
            }
            return -1;
        }
    }

    @Override
    public List<String> getPipelines() {
        return new ArrayList<>(pipelines.keySet());
    }

    public boolean checkPipeline(String name) {
        return pipelines.containsKey(name);
    }

    @Override
    public void createPipeline(PipelineSpecification pipelineSpecification) {
        //@todo create constants for processing steps
        String name = pipelineSpecification.getName();
//        PipelineBuilder pipelineBuilder = new PipelineBuilder(name);
        List<String> stopwordsList;
        List<String> specActive = new ArrayList<>();

        if (pipelineSpecification.hasProcessingStep("tokenize", true)) {
//            pipelineBuilder.tokenize();
            specActive.add("tokenize");
        }

        if (pipelineSpecification.hasProcessingStep("ner", true)) {
//            pipelineBuilder.extractNEs();
            specActive.add("ner");
        }

        if (pipelineSpecification.hasProcessingStep("cleanxml")) {
//            pipelineBuilder.cleanxml();
            specActive.add("cleanxml");
        }

        if (pipelineSpecification.hasProcessingStep("truecase")) {
//            pipelineBuilder.truecase();
            specActive.add("truecase");
        }

        if (pipelineSpecification.hasProcessingStep("dependency")) {
//            pipelineBuilder.dependencies();
            specActive.add("dependency");
        }

        String stopWords = pipelineSpecification.getStopWords() != null ? pipelineSpecification.getStopWords() : "default";
        if (pipelineSpecification.hasProcessingStep("checkLemmaIsStopWord")) {
            specActive.add("checkLemmaIsStopWord");
        }
        if (stopWords.equalsIgnoreCase("default")) {
//            pipelineBuilder.defaultStopWordAnnotator();
            stopwordsList = PipelineBuilder.getDefaultStopwords();
        } else {
//            pipelineBuilder.customStopWordAnnotator(stopWords, checkLemma);
            stopwordsList = PipelineBuilder.getCustomStopwordsList(stopWords);
        }

        if (pipelineSpecification.hasProcessingStep("sentiment")) {
//            pipelineBuilder.extractSentiment();
            specActive.add("sentiment");
        }
        if (pipelineSpecification.hasProcessingStep("coref")) {
//            pipelineBuilder.extractCoref();
            specActive.add("coref");
        }
        if (pipelineSpecification.hasProcessingStep("relations")) {
//            pipelineBuilder.extractRelations();
            specActive.add("relations");
        }
        Long threadNumber = pipelineSpecification.getThreadNumber() != 0 ? pipelineSpecification.getThreadNumber() : 4L;
//        pipelineBuilder.threadNumber(threadNumber.intValue());
        if (pipelineSpecification.hasProcessingStep("customNER")) {
            if (!specActive.contains("ner")) { // without "ner", annotation doesn't work (only sentences get extracted, but no tags!)
//                pipelineBuilder.extractNEs();
                specActive.add("ner");
            }
        }

        Map<String, Object> specMap = buildSpecifications(specActive);
        LOG.info(" >>>>>>> Creating pipeline");
        if (pipelineSpecification.hasProcessingStep("customNER")) {
            LOG.info(" >>>>>>>   Adding " + pipelineSpecification.getProcessingStepAsString("customNER"));
            LOG.info(" >>>>>>>   File: " + createModelFileName("ner", pipelineSpecification.getProcessingStepAsString("customNER")));
            System.out.println("\n >>>>>>>>>>>> Adding customNER to the pipeline: " + createModelFileName("ner", pipelineSpecification.getProcessingStepAsString("customNER")));
            specMap.put("customNER", pipelineSpecification.getProcessingStepAsString("customNER")); // here must be provided _path_ to the model file
//            pipelineBuilder.extractCustomNEs(pipelineSpecification.getProcessingStepAsString("customNER"));
        }

        StanfordCoreNLP pipeline = pipelines.get(CORE_PIPELINE_NAME);
        pipelines.put(name, pipeline);
        PipelineInfo pipelineInfo = new PipelineInfo(
                name,
                this.getClass().getName(),
                getPipelineProperties(pipeline),
                specMap,
                Integer.valueOf(threadNumber.toString()),
                stopwordsList
        );

        pipelineInfos.put(name, pipelineInfo);
    }

    @Override
    public void removePipeline(String name) {
        if (!pipelines.containsKey(name)) {
            throw new RuntimeException("No pipeline found with name: " + name);
        }
        pipelines.remove(name);
    }

    @Override
    public List<PipelineInfo> getPipelineInfos() {
        List<PipelineInfo> list = new ArrayList<>();

        for (String k : pipelines.keySet()) {
            list.add(pipelineInfos.get(k));
        }

        return list;
    }

    protected Map<String, Object> buildSpecifications(List<String> actives) {
        List<String> all = Arrays.asList("tokenize", "ner", "cleanxml", "truecase", "dependency", "relations", "checkLemmaIsStopWord", "coref", "sentiment", "phrase", "customNER");
        Map<String, Object> specs = new HashMap<>();
        all.forEach(s -> {
            specs.put(s, actives.contains(s));
        });

        return specs;
    }

    @Override
    public String train(String alg, String modelId, String file, String lang, Map<String, Object> params) {
        LOG.info("Training of " + alg + " with id " + modelId + " started.");
        String propFile = null;
        if (params != null && params.containsKey("propertiesFile"))
            propFile = (String) params.get("propertiesFile");
        LOG.info("Initialising ...");
        NERModelTool nerModel = new NERModelTool(file, modelId, lang, propFile);
        nerModel.train(createModelFileName(alg, modelId));
        return "Training successful.";
    }

    @Override
    public String test(String alg, String modelId, String file, String lang) {
        LOG.info("Testing of " + alg + " with id " + modelId + " started.");
        NERModelTool nerModel = new NERModelTool(modelId, lang);
        return nerModel.test(file, createModelFileName(alg, modelId));
    }

    protected List<String> getTokenIdsToUse(String tokenId, List<String> currTokenTokenIds) {
        if (currTokenTokenIds.isEmpty()) {
            return Arrays.asList(tokenId);
        }

        return currTokenTokenIds;
    }

    private String createModelFileName(String alg, String model) {
        String delim = "-";
        //String name = "import/" + lang.toLowerCase() + delim + alg.toLowerCase();
        String name = "import/" + alg.toLowerCase();
        if (model != null && !model.isEmpty()) {
            name += delim + model.toLowerCase();
        }
        name += ".ser.gz";
        return name;
    }
}
