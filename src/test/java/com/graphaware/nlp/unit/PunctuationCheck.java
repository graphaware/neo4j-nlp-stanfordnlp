package com.graphaware.nlp.unit;

import com.graphaware.nlp.processor.stanford.StanfordTextProcessor;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class PunctuationCheck {

    private Pattern patternCheck = Pattern.compile(StanfordTextProcessor.getPunctRegexPattern(), Pattern.CASE_INSENSITIVE);

    @Test
    public void testBasicPunctuationCheck() {
        assertTrue(isPassingPunctCheck("hello"));
        assertTrue(isPassingPunctCheck("be"));
        assertTrue(isPassingPunctCheck("monetary"));
        assertTrue(isPassingPunctCheck("circuit"));

        assertFalse(isPassingPunctCheck("("));
        assertFalse(isPassingPunctCheck("-"));
        assertFalse(isPassingPunctCheck(";"));
        assertFalse(isPassingPunctCheck(","));

        assertTrue(isPassingPunctCheck("one-thousand"));

        assertFalse(isPassingPunctCheck("-lrb-"));
    }

    private boolean isPassingPunctCheck(String token) {
        Matcher match = patternCheck.matcher(token);
        System.out.println(patternCheck.matcher(token).find());
        return match.find();
    }

}
