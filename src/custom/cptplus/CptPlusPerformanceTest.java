package custom.cptplus;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Item;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Sequence;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.SequenceDatabase;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.SequenceStatsGenerator;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.helpers.StatsLogger;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.CPT.CPT.CPTPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.CPT.CPTPlus.CPTPlusPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.DG.DGPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.Evaluator;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.LZ78.LZ78Predictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.Markov.MarkovAllKPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.Markov.MarkovFirstOrderPredictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.TDAG.TDAGPredictor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map;

public class CptPlusPerformanceTest {

    public static void main(String[] args) throws IOException {
        // performance with 5 combination
        URL url = CptPlus.class.getClassLoader().getResource("");
        Evaluator evaluator = new Evaluator(url.getPath());
        evaluator.addDataset("100taxi-month2-40.txt", 5000);
        evaluator.addPredictor(new DGPredictor("DG", "lookahead:4"));
        evaluator.addPredictor(new TDAGPredictor());
        evaluator.addPredictor(new CPTPlusPredictor("CPT+", "CCF:true CBS:true CCFmin:1 CCFmax:6"));
        evaluator.addPredictor(new CPTPredictor());
        evaluator.addPredictor(new MarkovFirstOrderPredictor());
        evaluator.addPredictor(new MarkovAllKPredictor("AKOM", "order:5"));
        //evaluator.addPredictor(new LZ78Predictor());
        StatsLogger results = evaluator.Start(1, 14.0F, true, true, true);
//
//        // performance without 5 combination
//        URL url1 = CptPlus.class.getClassLoader().getResource("");
//        Evaluator evaluator2 = new Evaluator(url1.getPath());
//        evaluator2.addDataset("taxi100_month1_week1_cpt_5transactions.txt", 5000);
//        evaluator2.addPredictor(new DGPredictor("DG", "lookahead:4"));
//        evaluator2.addPredictor(new TDAGPredictor());
//        evaluator2.addPredictor(new CPTPlusPredictor("CPT+", "CCF:true CBS:true CCFmin:1 CCFmax:6"));
//        evaluator2.addPredictor(new CPTPredictor());
//        evaluator2.addPredictor(new MarkovFirstOrderPredictor());
//        evaluator2.addPredictor(new MarkovAllKPredictor("AKOM", "order:5"));
//        //evaluator.addPredictor(new LZ78Predictor());
//        StatsLogger results2 = evaluator2.Start(1, 14.0F, true, true, true);
    }
}
