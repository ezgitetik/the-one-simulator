package custom.predictionclient;

import java.io.IOException;
import java.util.Arrays;

public class PredictionClientTest {

    public static void main(String[] args) throws IOException {

        BasePredictionClient akomPredictionClient = new AkomPredictionClient();
        BasePredictionClient cptPlusPredictionClient = new CPTPlusPredictionClient();
        BasePredictionClient tdagPredictionClient = new TDAGPredictionClient();

        System.out.println(akomPredictionClient.getPrediction(Arrays.asList(12,12,12)));
        System.out.println(cptPlusPredictionClient.getPrediction(Arrays.asList(12,12,12)));
        System.out.println(tdagPredictionClient.getPrediction(Arrays.asList(12,12,12)));

    }

}
