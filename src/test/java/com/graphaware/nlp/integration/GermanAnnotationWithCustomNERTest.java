package com.graphaware.nlp.integration;

import com.graphaware.nlp.StanfordNLPIntegrationTest;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class GermanAnnotationWithCustomNERTest extends StanfordNLPIntegrationTest {

    private static final String TEXT = "Die Violine oder Geige ist ein zu den Kastenhalslauten gehörendes Streichinstrument. Ihre vier Saiten (g – d1 – a1 – e2) werden mit einem Bogen gestrichen (Coll’arco), mit der Bogenstange leicht geschlagen (Col legno) oder mit den Fingern gezupft (Pizzicato). In der Tradition der klassischen europäischen Musik spielt die Violine eine wichtige Rolle – viele große Komponisten haben ihr bedeutende Teile ihres Schaffens gewidmet. Violinen werden von Geigenbauern hergestellt.";
    private static final String TEXT2 = "Schon auf den ersten Blick fällt die Wappenbratsche von Steffen Friedel durch ihre extravagante Gestaltung auf. Diese orientiert sich an der Form der f-Löcher einer Campanula von H. Bleffert sowie dem Wirbelkastenkopf der Dancing Masters Violine „Gillott“ 1720 von A. Stradivari. Wie das Design gefielen auch die weiteren Eigenschaften des Instruments: Sowohl die Spielbarkeit als auch die Ansprache überzeugten die Testmusiker in allen Belangen. Ihre Bestbewertungen gaben sie unter anderem für den offenen Klang, das Klangvolumen und die Variabilität. Insgesamt beschrieben die Juroren die Wappenbratsche als innovativ und gefällig. Die zweitbeste Bewertung innerhalb der objektiven Tests und der vierte Rang in der fertigungstechnischen Begutachtung ergänzten die sehr positiven Meinungen.\n" +
            "\n" +
            "Die Wappenbratsche (Korpuslänge: 415 mm) setzt sich aus hochwertiger tiroler Fichte für die Decke und bosnischem Ahorn für den Boden zusammen. Zargen und Hals entsprechen dem klassischen Geigenbau. Abrundung findet das Instrument durch eine Lackierung mit Leinöllack auf Grundlage von Dammar und Kopal.";
    @Test
    public void testAnnotatingTextWithDefaultCustomPipeline() {
        addModel();
        String q = "CALL ga.nlp.processor.addPipeline({language:'en', \n" +
                "name: \"muz-ner\",\n" +
                "textProcessor: \"com.graphaware.nlp.processor.stanford.StanfordTextProcessor\",\n" +
                "processingSteps: {tokenize: true, ner: true, customNER: 'music'},\n" +
                "language: \"german\"\n" +
                "})";
        executeInTransaction(q, emptyConsumer());
        executeInTransaction("CREATE (n:Document) SET n.text = $p0", buildSeqParameters(TEXT), emptyConsumer());

        executeInTransaction("MATCH (n:Document)\n" +
                "CALL ga.nlp.annotate({pipeline: \"muz-ner\", id: id(n), text: n.text, checkLanguage: false})\n" +
                "YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result) RETURN result", (result -> {
            assertTrue(result.hasNext());
        }));

        String vq = "MATCH (n) WHERE ANY(x IN labels(n) WHERE x STARTS WITH \"NE_\")\n" +
                "RETURN n.value, labels(n)";

        TestNLPGraph testNLPGraph = new TestNLPGraph(getDatabase());
        testNLPGraph.assertTagOccurrenceWithValueAndNeExist("Violine", "NE_Musical_instrument");
    }

    @Test
    public void testMultiplePipelinesWithSameModel() {
        clearDb();
        addModel();
        String q = "CALL ga.nlp.processor.addPipeline({\n" +
                "name: \"muz-ner\",\n" +
                "textProcessor: \"com.graphaware.nlp.processor.stanford.StanfordTextProcessor\",\n" +
                "processingSteps: {tokenize: true, ner: true, customNER: 'music'},\n" +
                "language: \"german\"\n" +
                "})";
        executeInTransaction(q, emptyConsumer());
        executeInTransaction("CREATE (n:Document:TEXT2) SET n.text = $p0", buildSeqParameters(TEXT2), emptyConsumer());
        executeInTransaction("MATCH (n:Document:TEXT2)\n" +
                "CALL ga.nlp.annotate({pipeline: \"muz-ner\", id: id(n), text: n.text, checkLanguage: false})\n" +
                "YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result) RETURN result", (result -> {
            assertTrue(result.hasNext());
        }));

        String q2 = "CALL ga.nlp.processor.addPipeline({\n" +
                "name: \"muz-ner2\",\n" +
                "textProcessor: \"com.graphaware.nlp.processor.stanford.StanfordTextProcessor\",\n" +
                "processingSteps: {tokenize: true, ner: true, customNER: 'music', dependency: true},\n" +
                "language: \"german\"\n" +
                "})";
        executeInTransaction(q2, emptyConsumer());
        executeInTransaction("CREATE (n:Document) SET n.text = $p0", buildSeqParameters(TEXT), emptyConsumer());

        executeInTransaction("MATCH (n:Document)\n" +
                "CALL ga.nlp.annotate({pipeline: \"muz-ner2\", id: id(n), text: n.text, checkLanguage: false})\n" +
                "YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result) RETURN result", (result -> {
            assertTrue(result.hasNext());
        }));


        TestNLPGraph testNLPGraph = new TestNLPGraph(getDatabase());
        testNLPGraph.assertTagOccurrenceWithValueAndNeExist("Violine", "NE_Musical_instrument");

    }

    private void addModel() {
        String path = getClass().getClassLoader().getResource("musical-de.gz").getPath();
        executeInTransaction("CALL ga.nlp.config.model.add('music', $p0)", buildSeqParameters(path), (result -> {
            assertTrue(result.hasNext());
        }));
    }
}
