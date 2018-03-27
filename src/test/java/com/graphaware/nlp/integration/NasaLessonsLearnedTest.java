package com.graphaware.nlp.integration;

import org.junit.Test;

public class NasaLessonsLearnedTest extends StanfordNLPIntegrationTest {

    @Test
    public void testNASACustomNER() {
        String text = "The exhaustive investigation of the fire and extensive reworking of the Apollo command modules postponed crewed launches until NASA officials cleared them for flight. Saturn IB schedules were suspended for nearly a year, and the launch vehicle that finally bore the designation AS-204 carried a lunar module, or LM, as the payload, instead of a CM. The missions of AS-201 and AS-202 with Apollo spacecraft aboard had been unofficially known as Apollo 1 and Apollo 2 missions. AS-203 carried only the aerodynamic nose cone.";
        String s = "";
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
