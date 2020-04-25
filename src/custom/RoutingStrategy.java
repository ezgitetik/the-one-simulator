package custom;

import core.DTNHost;
import core.Message;
import core.SimClock;
import custom.predictionclient.AkomPredictionClient;
import custom.predictionclient.BasePredictionClient;
import custom.predictionclient.CPTPlusPredictionClient;
import custom.predictionclient.TDAGPredictionClient;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RoutingStrategy {

    private static final Logger LOGGER = Logger.getLogger("file");

    private static BasePredictionClient akomPredictionClient = new AkomPredictionClient();
    private static BasePredictionClient cptPlusPredictionClient = new CPTPlusPredictionClient();
    private static BasePredictionClient tdagPredictionClient = new TDAGPredictionClient();

    public static Message compare(Message message, DTNHost fromNode, DTNHost toNode) {
        String destinationCluster = message.getToGoRegions().get(message.getToGoRegions().size() - 1);

        Double fromLikelihood = likelihoodMobUpdate(fromNode, message);
        Double toLikelihood = likelihoodMobUpdate(toNode, message);
        Double fromConHistory = likelihoodConUpdate(fromNode, destinationCluster);
        Double toConHistory = likelihoodConUpdate(toNode, destinationCluster);

        boolean forwardMessage = false;

        if (toLikelihood > fromLikelihood) {
            if (toLikelihood > 0) {
                forwardMessage = true;
                if (fromLikelihood < 0) {
                    // TODO: drop message from "from" node
                }
            } else {
                forwardMessage = false;
            }
        } else {
            if (toConHistory > fromConHistory) {
                forwardMessage = true;
                if (fromLikelihood < 0) {
                    // TODO: drop message from "from" node
                }
            } else {
                forwardMessage = false;
                if (fromLikelihood < 0) {
                    // TODO: drop message from "from" node
                    // bu case biraz garip inceleyelim
                }
            }
        }

        if (forwardMessage) {
            message.setTo(toNode);
            message.setOnTheRoad(true);
           /* System.out.println("message: " + message.getId()
                    + ", has transferred. From: " + fromNode.getName()
                    + ", To: " + toNode.getName());*/
            LOGGER.info(SimClock.getTimeString() + " "
                    + InfoMessage.MESSAGE_TRANSFERRED_TO_ANOTHER_TAXI
                    + ", messageId: '" + message.getId()
                    + ", cluster: '" + fromNode.getCurrentCluster()
                    + "', from taxiName: '" + fromNode.getName()
                    + "', to taxiName: '" + toNode.getName()
                    + "', fromTaxiHasCustomer: '" + fromNode.isHasTaxiCustomer()
                    + "', toTaxiHasCustomer: '" + toNode.isHasTaxiCustomer()
                    + "', fromTaxi LikeliHood: '" + fromLikelihood
                    + "', toTaxi LikeliHood: '" + toLikelihood
                    + "', fromTaxi conLikeliHood: '" + fromConHistory
                    + "', toTaxi conLikeliHood: '" + toConHistory + "'");
        }

        return message;
    }

    public static Double likelihoodConUpdate(DTNHost node, String destinationCluster) {
        return node.getContactHistoryMap().get(destinationCluster);
    }

    public static Double likelihoodMobUpdate(DTNHost node, Message message) {

        if (node.isHasTaxiCustomer()) {
            return calculateLikelihood(node, message);
        } else {
            return -1D;
            //return calculatePredictedLikelihood(node, message);
        }
    }

    public static Double calculateLikelihood(DTNHost node, Message message) {
        AtomicReference<Double> likelihood = new AtomicReference<>();
        likelihood.set(-1.0);
        List<String> nodeClusters = node.getFutureRegions()
                .stream()
                .map(ArffRegion::getRegion)
                .collect(Collectors.toList());

        IntStream.range(0, message.getToGoRegions().size()).forEach(index -> {
            if (nodeClusters.contains(message.getToGoRegions().get(index))) {
                likelihood.set((1 + ((double) index / (double) message.getToGoRegions().size())));
            }
        });

        return likelihood.get();
    }

    public static Double calculatePredictedLikelihood(DTNHost node, Message message) {
        AtomicReference<Double> likelihood = new AtomicReference<>();
        likelihood.set(-1.0);
        String nextCluster = predictNextCluster(node);
        if(nextCluster != null){
            IntStream.range(0, message.getToGoRegions().size()).forEach(index -> {
                if ( message.getToGoRegions().get(index).equals(nextCluster) ) {
                    likelihood.set((1 + ((double) index / (double) message.getToGoRegions().size())));
                }
            });
        }

        return likelihood.get();
    }

    public static String predictNextCluster(DTNHost node) {
        BasePredictionClient predictionClient = getPredictionClient();
        List<Integer> lastClusters = new ArrayList<>(); // get last 4 cluster from node
        String nextCluster = null;
        try {
            String nextClusterNumber = predictionClient.getPrediction(lastClusters);
            if(nextClusterNumber != null){
                nextCluster = "cluster" + nextClusterNumber;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nextCluster;
    }

    private static BasePredictionClient getPredictionClient() {
        return akomPredictionClient;
    }

}
