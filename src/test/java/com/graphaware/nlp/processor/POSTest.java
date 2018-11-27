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

import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Sentence;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.domain.TagOccurrence;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.processor.stanford.StanfordTextProcessor;
import com.graphaware.nlp.util.TestAnnotatedText;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static com.graphaware.nlp.util.TagUtils.newTag;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class POSTest {

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
        String QUESTION_TEXT_1 = "Who invented papyrus?";
        String QUESTION_TEXT_2 = "What did Newton discover?";
        String QUESTION_TEXT_3 = "";
        String QUESTION_TEXT_4 = "";
        String QUESTION_TEXT_5 = "";
        String QUESTION_TEXT_6 = "";
        String QUESTION_TEXT_7 = "";

        AnnotatedText annotatedText = textProcessor.annotateText(QUESTION_TEXT_1, PIPELINE_DEFAULT);
        TestAnnotatedText test = new TestAnnotatedText(annotatedText);
        test.assertSentencesCount(1);
        test.assertTag(newTag("who", Collections.emptyList(), Collections.singletonList("WP")));
        test.assertTag(newTag("invent", Collections.emptyList(), Collections.singletonList("VBD")));
        test.assertTag(newTag("papyrus", Collections.emptyList(), Collections.singletonList("NN")));

        annotatedText = textProcessor.annotateText(QUESTION_TEXT_2, PIPELINE_DEFAULT);
        test = new TestAnnotatedText(annotatedText);
        test.assertSentencesCount(1);
        test.assertTag(newTag("what", Collections.emptyList(), Collections.singletonList("WP")));
        test.assertTag(newTag("do", Collections.emptyList(), Collections.singletonList("VBD")));
        test.assertTag(newTag("Newton", Collections.singletonList("PERSON"), Collections.singletonList("NNP")));
        test.assertTag(newTag("discover", Collections.emptyList(), Collections.singletonList("VB")));
    }
    

}
