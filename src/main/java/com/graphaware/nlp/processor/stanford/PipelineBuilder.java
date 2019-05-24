package com.graphaware.nlp.processor.stanford;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.exception.InvalidPipelineException;
import com.graphaware.nlp.processor.AbstractTextProcessor;
import com.graphaware.nlp.processor.stanford.annotators.StopwordAnnotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.neo4j.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PipelineBuilder {

    private static final Log LOG = LoggerFactory.getLogger(PipelineBuilder.class);
    public static final String DEFAULT_ENGLISH_NER_MODEL = "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz";

    protected final Properties properties = new Properties();
    protected final StringBuilder annotators = new StringBuilder(); //basics annotators
    protected int threadsNumber = 4;
    private String language;
    
    protected final String name;

    public PipelineBuilder(String name) {

        this.name = name;
    }

    public PipelineBuilder(String name, String language) {
        this(name);
        this.language = language;
        if (language != null && !language.equalsIgnoreCase("en")) {
            try {
                String languageName = new Locale(language).getDisplayLanguage().toLowerCase();
                String propertiesFile = "StanfordCoreNLP-" + languageName.toLowerCase() + ".properties";
                InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(propertiesFile);
                LOG.info("Using properties file " + propertiesFile);
                properties.load(resourceAsStream);
            } catch (Exception ex) {
//                fallbackLoadProperties(language);
                throw new InvalidPipelineException("Language module not found for: " + language);
            }
        }
    }

    private void fallbackLoadProperties(String language) {
        if (language.equalsIgnoreCase("en")) {
            try {
                properties.load(getClass().getClassLoader().getResourceAsStream("StanfordCoreNLP"
                        + ".properties"));
            } catch (IOException ex) {
                throw new InvalidPipelineException("Language not found: " + language);
            }
        } else {
            throw new InvalidPipelineException("Language not found: " + language);
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
        String currentModels = properties.getProperty("ner.model", "");
//        if (currentModels.equalsIgnoreCase("") && language.equalsIgnoreCase("en")) {
//            currentModels = DEFAULT_ENGLISH_NER_MODEL;
//        }
        String sep = currentModels.trim().equalsIgnoreCase("") ? "" : ",";
        String newModels = modelPaths + sep + currentModels;
        LOG.info("Setting NER MODELS property to " + newModels);
        properties.setProperty("ner.model", newModels);
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

    public String getDefaultNERModel() {
        return properties.getProperty("ner.model", "");
    }
}
