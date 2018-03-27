package com.graphaware.nlp.processor.stanford;

import com.graphaware.nlp.processor.AbstractTextProcessor;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class PipelineBuilder {

    protected final Properties properties = new Properties();
    protected final StringBuilder annotators = new StringBuilder(); //basics annotators
    protected int threadsNumber = 4;
    
    protected final String name;

    public PipelineBuilder(String name) {
        this.name = name;
    }
    
    public PipelineBuilder tokenize() {
        checkForExistingAnnotators();
        annotators.append("tokenize, ssplit, pos, lemma");
        return this;
    }

    public PipelineBuilder extractNEs() {
        checkForExistingAnnotators();
        annotators.append("ner");
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
        annotators.append("parse");
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
        annotators.append("mention, coref");
        properties.setProperty("coref.doClustering", "true");
        properties.setProperty("coref.md.type", "rule");
        properties.setProperty("coref.mode", "statistical");
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
//            String alreadyexistingStopWordList = properties.getProperty(StopwordAnnotator.STOPWORDS_LIST);
//            stopWordList = alreadyexistingStopWordList + "," + customStopWordList;
            throw new RuntimeException("A standard stopword annotator already exist!");
        } else {
            String annoName = "stopword_" + name;
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

    public StanfordCoreNLP build() {
        properties.setProperty("annotators", annotators.toString());
        //properties.setProperty("ner.model", customNEs.toString());
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
