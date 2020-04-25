package custom;

import core.DTNHost;
import core.Message;
import core.SimClock;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LikelihoodComparator {

    private static final Logger LOGGER = Logger.getLogger("admin");

    public static Message compare(Message message, DTNHost fromNode, DTNHost toNode) {
        String destinationCluster = message.getToGoRegions().get(message.getToGoRegions().size() - 1);
        boolean forwardMessage = false;
        Double fromLikelihood = likelihoodMobUpdate(fromNode, message);
        Double toLikelihood = likelihoodMobUpdate(toNode, message);
        Double fromConHistory = fromNode.getContactHistoryMap().get(destinationCluster);
        Double toConHistory = toNode.getContactHistoryMap().get(destinationCluster);

        if (toNode.isHasTaxiCustomer() && fromNode.isHasTaxiCustomer()) {
            if ((toLikelihood > fromLikelihood)
                    || (toConHistory > fromConHistory)) {
                forwardMessage = true;
            }
        } else if (toNode.isHasTaxiCustomer() && !fromNode.isHasTaxiCustomer()) {
            if (toLikelihood > -1 || (toConHistory > fromConHistory)) {
                forwardMessage = true;
            }
        } else if (!toNode.isHasTaxiCustomer() && fromNode.isHasTaxiCustomer()) {
            if (fromLikelihood <= -1 && (toConHistory > fromConHistory)) {
                forwardMessage = true;
            }
        } else if (!toNode.isHasTaxiCustomer() && !fromNode.isHasTaxiCustomer()) {
            if ((toConHistory > fromConHistory)) {
                forwardMessage = true;
            }
        }

        if (forwardMessage) {
            message.setTo(toNode);
            message.setOnTheRoad(true);
           /* System.out.println("message: " + message.getId()
                    + ", has transferred. From: " + fromNode.getName()
                    + ", To: " + toNode.getName());*/
            LOGGER.info(SimClock.getTimeString()+" "
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
                    + "', toTaxi conLikeliHood: '" + toConHistory+"'");
        }

        return message;
    }

    public static Double likelihoodMobUpdate(DTNHost node, Message message) {
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
}
