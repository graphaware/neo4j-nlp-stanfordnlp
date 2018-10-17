package com.graphaware.nlp;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.configuration.SettingsConstants;
import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.stanford.StanfordTextProcessor;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.impl.proc.Procedures;
import org.reflections.Reflections;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public abstract class StanfordNLPIntegrationTest extends NLPIntegrationTest {

    @Override
    protected void registerProceduresAndFunctions(Procedures procedures) throws Exception {
        super.registerProceduresAndFunctions(procedures);
        Reflections reflections = new Reflections("com.graphaware.nlp.dsl");
        Set<Class<? extends AbstractDSL>> cls = reflections.getSubTypesOf(AbstractDSL.class);
        for (Class c : cls) {
            try {
                procedures.registerProcedure(c);
                procedures.registerFunction(c);
            } catch (Exception e) {
                //
            }
        }
    }

    protected void createDefaultStanfordPipeline() {
        createPipeline(StanfordTextProcessor.class.getName(), "default");
        executeInTransaction("CALL ga.nlp.processor.pipeline.default({p0})", buildSeqParameters("default"), emptyConsumer());
    }

    protected void createDocumentAndAnnotate(String text) {
        executeInTransaction("CREATE (n:Document) SET n.text = {p0} WITH n CALL ga.nlp.annotate({text: n.text, id: id(n), checkLanguage: false}) YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)", buildSeqParameters(text), emptyConsumer());
    }

    protected void createPipelineWithDependency(String name) {
        executeInTransaction("CALL ga.nlp.processor.addPipeline({language:'en', name: {p0}, textProcessor: {p1} , processingSteps: {tokenize:true, ner:true, dependency: true}})",
                buildSeqParameters(name, StanfordTextProcessor.class.getName()), emptyConsumer());
    }

}
