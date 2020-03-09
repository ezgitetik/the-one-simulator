/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import core.DTNHost;
import core.Message;
import core.SimClock;
import core.World;
import custom.ArffReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * External event for creating a message.
 */
public class MessageCreateEvent extends MessageEvent {
    private int size;
    private int responseSize;
    private static Message message = null;

    /**
     * Creates a message creation event with a optional response request
     *
     * @param from         The creator of the message
     * @param to           Where the message is destined to
     * @param id           ID of the message
     * @param size         Size of the message
     * @param responseSize Size of the requested response message or 0 if
     *                     no response is requested
     * @param time         Time, when the message is created
     */
    public MessageCreateEvent(int from, int to, String id, int size,
                              int responseSize, double time) {
        super(from, to, id, time);
        this.size = size;
        this.responseSize = responseSize;
    }


    /**
     * Creates the message this event represents.
     */
    @Override
    public void processEvent(World world) {
        DTNHost to = world.getNodeByAddress(this.toAddr);
        DTNHost from = world.getNodeByAddress(this.fromAddr);
        Message m = new Message(from, to, this.id, this.size);
        String watchedTaxi = "taxi-528";

        // TODO:: this must be updated dynamically
        if (!world.isWatchedMessageCreated() && from.toString().equals(watchedTaxi)) {
            String sourceCluster = ArffReader.getMostClosestArffRegionByPointsAndList(from.getLocation().getxRoute(), from.getLocation().getyRoute(), from.getAllRegions()).getRegion();
            //String sourceCluster = from.getCurrentPoint().getRegion();
            String destinationCluster = "cluster35";

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

            shortestPath.forEach(path -> System.out.print(path + ", "));
            m.addToGoRegions(shortestPath);
            m.setTo(null);
            world.setWatchedMessageCreated(true);
            m.setWatched(true);
            m.setCreatedTime(SimClock.getTime());
            System.out.println("( message name: " + m.getId() + ", start time: " + SimClock.getTime() + " )");
        }

        m.setResponseSize(this.responseSize);
        from.createNewMessage(m);   // TODO:: MESSAGES ARE CREATED HERE!
        //System.out.println( from.toString() + " message location: " + from.getLocation());
    }

    // ezgi
 /*   @Override
    public void processEvent(World world) {
        DTNHost from = world.getNodeByAddress(this.fromAddr);
        if (from.toString().equals("c0")) {
            if (message == null) from.createNewMessage(buildNewMessage(world));
        }
    }*/

    private Message buildNewMessage(World world) {
        if (message == null) {
            DTNHost to = world.getNodeByAddress(this.toAddr);
            DTNHost from = world.getNodeByAddress(this.fromAddr);

            Message m = new Message(from, null, this.id, this.size);
            m.setResponseSize(this.responseSize);
            message = m;
        }
        return message;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + fromAddr + "->" + toAddr + "] " +
                "size:" + size + " CREATE";
    }
}
