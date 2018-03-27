package com.graphaware.nlp.integration;

import org.junit.Test;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class NasaLessonsLearnedTest extends StanfordNLPIntegrationTest {

    @Test
    public void testNASACustomNER() {
        String text = "Apollo 1, initially designated AS-204, was the first manned mission of the United States Apollo program, which had as its ultimate goal a manned lunar landing.";
        String train = getClass().getClassLoader().getResource("nasa-train.tsv").getPath();
        String test = getClass().getClassLoader().getResource("nasa-test.tsv").getPath();

        String q = "CALL ga.nlp.processor.train({textProcessor: \"com.graphaware.nlp.processor.stanford.StanfordTextProcessor\", modelIdentifier: \"test-ner\", alg: \"ner\", inputFile: \"" + test + "\", trainingParameters: {iter: 10}})";
        executeInTransaction(q, emptyConsumer());
        String t = "CALL ga.nlp.processor.test({textProcessor: \"com.graphaware.nlp.processor.stanford.StanfordTextProcessor\", modelIdentifier: \"test-ner\", alg: \"ner\", inputFile: \"" + test + "\", trainingParameters: {iter: 10}})";
        executeInTransaction(t, emptyConsumer());

        // Create pipeline
        String addPipelineQuery = "CALL ga.nlp.processor.addPipeline({textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', name: 'customNER', processingSteps: {tokenize: true, ner: true, sentiment: false, dependency: true, customNER: \"test-ner\"}})";
        executeInTransaction(addPipelineQuery, emptyConsumer());

        // Import some text
        executeInTransaction("CREATE (n:Document) SET n.text = {text}", Collections.singletonMap("text", text), emptyConsumer());

        // Annotate
        executeInTransaction("MATCH (n:Document) CALL ga.nlp.annotate({text: n.text, id:id(n), pipeline: 'customNER', checkLanguage:false}) YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)", emptyConsumer());

        executeInTransaction("MATCH (n:Tag) RETURN [x IN labels(n) | x] AS labels, n.value AS val", (result -> {
            while (result.hasNext()) {
                Map<String, Object> record = result.next();
                List<String> labels = (List<String>) record.get("labels");
                System.out.println(labels);
                System.out.println(record.get("val").toString());
            }
        }));


//        // Check if some labels are there
//        executeInTransaction("MATCH (n:NER_Mission) RETURN count(n) AS c", (result -> {
//            assertTrue((long) result.next().get("c") > 0);
//        }));
    }

    private void importDataset() {
        String query =
                "LOAD CSV WITH HEADERS FROM \"https://raw.githubusercontent.com/davidmeza1/doctopics/master/data/llis.csv\" AS line\n" +
                "WITH line, SPLIT(line.LessonDate, '-') AS date LIMIT 10\n" +
                "CREATE (lesson:Lesson { name: toInteger(line.`LessonId`) } )\n" +
                "SET lesson.year = toInteger(date[0]),\n" +
                "    lesson.month = toInteger(date[1]),\n" +
                "    lesson.day = toInteger(date[2]),\n" +
                "    lesson.title = (line.Title),\n" +
                "    lesson.abstract = (line.Abstract),\n" +
                "    lesson.lesson = (line.Lesson),\n" +
                "    lesson.org = (line.MissionDirectorate),\n" +
                "    lesson.safety = (line.SafetyIssue),\n" +
                "    lesson.url = (line.url)";

        executeInTransaction(query, emptyConsumer());
        executeInTransaction("MATCH (n:Lesson) SET n.text = n.title + '. ' + n.abstract + '. ' + n.lesson", emptyConsumer());
    }
}
