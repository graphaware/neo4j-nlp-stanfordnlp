package com.graphaware.nlp.performance;

import com.graphaware.nlp.StanfordNLPIntegrationTest;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class AnnotationPerformanceTest extends StanfordNLPIntegrationTest {

    @Test
    public void testAnnotationOnSmallText() throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("textFile6k.java")));

    }

}
