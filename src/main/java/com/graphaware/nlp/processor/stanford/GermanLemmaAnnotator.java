package com.graphaware.nlp.processor.stanford;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;

import java.util.*;

import edu.stanford.nlp.io.*;
import edu.stanford.nlp.util.ArraySet;

public class GermanLemmaAnnotator implements Annotator {
    HashMap<String,String> wordToLemma = new HashMap<String,String>();

    public GermanLemmaAnnotator(String name, Properties props) {
        // load the lemma file
        // format should be tsv with word and lemma
        String lemmaFile = props.getProperty("custom.lemma.lemmaFile");
        List<String> lemmaEntries = IOUtils.linesFromFile(lemmaFile);
        //System.out.println("\n Preparing custom german lemmatizer.\n");
        for (String lemmaEntry : lemmaEntries) {
            if (!wordToLemma.containsKey(lemmaEntry.split("\\t")[1]))
                wordToLemma.put(lemmaEntry.split("\\t")[1], lemmaEntry.split("\\t")[0]);
        }
    }

    public void annotate(Annotation annotation) {
        System.out.println(" Custom Lemma Annotator");
        for (CoreLabel token : annotation.get(CoreAnnotations.TokensAnnotation.class)) {
            String lemma = wordToLemma.getOrDefault(token.word(), token.word());
            token.set(CoreAnnotations.LemmaAnnotation.class, lemma);
        }
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                CoreAnnotations.TextAnnotation.class,
                CoreAnnotations.TokensAnnotation.class,
                CoreAnnotations.SentencesAnnotation.class,
                CoreAnnotations.PartOfSpeechAnnotation.class
        )));
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.singleton(CoreAnnotations.LemmaAnnotation.class);
    }
}
