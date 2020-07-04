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
    private static final String GEOMOBCON = s.getSetting("GEOMOBCON");
    private static final String SPContactHistory = s.getSetting("SP_CONTACT_HISTORY");

    private static BasePredictionClient akomPredictionClient = new AkomPredictionClient();
    private static BasePredictionClient cptPlusPredictionClient = new CPTPlusPredictionClient();
    private static BasePredictionClient tdagPredictionClient = new TDAGPredictionClient();

    private static Map<List<Integer>, String> predictionResults = new HashMap<>();

    public static Message compare(Message message, DTNHost fromNode, DTNHost toNode) {
        String destinationCluster = message.getToGoRegions().get(message.getToGoRegions().size() - 1);

        Double fromLikelihood = likelihoodMobUpdate(fromNode, message);
        Double toLikelihood = likelihoodMobUpdate(toNode, message);

        Double fromConHistory = null;
        Double toConHistory = null;

        if(SPContactHistory.equals("true")){
            fromConHistory = calculatePredictedLikelihood(fromNode, message);
            toConHistory = calculatePredictedLikelihood(toNode, message);
        } else {
            fromConHistory = likelihoodConUpdate(fromNode, destinationCluster);
            toConHistory = likelihoodConUpdate(toNode, destinationCluster);
        }

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
            message.getHostHistory().add(toNode.getName());

            LOGGER.info(SimClock.getTimeString() + " "
                            + "likelihood is found bigger at to node"
                            + ", messageId: '" + message.getId()
                            + "', from taxiName: '" + fromNode.getName()
                            + "', to taxiName: '" + toNode.getName() +"'");

            /*LOGGER.info(SimClock.getTimeString() + " "
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
                    + "', toTaxi conLikeliHood: '" + toConHistory + "'");*/
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

            if (GEOMOBCON.equals("true") || SPContactHistory.equals("true")) return nthOrderPrediction(node, message);
            return calculatePredictedLikelihood(node, message);
            //    return nthOrderPrediction(node, message);
        }
    }

    private static Double nthOrderPrediction(DTNHost node, Message message) {
        if (node.getSequence() == null) return -1d;
        List<Integer> lastClusters = getPreviousClusterIds(node);

        double result = -1d;
        int count;
        int N = Math.min(lastClusters.size(), 4);
        for (int i = 0; i < N - 1; i++) {
            List<Integer> lastClustersSubList = lastClusters.subList(i, N);
            count = nthOrderCount(node, lastClustersSubList);
            if (count != 0) {
                for (int k = 0; k < message.getToGoRegions().size(); k++) {
                    String toGoRegion = message.getToGoRegions().get(k);
                    int toGoRegionId = Integer.parseInt(toGoRegion.replace("cluster", ""));
                    List<Integer> lastClustersWithToGo = new ArrayList<>(lastClustersSubList);
                    if (k == 0 && toGoRegionId == lastClustersWithToGo.get(lastClustersWithToGo.size() - 1)) continue;

                    lastClustersWithToGo.add(toGoRegionId);
                    double tempResult = (double) nthOrderCount(node, lastClustersWithToGo) / (double) count;
                    if (tempResult > result) result = tempResult;
                }
                break;
            }
        }
        return result;
    }

    private static int nthOrderCount(DTNHost node, List<Integer> lastClusters) {
        List<List<Integer>> sequence = node.getSequence();
        int count = 0;
        for (List<Integer> sequenceLine : sequence) {
            if (Collections.indexOfSubList(sequenceLine, lastClusters) == 0) {
                count++;
            }
        }
        return count;
    }

    public static Double calculateLikelihood(DTNHost node, Message message) {
        AtomicReference<Double> likelihood = new AtomicReference<>();
        likelihood.set(-1.0);
        /*List<String> nodeClusters = node.getFutureRegions()
                .stream()
                .map(ArffRegion::getRegion)
                .collect(Collectors.toList());*/

        List<String> futureRegions = new ArrayList<>();
        for (ArffRegion arffRegion : node.getFutureRegions()) {
            futureRegions.add(arffRegion.getRegion());
        }


        for (int i = 0; i < message.getToGoRegions().size(); i++) {
            if (futureRegions.contains(message.getToGoRegions().get(i))) {
                likelihood.set((1 + ((double) (i + 1) / (double) message.getToGoRegions().size())));
            }
        }

        return likelihood.get();
    }

    public static Double calculatePredictedLikelihood(DTNHost node, Message message) {
        AtomicReference<Double> likelihood = new AtomicReference<>();
        likelihood.set(-1.0);
        List<Integer> lastClusters = getPreviousClusterIds(node);
        String predictedNextCluster = predictNextCluster(lastClusters);
        if (predictedNextCluster != null) {
            for (int i = 0; i < message.getToGoRegions().size(); i++) {
                if (message.getToGoRegions().get(i).equals(predictedNextCluster)) {
                    likelihood.set((1 + ((double) (i + 1) / (double) message.getToGoRegions().size())));
                }
            }
        }

        return likelihood.get();
    }

    // for nth order
