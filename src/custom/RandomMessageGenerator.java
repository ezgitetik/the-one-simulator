package custom;

import core.DTNHost;
import core.Message;
import core.SimClock;
import core.World;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomMessageGenerator {

    public static int MESSAGE_COUNT = 1;
    private static final int MESSAGE_SIZE = 977273;
    private static final int CLUSTER_COUNT = 40;
    private static final Random random = new Random();
    public static int MESSAGE_CREATE_DELAY = 2 * 60;
    public static double LAST_MESSAGE_CREATE_TIME = 0;
    private static final Logger LOGGER = Logger.getLogger("file");

    public static Message generateMessage(DTNHost carrierHost) {
        String messageId = getMessageId();
        String sourceCluster = getSourceCluster(carrierHost);

        String destinationCluster = pickRandomDestinationCluster(sourceCluster);
        List<String> shortestPath = getMessageShortestPath(sourceCluster, destinationCluster);

        Message message = new Message(carrierHost, null, messageId, MESSAGE_SIZE);
        message.addToGoRegions(shortestPath);
        message.setWatched(true);
        message.setCreatedTime(SimClock.getTime());
     //   message.setTtl(-1);
        message.setTtl(120); //120 minutes
        LAST_MESSAGE_CREATE_TIME = SimClock.getTime();
        LOGGER.info(SimClock.getTimeString()+" "+InfoMessage.MESSAGE_CREATED
                + ", messageId: '"+message.getId()
                + "', carrier taxiName: '"+carrierHost.getName()
                + "', toGoClusters: '" + shortestPath.stream().collect(Collectors.joining(","))+"'");
        return message;
    }

    private static String getSourceCluster(DTNHost carrierHost) {
        return carrierHost.getCurrentCluster() == null
                ?
                ArffReader.getMostClosestArffRegionByPointsAndList(carrierHost.getLocation().getxRoute(),
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

    private static String getMessageId() {
        String messageId = "WM_" + MESSAGE_COUNT;
        MESSAGE_COUNT++;
        return messageId;
    }

    private static List<String> getMessageShortestPath(String sourceCluster, String destinationCluster) {
        List<String> toGoRegions = null;
        try {
            toGoRegions = ArffReader.getShortestPath(sourceCluster, destinationCluster);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.reverse(toGoRegions);
        List<String> shortestPath = new ArrayList<>();
        shortestPath.add(sourceCluster);
        shortestPath.addAll(toGoRegions);
        return shortestPath;
    }

    public static void main(String[] args) {

    }
}
