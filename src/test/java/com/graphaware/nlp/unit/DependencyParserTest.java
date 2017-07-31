package com.graphaware.nlp.unit;

import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.stanford.StanfordTextProcessor;
import com.graphaware.nlp.util.ServiceLoader;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import org.junit.Test;

import java.util.List;
import java.util.Properties;
import java.util.Set;

public class DependencyParserTest {

    @Test
    public void testStanfordTypedDependenciesParsing() {
        String annotators = "tokenize, ssplit, pos, lemma, ner, parse, depparse, dcoref, relation";
        Properties properties = new Properties();
        properties.setProperty("annotators", annotators);
        StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);

        String text = "Donald Trump flew yesterday to New York City";
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        CoreMap sentence = sentences.get(0);
        System.out.println(sentence.toString());
        SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);
        System.out.println(graph);

        List<SemanticGraphEdge> edges = graph.edgeListSorted();
        for (SemanticGraphEdge edge : edges) {
            System.out.println(edge.getRelation().getSpecific());
            System.out.println(edge.getRelation().getShortName());
            System.out.println(String.format("Source is : %s - Target is : %s - Relation is : %s", edge.getSource(), edge.getTarget(), edge.getRelation()));
        }
    }

    @Test
    public void testStanfordNLPWithPredefinedProcessors() {
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        StanfordCoreNLP pipeline = ((StanfordTextProcessor) textProcessor).getPipeline(StanfordTextProcessor.DEPENDENCY_GRAPH);
        String text = "Donald Trump flew yesterday to New York City";
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        CoreMap sentence = sentences.get(0);
        System.out.println(sentence.toString());
        SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);
        System.out.println(graph);

        List<SemanticGraphEdge> edges = graph.edgeListSorted();
        for (SemanticGraphEdge edge : edges) {
            System.out.println(String.format("Source is : %s - Target is : %s - Relation is : %s", edge.getSource(), edge.getTarget(), edge.getRelation()));
        }
    }
}
