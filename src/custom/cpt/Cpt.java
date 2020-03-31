package custom.cpt;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Item;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Sequence;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.SequenceDatabase;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.SequenceStatsGenerator;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.CPT.CPT.CPTPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.CPT.CPTPlus.CPTPlusPredictor;
import ca.pfv.spmf.test.MainTestCPT;
import ca.pfv.spmf.test.MainTestCPTPlus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map;

public class Cpt {

    public static void main(String[] arg) throws IOException {
        String inputPath = fileToPath("contextCPT.txt");
        SequenceDatabase trainingSet = new SequenceDatabase();
        trainingSet.loadFileSPMFFormat(inputPath, 2147483647, 0, 2147483647);
        System.out.println("--- Training sequences ---");
        Iterator var4 = trainingSet.getSequences().iterator();

        while(var4.hasNext()) {
            Sequence sequence = (Sequence)var4.next();
            System.out.println(sequence.toString());
        }

        System.out.println();
        SequenceStatsGenerator.prinStats(trainingSet, " training sequences ");
        String optionalParameters = "splitLength:6 splitMethod:0 recursiveDividerMin:1 recursiveDividerMax:5";
        CPTPredictor predictionModel = new CPTPredictor("CPT", optionalParameters);
        predictionModel.Train(trainingSet.getSequences());
        Sequence sequence = new Sequence(0);
        sequence.addItem(new Item(1));
        sequence.addItem(new Item(4));
        sequence.addItem(new Item(2));
        Sequence thePrediction = predictionModel.Predict(sequence);
        System.out.println("For the sequence <(1),(4)>, the prediction for the next symbol is: +" + thePrediction);
        System.out.println();
        System.out.println("To make the prediction, the scores were calculated as follows:");
        Map<Integer, Float> countTable = predictionModel.getCountTable();
        Iterator var9 = countTable.entrySet().iterator();

        while(var9.hasNext()) {
            Map.Entry<Integer, Float> entry = (Map.Entry)var9.next();
            System.out.println("symbol" + entry.getKey() + "\t score: " + entry.getValue());
        }

    }

    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        URL url = Cpt.class.getClassLoader().getResource(filename);
        return URLDecoder.decode(url.getPath(), "UTF-8");
    }

}
