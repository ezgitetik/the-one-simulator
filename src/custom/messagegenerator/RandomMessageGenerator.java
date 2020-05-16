package custom.messagegenerator;

import custom.ArffReader;
import core.DTNHost;
import core.Message;
import core.SimClock;
import core.World;
import custom.ArffRegion;
import custom.InfoMessage;
import custom.shortestpath.ShortestPathCalculator;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RandomMessageGenerator {

    private static int MESSAGE_COUNT = 0;
    private static final int MESSAGE_SIZE = 977273;
    private static final int CLUSTER_COUNT = 40;
    private static final int FINAL_HOUR = 60 * 60 * 22;
    private static final int START_HOUR = 0;//60 * 60 * 6;

    private static final Random random = new Random();
    private static double LAST_MESSAGE_CREATE_TIME = 0;
    private static final Logger LOGGER = Logger.getLogger("file");

    private static final List<String> farClusters = Arrays.asList("cluster22", "cluster13");
    private static final List<String> centralClusters = Arrays.asList("cluster14", "cluster12");
    private static final List<String> centralAndFarClusters = Arrays.asList("cluster22", "cluster30");

    private static final MessageGenerationType MESSAGE_GENERATION_TYPE = MessageGenerationType.BETWEEN_TWO_CENTRAL_CLUSTERS;
    private static final MessageGenerationFrequency MESSAGE_GENERATION_FREQUENCY = MessageGenerationFrequency.ONE_MESSAGE_PER_MINUTE;

    public static Message generateMessage(DTNHost fromHost) {
        Message message = null;
        if (SimClock.getTime() > START_HOUR && SimClock.getTime() > elapsedTimeAfterLastMessageGeneration() && SimClock.getTime() < FINAL_HOUR) {
            if (MESSAGE_GENERATION_TYPE == MessageGenerationType.UNIFORM) {
                String sourceCluster = getSourceCluster(fromHost);
                String destinationCluster = pickRandomDestinationCluster(sourceCluster);
                message = buildMessage(fromHost, sourceCluster, destinationCluster);
            } else if (MESSAGE_GENERATION_TYPE == MessageGenerationType.BETWEEN_TWO_FAR_DISTANCE_CLUSTERS) {
                String sourceCluster = getSourceCluster(fromHost);
                if (farClusters.contains(sourceCluster)) {
                    String destinationCluster = farClusters.stream()
                            .filter(cluster -> !cluster.equalsIgnoreCase(sourceCluster))
                            .collect(Collectors.toList())
                            .get(0);
                    message = buildMessage(fromHost, sourceCluster, destinationCluster);
                }
            } else if (MESSAGE_GENERATION_TYPE == MessageGenerationType.BETWEEN_TWO_CENTRAL_CLUSTERS) {
                String sourceCluster = getSourceCluster(fromHost);
                if (centralClusters.contains(sourceCluster)) {
                    String destinationCluster = centralClusters.stream()
                            .filter(cluster -> !cluster.equalsIgnoreCase(sourceCluster))
                            .collect(Collectors.toList())
                            .get(0);
                    message = buildMessage(fromHost, sourceCluster, destinationCluster);
                }
            } else if (MESSAGE_GENERATION_TYPE == MessageGenerationType.BETWEEN_CENTRAL_AND_FAR_DISTANCE_CLUSTERS) {
                String sourceCluster = getSourceCluster(fromHost);
                if (centralAndFarClusters.contains(sourceCluster)) {
                    String destinationCluster = centralAndFarClusters.stream()
                            .filter(cluster -> !cluster.equalsIgnoreCase(sourceCluster))
                            .collect(Collectors.toList())
                            .get(0);
                    message = buildMessage(fromHost, sourceCluster, destinationCluster);
                }
            }
        }
        return message;
    }

    private static double elapsedTimeAfterLastMessageGeneration() {
        if (MESSAGE_GENERATION_FREQUENCY == MessageGenerationFrequency.ONE_MESSAGE_PER_MINUTE) {
            return RandomMessageGenerator.LAST_MESSAGE_CREATE_TIME + (60);
        } else {
            return RandomMessageGenerator.LAST_MESSAGE_CREATE_TIME + (2 * 60);
        }
    }

    private static Message buildMessage(DTNHost fromHost, String sourceCluster, String destinationCluster) {
        incrementMessageCount();
        List<String> shortestPath = getMessageShortestPath(sourceCluster, destinationCluster);
        String messageId = createMessageId();
        Message message = new Message(fromHost, null, messageId, MESSAGE_SIZE);
        message.addToGoRegions(shortestPath);
        message.setWatched(true);
        message.setCreatedTime(SimClock.getTime());
        // message.setTtl(120); //120 minutes

        LAST_MESSAGE_CREATE_TIME = SimClock.getTime();

        LOGGER.info(SimClock.getTimeString() + " " + InfoMessage.MESSAGE_CREATED
                + ", messageId: '" + message.getId()
                + "', carrier taxiName: '" + fromHost.getName()
                + "', cluster: '" + fromHost.getCurrentCluster()
                + "', toGoClusters: '" + String.join(",", shortestPath) + "'");

        return message;
    }

    private static String getSourceCluster(DTNHost carrierHost) {
        return carrierHost.getCurrentCluster() == null
                ?
                getMostClosestArffRegionByPointsAndList(carrierHost.getLocation().getxRoute(),
                        carrierHost.getLocation().getyRoute(),
                        carrierHost.getAllRegions()).getRegion()
                :
                carrierHost.getCurrentCluster();
    }

    public static DTNHost pickRandomCarrierHost(World world) {
        List<DTNHost> runningHosts = world.getHosts();
        return world.getHosts().get(random.nextInt(runningHosts.size()));
    }

    private static String pickRandomDestinationCluster(String sourceCluster) {
        int randomClusterId = random.nextInt(CLUSTER_COUNT);
        String randomClusterName = "cluster" + randomClusterId;
        if (randomClusterName.equalsIgnoreCase(sourceCluster)) {
            pickRandomDestinationCluster(sourceCluster);
        }
        return randomClusterName;
    }

    private static String createMessageId() {
        return "WM_" + MESSAGE_COUNT;
    }

    private static void incrementMessageCount() {
        MESSAGE_COUNT++;
    }

    private static List<String> getMessageShortestPath(String sourceCluster, String destinationCluster) {
        List<String> toGoRegions = ShortestPathCalculator.getShortestPath(sourceCluster, destinationCluster);
        Collections.reverse(toGoRegions);
        List<String> shortestPath = new ArrayList<>();
        shortestPath.add(sourceCluster);
        shortestPath.addAll(toGoRegions);
        return shortestPath;
    }

    private static ArffRegion getMostClosestArffRegionByPointsAndList(Double xPoint, Double yPoint, List<ArffRegion> arffRegions) {
        Map<Double, ArffRegion> hypotenuseCluster = new HashMap<>();
        arffRegions.forEach(arffRegion -> {
            hypotenuseCluster.put(Math.hypot(xPoint - arffRegion.getxPoint(), yPoint - arffRegion.getyPoint()), arffRegion);
        });
        OptionalDouble key = hypotenuseCluster.keySet().stream().mapToDouble(v -> v).min();
        return hypotenuseCluster.get(key.getAsDouble());
    }

}
