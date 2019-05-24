package com.graphaware.nlp.integration;

import com.graphaware.nlp.StanfordNLPIntegrationTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class GermanPipelineTest extends StanfordNLPIntegrationTest {

    private static String TEXT = "Schon auf den ersten Blick fällt die Wappenbratsche von Steffen Friedel durch ihre extravagante Gestaltung auf. Diese orientiert sich an der Form der f-Löcher einer Campanula von H. Bleffert sowie dem Wirbelkastenkopf der Dancing Masters Violine „Gillott“ 1720 von A. Stradivari. Wie das Design gefielen auch die weiteren Eigenschaften des Instruments: Sowohl die Spielbarkeit als auch die Ansprache überzeugten die Testmusiker in allen Belangen. Ihre Bestbewertungen gaben sie unter anderem für den offenen Klang, das Klangvolumen und die Variabilität. Insgesamt beschrieben die Juroren die Wappenbratsche als innovativ und gefällig. Die zweitbeste Bewertung innerhalb der objektiven Tests und der vierte Rang in der fertigungstechnischen Begutachtung ergänzten die sehr positiven Meinungen. Die Wappenbratsche (Korpuslänge: 415 mm) setzt sich aus hochwertiger tiroler Fichte für die Decke und bosnischem Ahorn für den Boden zusammen. Zargen und Hals entsprechen dem klassischen Geigenbau. Abrundung findet das Instrument durch eine Lackierung mit Leinöllack auf Grundlage von Dammar und Kopal.";

    @Test
    public void testAnnotatingTextWithDefaultGermanPipeline() {
        String q = "CALL ga.nlp.processor.addPipeline({\n" +
                "name: \"de-muz-noner\",\n" +
                "textProcessor: \"com.graphaware.nlp.processor.stanford.StanfordTextProcessor\",\n" +
                "processingSteps: {tokenize: true, ner: true},\n" +
                "language: \"german\"\n" +
                "})";
        executeInTransaction(q, emptyConsumer());
        executeInTransaction("CREATE (n:Document) SET n.text = $p0", buildSeqParameters(TEXT), emptyConsumer());

        executeInTransaction("MATCH (n:Document)\n" +
                "CALL ga.nlp.annotate({pipeline: \"de-muz-noner\", id: id(n), text: n.text, checkLanguage: false})\n" +
                "YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result) RETURN result", (result -> {
            assertTrue(result.hasNext());
        }));

    }

    @Test
    public void testFrench() {
        String q = "CALL ga.nlp.processor.addPipeline({\n" +
                "name: \"french\",\n" +
                "textProcessor: \"com.graphaware.nlp.processor.stanford.StanfordTextProcessor\",\n" +
                "processingSteps: {tokenize: true, ner: true},\n" +
                "language: \"fr\"\n" +
                "})";
        executeInTransaction(q, emptyConsumer());
    }

}