//    public static Double calculatePredictedLikelihood(DTNHost node, Message message) {
//        AtomicReference<Double> likelihood = new AtomicReference<>();
//        likelihood.set(-1.0);
//        List<Integer> lastClusters = getPreviousClusterIds(node);
//        int N = Math.min(lastClusters.size(), 4);
//        for(int k=0; k<N-1 ; k++) {
//            List<Integer> lastClustersSubList = lastClusters.subList(k, N);
//            String predictedNextCluster = predictNextCluster(lastClustersSubList);
//            if (predictedNextCluster != null) {
//                for (int i = 0; i < message.getToGoRegions().size(); i++) {
//                    if (message.getToGoRegions().get(i).equals(predictedNextCluster)) {
//                        likelihood.set((1 + ((double) (i + 1) / (double) message.getToGoRegions().size())));
//                    }
//                }
//            }
//
//            if(likelihood.get() > 0) break;
//        }
//        return likelihood.get();
//    }

    // for predict next 3 clusters
//    public static Double calculatePredictedLikelihood(DTNHost node, Message message) {
//        AtomicReference<Double> likelihood = new AtomicReference<>();
//        likelihood.set(-1.0);
//        List<Integer> lastClusters = getPreviousClusterIds(node);
//        if (lastClusters.size() < 2) return likelihood.get();
//        List<String> futureRegions = new ArrayList<>();
//
//        List<List<Integer>> lastClustersSubListList = new ArrayList<>();
//        lastClustersSubListList.add(lastClusters);
//
//        for (int k = 0; k < 3; k++) {
//            List<Integer> lastClustersSubList = lastClustersSubListList.get(k);
//            String predictedNextCluster = predictNextCluster(lastClustersSubList);
//            if (predictedNextCluster != null && !predictedNextCluster.equals("")) {
//                futureRegions.add(predictedNextCluster);
//                List<Integer> deneme = new ArrayList<>();
//                if (lastClustersSubListList.get(k).size() != 1) {
//                    deneme = new ArrayList<>(lastClustersSubListList.get(k).subList(1, lastClustersSubListList.get(k).size()));
//                }
//                deneme.add(Integer.parseInt(predictedNextCluster.replace("cluster", "")));
//                lastClustersSubListList.add(deneme);
//            }else {
//                break;
//            }
//        }
//
//        for (int i = 0; i < message.getToGoRegions().size(); i++) {
//            if (futureRegions.contains(message.getToGoRegions().get(i))) {
//                likelihood.set((1 + ((double) (i + 1) / (double) message.getToGoRegions().size())));
//            }
//        }
//
//        return likelihood.get();
//    }

    private static String predictNextCluster(List<Integer> lastClusters) {
        BasePredictionClient predictionClient = getPredictionClient();
        String nextCluster = null;
        try {
            if (lastClusters.size() > 0) {
                if (predictionResults.get(lastClusters) != null) {
                    nextCluster = "cluster" + predictionResults.get(lastClusters);
                } else {
                    String nextClusterNumber = predictionClient.getPrediction(lastClusters);
                    if (nextClusterNumber != null && !nextClusterNumber.equals("")) {
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
        if (PREDICTION_MOD.equals("akom")) return akomPredictionClient;
        if (PREDICTION_MOD.equals("tdag")) return tdagPredictionClient;
        if (PREDICTION_MOD.equals("cpt")) return cptPlusPredictionClient;

        // default akomPredictionClient
        return akomPredictionClient;
    }

}
