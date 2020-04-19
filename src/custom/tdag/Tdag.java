package custom.tdag;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Item;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Sequence;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.SequenceDatabase;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.SequenceStatsGenerator;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.TDAG.TDAGPredictor;
import ca.pfv.spmf.test.MainTestTDAG;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;

public class Tdag {

    public static void main(String[] arg) throws IOException {

        String inputPath = fileToPath("taxi100_month1_week1_cpt_5transactions_combination.txt");
        SequenceDatabase trainingSet = new SequenceDatabase();
        trainingSet.loadFileSPMFFormat(inputPath, 2147483647, 0, 2147483647);
//        System.out.println("--- Training sequences ---");
//        Iterator var4 = trainingSet.getSequences().iterator();
//
//        while(var4.hasNext()) {
//            Sequence sequence = (Sequence)var4.next();
//            System.out.println(sequence.toString());
//        }
//
//        System.out.println();
        SequenceStatsGenerator.prinStats(trainingSet, " TDAG training sequences ");
        TDAGPredictor predictionModel = new TDAGPredictor("TDAG");
        predictionModel.Train(trainingSet.getSequences());
        Sequence sequence = new Sequence(0);
        // 21 -1 7 -1 24 -1 15 -1 24 -2
        sequence.addItem(new Item(21));
        sequence.addItem(new Item(7));
        sequence.addItem(new Item(24));
        double startTime=System.currentTimeMillis();

        Sequence thePrediction = predictionModel.Predict(sequence);

        double endTime=System.currentTimeMillis();
        System.out.println("delay: "+(endTime-startTime));
        System.out.println("For the sequence <(21),(7),(24)>, the prediction for the next symbol is: +" + thePrediction);
    }

    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        URL url = Tdag.class.getClassLoader().getResource(filename);
        return URLDecoder.decode(url.getPath(), "UTF-8");
    }
}
