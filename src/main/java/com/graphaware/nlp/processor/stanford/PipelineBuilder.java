package com.graphaware.nlp.processor.stanford;

import com.graphaware.nlp.processor.AbstractTextProcessor;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.io.IOException;
import java.util.*;

public class PipelineBuilder {

    protected final Properties properties = new Properties();
    protected final StringBuilder annotators = new StringBuilder(); //basics annotators
    protected int threadsNumber = 4;
    
    protected final String name;

    public PipelineBuilder(String name) {
        this.name = name;
    }

    public PipelineBuilder(String name, String language) {
        this(name);
        if (language != null) {
            try {
                properties.load(getClass().getClassLoader().getResourceAsStream("StanfordCoreNLP-"
                        + language
                        + ".properties"));
            } catch (IOException ex) {
                throw new RuntimeException("Language not found: " + language, ex);
            }
        }
    }
    
    public PipelineBuilder tokenize() {
        checkForExistingAnnotators();
        annotators.append("tokenize, ssplit, pos, lemma");
//        properties.setProperty("pos.maxlen", "100");
//        properties.setProperty("parse.maxlen", "100");
        return this;
    }

    public PipelineBuilder extractNEs(boolean fineGrained) {
        checkForExistingAnnotators();
        annotators.append("ner");
        properties.setProperty("ner.useSUTime", "false");
        properties.setProperty("ner.applyNumericClassifiers", "false");
        properties.setProperty("ner.applyFineGrained", String.valueOf(fineGrained));
        return this;
    }

    public PipelineBuilder extractNEs(String modelPath) {
        checkForExistingAnnotators();
        annotators.append("ner");
        properties.setProperty("ner.model", modelPath);
        return this;
    }
    
    public PipelineBuilder cleanxml() {
        checkForExistingAnnotators();
        annotators.append("cleanxml");
        properties.setProperty("clean.allowflawedxml", "true");
        return this;
    }
    
    public PipelineBuilder truecase() {
        checkForExistingAnnotators();
        annotators.append("truecase");
        properties.setProperty("truecase.overwriteText", "true");
        return this;
    }

    public PipelineBuilder dependencies() {
        checkForExistingAnnotators();
        annotators.append("depparse");
        return this;
    }

    protected void checkForExistingAnnotators() {
        if (annotators.toString().length() > 0) {
            annotators.append(", ");
        }
    }

    public PipelineBuilder extractSentiment() {
        checkForExistingAnnotators();
        annotators.append("parse, sentiment");
        //properties.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
        return this;
    }

    public PipelineBuilder extractRelations() {
        checkForExistingAnnotators();
        annotators.append("relation");
        return this;
    }

    public PipelineBuilder extractCoref() {
        checkForExistingAnnotators();
        annotators.append("parse, coref");
        properties.setProperty("coref.maxMentionDistance", "15");
        properties.setProperty("coref.algorithm", "statistical");
        return this;
    }

    public PipelineBuilder openIE() {
        checkForExistingAnnotators();
        annotators.append("ner, depparse, natlog, openie");

        return this;
    }

    public PipelineBuilder defaultStopWordAnnotator() {
        checkForExistingAnnotators();
        annotators.append("stopword");
        properties.setProperty("customAnnotatorClass.stopword", StopwordAnnotator.class.getName());
        properties.setProperty(StopwordAnnotator.STOPWORDS_LIST, AbstractTextProcessor.DEFAULT_STOP_WORD_LIST);
        return this;
    }

    public PipelineBuilder customStopWordAnnotator(String customStopWordList, boolean checkLemma) {
        checkForExistingAnnotators();
        String stopWordList;
        if (annotators.indexOf("stopword") >= 0) {
            throw new RuntimeException("A standard stopword annotator already exist!");
        } else {
            String annoName = name + "_" + UUID.randomUUID().toString();
            annotators.append(annoName);
            properties.setProperty("customAnnotatorClass." + annoName, StopwordAnnotator.class.getName());
            if (customStopWordList.startsWith("+")) {
                stopWordList = AbstractTextProcessor.DEFAULT_STOP_WORD_LIST + "," + customStopWordList.replace("+,", "").replace("+", "");
            } else {
                stopWordList = customStopWordList;
            }
        }
        properties.setProperty(StopwordAnnotator.STOPWORDS_LIST, stopWordList);
        properties.setProperty(StopwordAnnotator.CHECK_LEMMA, Boolean.toString(checkLemma));
        
        return this;
    }

    public PipelineBuilder stopWordAnnotator(Properties properties) {
        properties.entrySet().stream().forEach((entry) -> {
            this.properties.setProperty((String) entry.getKey(), (String) entry.getValue());
        });
        return this;
    }

    public PipelineBuilder threadNumber(int threads) {
        this.threadsNumber = threads;
        return this;
    }

    public PipelineBuilder withCustomModels(String modelPaths) {
        properties.setProperty("ner.model", modelPaths);
        return this;
    }

    public PipelineBuilder withCustomLemmas(String filePath) {
        //annotators.append("custom.lemma");
        annotators.setLength(0);
        annotators.append("tokenize,ssplit,pos,custom.lemma,ner");
        properties.setProperty("custom.lemma.lemmaFile", filePath);
        properties.setProperty("customAnnotatorClass.custom.lemma", GermanLemmaAnnotator.class.getName());
        return this;
    }

    public StanfordCoreNLP build() {
        properties.setProperty("annotators", annotators.toString());
        properties.setProperty("threads", String.valueOf(threadsNumber));
        StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);
        return pipeline;
    }

    public static List<String> getDefaultStopwords() {
        List<String> stopwords = new ArrayList<>();
        Arrays.stream(AbstractTextProcessor.DEFAULT_STOP_WORD_LIST.split(",")).forEach(s -> {
            stopwords.add(s.trim());
        });

        return stopwords;
    }

    public static List<String> getCustomStopwordsList(String customStopWordList) {
        String stopWordList;
        if (customStopWordList.startsWith("+")) {
            stopWordList = AbstractTextProcessor.DEFAULT_STOP_WORD_LIST + "," + customStopWordList.replace("+,", "").replace("+", "");
        } else {
            stopWordList = customStopWordList;
        }

        List<String> list = new ArrayList<>();
        Arrays.stream(stopWordList.split(",")).forEach(s -> {
            list.add(s.trim());
        });

        return list;
    }
}
