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

import java.util.*;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotator;

import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.Pair;

public class StopwordAnnotator implements Annotator, CoreAnnotation<Pair<Boolean, Boolean>> {

    /**
     * stopword annotator class name used in annotators property
     */
    public static final String ANNOTATOR_CLASS = "stopword";

    public static final String STANFORD_STOPWORD = ANNOTATOR_CLASS;

    /**
     * Property key to specify the comma delimited list of custom stopwords
     */
    public static final String STOPWORDS_LIST = "stopword-list";

    /**
     * Property key to specify if stopword list is case insensitive
     */
    public static final String IGNORE_STOPWORD_CASE = "ignore-stopword-case";

    /**
     * Property key to specify of StopwordAnnotator should check word lemma as
     * stopword
     */
    public static final String CHECK_LEMMA = "check-lemma";

    private static final Class<? extends Pair> boolPair = Pair.makePair(true, true).getClass();

    private final Properties props;
    private CharArraySetWrapper stopwords;
    private boolean checkLemma;

    public StopwordAnnotator(String annotatorClass, Properties props) {
        System.out.println(annotatorClass);
        this.props = props;
        checkStopwords();

    }

    protected void checkStopwords() {
        this.checkLemma = Boolean.parseBoolean(props.getProperty(CHECK_LEMMA, "false"));

        if (this.props.containsKey(STOPWORDS_LIST)) {
            String stopwordList = props.getProperty(STOPWORDS_LIST);
            boolean ignoreCase = Boolean.parseBoolean(props.getProperty(IGNORE_STOPWORD_CASE, "false"));
            this.stopwords = getStopWordList(stopwordList, ignoreCase);
        } else {
            this.stopwords = new CharArraySetWrapper();
        }
    }

    @Override
    public void annotate(Annotation annotation) {
        //checkStopwords();
        if (stopwords != null && stopwords.size() > 0 && annotation.containsKey(TokensAnnotation.class)) {
            List<CoreLabel> tokens = annotation.get(TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                boolean isWordStopword = stopwords.contains(token.word().toLowerCase());
                boolean isLemmaStopword = checkLemma && token.lemma() != null ? stopwords.contains(token.lemma().toLowerCase()) : false;
                Pair<Boolean, Boolean> pair = Pair.makePair(isWordStopword, isLemmaStopword);
                token.set(StopwordAnnotator.class, pair);
            }
        }
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.singleton(StopwordAnnotator.class);
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requires() {
        if (checkLemma) {
            return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                    CoreAnnotations.TextAnnotation.class,
                    CoreAnnotations.TokensAnnotation.class,
                    CoreAnnotations.CharacterOffsetBeginAnnotation.class,
                    CoreAnnotations.CharacterOffsetEndAnnotation.class,
                    CoreAnnotations.SentencesAnnotation.class,
                    CoreAnnotations.LemmaAnnotation.class
            )));
        } else {
            return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                    CoreAnnotations.TextAnnotation.class,
                    CoreAnnotations.TokensAnnotation.class,
                    CoreAnnotations.CharacterOffsetBeginAnnotation.class,
                    CoreAnnotations.CharacterOffsetEndAnnotation.class,
                    CoreAnnotations.SentencesAnnotation.class
            )));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Pair<Boolean, Boolean>> getType() {
        return (Class<Pair<Boolean, Boolean>>) boolPair;
    }

    public CharArraySetWrapper getStopWordList(String stopwordList, boolean ignoreCase) {
        String[] terms = stopwordList.split(",");
        CharArraySetWrapper stopwordSet = new CharArraySetWrapper(terms.length, ignoreCase);
        for (String term : terms) {
            stopwordSet.add(term.trim());
        }
        return CharArraySetWrapper.unmodifiableSet(stopwordSet);
    }

}
