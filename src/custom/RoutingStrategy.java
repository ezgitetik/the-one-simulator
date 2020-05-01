package custom;

import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import custom.predictionclient.AkomPredictionClient;
import custom.predictionclient.BasePredictionClient;
import custom.predictionclient.CPTPlusPredictionClient;
import custom.predictionclient.TDAGPredictionClient;
import org.apache.log4j.Logger;
import org.mockito.cglib.core.CollectionUtils;
import org.mockito.internal.util.collections.ListUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RoutingStrategy {

    private static final Logger LOGGER = Logger.getLogger("file");

    private static final Settings s = new Settings();
    private static final String PREDICTION_MOD = s.getSetting("prediction");

    private static BasePredictionClient akomPredictionClient = new AkomPredictionClient();
    private static BasePredictionClient cptPlusPredictionClient = new CPTPlusPredictionClient();
    private static BasePredictionClient tdagPredictionClient = new TDAGPredictionClient();

    private static Map<List<Integer>, String> predictionResults = new HashMap<>();

    public static Message compare(Message message, DTNHost fromNode, DTNHost toNode) {
        String destinationCluster = message.getToGoRegions().get(message.getToGoRegions().size() - 1);

        Double fromLikelihood = likelihoodMobUpdate(fromNode, message);
        Double toLikelihood = likelihoodMobUpdate(toNode, message);

        Double fromConHistory = likelihoodConUpdate(fromNode, destinationCluster);
        //Double fromConHistory = calculatePredictedLikelihood(fromNode, message);

        Double toConHistory = likelihoodConUpdate(toNode, destinationCluster);
        //Double toConHistory = calculatePredictedLikelihood(toNode, message);

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
            //return -1d;
            return calculatePredictedLikelihood(node, message);
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
                likelihood.set((1 + ((double) (index + 1) / (double) message.getToGoRegions().size())));
            }
        });

        return likelihood.get();
    }

    public static Double calculatePredictedLikelihood(DTNHost node, Message message) {
        AtomicReference<Double> likelihood = new AtomicReference<>();
        likelihood.set(-1.0);
        String nextCluster = predictNextCluster(node);
        if (nextCluster != null) {
            IntStream.range(0, message.getToGoRegions().size()).forEach(index -> {
                if (message.getToGoRegions().get(index).equals(nextCluster)) {
                    likelihood.set((1 + ((double) (index+1) / (double) message.getToGoRegions().size())));
                }
            });
        }

        return likelihood.get();
    }

    private static String predictNextCluster(DTNHost node) {
        BasePredictionClient predictionClient = getPredictionClient();
        List<Integer> lastClusters = getPreviousClusterIds(node);
        String nextCluster = null;
        try {
            if (lastClusters.size() > 0) {
                if (predictionResults.get(lastClusters) != null) {
                    nextCluster = "cluster" + predictionResults.get(lastClusters);
                } else {
                    String nextClusterNumber = predictionClient.getPrediction(lastClusters);
                    if (nextClusterNumber != null) {
                        nextCluster = "cluster" + nextClusterNumber;
                        predictionResults.put(lastClusters, nextClusterNumber);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nextCluster;
    }

    private static List<Integer> getPreviousClusterIds(DTNHost node) {
        List<Integer> previousClusterIds = new ArrayList<>();
        if (node.isTaxiOnReturnPath()) {
            for (int i = node.getCurrentPointIndex(); i < node.getAllRegions().size(); i++) {
                Integer clusterId = Integer.parseInt(node.getAllRegions().get(i).getRegion().replace("cluster", ""));
                if (!previousClusterIds.contains(clusterId)) {
                    previousClusterIds.add(clusterId);
                    if (previousClusterIds.size() == 4) break;
                }
            }
        } else {
            for (int i = node.getCurrentPointIndex(); i >= 0; i--) {
                Integer clusterId = Integer.parseInt(node.getAllRegions().get(i).getRegion().replace("cluster", ""));
                if (!previousClusterIds.contains(clusterId)) {
                    previousClusterIds.add(clusterId);
                    if (previousClusterIds.size() == 4) break;
                }
            }
        }
        Collections.reverse(previousClusterIds);
        return previousClusterIds;
    }

    private static BasePredictionClient getPredictionClient() {
        if(PREDICTION_MOD.equals("akom")) return akomPredictionClient;
        if(PREDICTION_MOD.equals("tdag")) return tdagPredictionClient;
        if(PREDICTION_MOD.equals("cpt")) return cptPlusPredictionClient;

        // default akomPredictionClient
        return akomPredictionClient;
    }

}
