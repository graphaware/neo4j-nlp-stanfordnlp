/*
 *
 *
 */
package com.graphaware.nlp.processor.stanford.model;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.Map;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.sequences.DocumentReaderAndWriter;
import edu.stanford.nlp.sequences.SeqClassifierFlags;
import edu.stanford.nlp.util.Triple;
import edu.stanford.nlp.util.StringUtils;

import com.graphaware.nlp.util.GenericModelParameters;


public class NERModelTool {

    private static final String MODEL_NAME = "NER";
    private static final String DEFAULT_PROPERTIES_FILE = "ner-config.properties";
    private static final DecimalFormat decFormat = new DecimalFormat("#0.00"); // for formating validation results with precision 2 decimals
    private final String modelDescr;
    private String entityType;
    private Properties props;

    private static final Logger LOG = LoggerFactory.getLogger(NERModelTool.class);

    public NERModelTool(String fileIn, String modelDescr, String lang, String propertiesFile) {
        this.entityType = null; // train only specific named entity; null = train all entities present in the training set
        this.modelDescr = modelDescr;
        this.props = processPropertiesFile(propertiesFile);
        if (fileIn != null && !fileIn.isEmpty()) {
            this.props.setProperty("trainFile", fileIn);
        }

    }

    public NERModelTool(String modelDescr, String lang) { this(null, modelDescr, lang, null); }

    public void train(String modelPath) {
        SeqClassifierFlags flags = new SeqClassifierFlags(props);
        CRFClassifier<CoreLabel> model = new CRFClassifier<CoreLabel>(flags);

        LOG.info("Starting the training ...");
        model.train();
        LOG.info("Training finished!");

        // @TODO: 29/03/2018 @vlasta when do we want to use a props file from disk instead of passing via procedure ?
//        // Save the model
//        if (modelPath == null || modelPath.isEmpty())
//            modelPath = props.getProperty("serializeTo");

        model.serializeClassifier(modelPath);
        LOG.info("Model saved to " + modelPath);
    }

    public String validate() {
        throw new UnsupportedOperationException("Method validate() not implemented yet (StanfordNLP Text Processor).");
        /*String result = "";
        if (this.fileValidate == null) {
            //List<EvaluationMonitor<NameSample>> listeners = new LinkedList<EvaluationMonitor<NameSample>>();
            try (ObjectStream<String> lineStream = openFile(fileIn); NameSampleDataStream sampleStream = new NameSampleDataStream(lineStream)) {
                LOG.info("Validation of " + MODEL_NAME + " started ...");
                // Using CrossValidator
                TokenNameFinderCrossValidator evaluator = new TokenNameFinderCrossValidator(lang, entityType, trainParams, null);
                // the second argument of 'evaluate()' gives number of folds (n), i.e. number of times the training-testing will be run (with data splitting train:test = (n-1):1)
                evaluator.evaluate(sampleStream, nFolds);
                result = "F = " + decFormat.format(evaluator.getFMeasure().getFMeasure())
                        + " (Precision = " + decFormat.format(evaluator.getFMeasure().getPrecisionScore())
                        + ", Recall = " + decFormat.format(evaluator.getFMeasure().getRecallScore()) + ")";
                LOG.info("Validation: " + result);
            } catch (IOException ex) {
                LOG.error("Error while opening training file: " + fileIn, ex);
                throw new RuntimeException(ex);
            } catch (Exception ex) {
                LOG.error("Error while evaluating " + MODEL_NAME + " model.", ex);
                throw new RuntimeException(ex);
            }
        } else {
          result = test(this.fileValidate, new NameFinderME((TokenNameFinderModel) model));
        }

        return result;*/
    }

    public String test(String file, String modelPath) {
        String result = "";
        AbstractSequenceClassifier<CoreLabel> model = loadClassifier(modelPath);

        try {
            System.out.println("\n---");
            Triple<Double,Double,Double> scores = model.classifyAndWriteAnswers(file, true);
            result = "F = " + decFormat.format(scores.third())
                    + " (Precision = " + decFormat.format(scores.first())
                    + ", Recall = " + decFormat.format(scores.second()) + ")";
            System.out.println("\n --- " + result);
            //System.out.println("  Precision = " + scores.first()); // precision
            //System.out.println("  Recall = " + scores.second()); // recall
            //System.out.println("  F1 = " + scores.third()); // F1 score
        } catch (IOException ex) {
            LOG.error("Couldn't open test file " + file + ". " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

        /*System.out.println("\n---");
        try {
            String fileContents = IOUtils.slurpFile(file);
            List<List<CoreLabel>> out = model.classify(fileContents);
            for (List<CoreLabel> sentence : out) {
                for (CoreLabel word : sentence) {
                    System.out.print(word.word() + '/' + word.get(CoreAnnotations.AnswerAnnotation.class) + ' ');
                }
                System.out.println();
            }
        } catch (IOException ex) {
            LOG.error("Could not open file " + file + ". " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }*/

        /*System.out.println("\n---");
        List<List<CoreLabel>> out = model.classifyFile(file); //
        for (List<CoreLabel> sentence : out) {
            for (CoreLabel word : sentence) {
                System.out.print(word.word() + '/' + word.get(CoreAnnotations.AnswerAnnotation.class) + ' ');
                //System.out.println("  * " + word.word());
                //System.out.print("  ** " + word.get(CoreAnnotations.AnswerAnnotation.class) + '\n');
            }
            System.out.println();
        }
        System.out.println();*/

        String S1 = "James Trescothick, senior global strategist, said there is the potential.";
        String S2 = "I go to school at Stanford University, which is located in California.";
        String S3 = "Vega 1 is currently in heliocentric orbit, with perihelion of 0.70 AU, aphelion of 0.98 AU and orbital period of 281 days.";
        String S4 = "It defines a heliospheric mission for ICE consisting of investigations of coronal mass ejections in coordination with ground-based observations, continued cosmic rays studies, and the Ulysses probe.";
//        System.out.println(model.classifyToString(S1));
//        System.out.println(model.classifyToString(S2));
//        System.out.println(model.classifyToString(S3));
//        System.out.println(model.classifyToString(S4));
        /*System.out.println(model.classifyWithInlineXML(S2));
        System.out.println(model.classifyToString(S2, "xml", true));*/

        return result;
    }

    private Properties processPropertiesFile(String path) {
        LOG.info("In processPropertiesFile()");
        if (path == null || path.isEmpty())
            path = this.DEFAULT_PROPERTIES_FILE;
        LOG.info("Properties file: " + path);

        Properties prop = new Properties();
        InputStream is = openFile(path);

        try {
            prop.load(is);
        } catch (IOException ex) {
            LOG.error("Unable to load properties. " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

        try {
            is.close();
        } catch (IOException ex) {
            LOG.error("Unable to close properties file. " + ex.getMessage());
        }

        return prop;
    }

    private Properties processPropertiesFile() {
        return processPropertiesFile(this.DEFAULT_PROPERTIES_FILE);
    }

    private InputStream openFile(String path) {
        InputStream is;
        try {
            if (path.startsWith("file://")) {
                is = new FileInputStream(new File(new URI(path)));
            } else if (path.startsWith("/")) {
                is = new FileInputStream(new File(path));
            } else {
                is = this.getClass().getResourceAsStream(path);
            }
        } catch (FileNotFoundException ex) {
            LOG.error("Unable to load file " + path + ". File not found. " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException(ex);
        } catch (URISyntaxException ex) {
            LOG.error("Unable to load file " + path + ". " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        return is;
    }

    private AbstractSequenceClassifier<CoreLabel> loadClassifier(String modelPath) {
        if (modelPath == null || modelPath.isEmpty())
            modelPath = props.getProperty("serializeTo");
        AbstractSequenceClassifier<CoreLabel> classifier;
        try {
            classifier = CRFClassifier.getClassifier(modelPath);
        } catch (IOException | ClassNotFoundException ex) {
            LOG.error("Couldn't load classifier " + modelPath + ". " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        LOG.info("Classifier " + modelPath + " successfully processed.");
        return classifier;
    }

}
