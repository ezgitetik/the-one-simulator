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
import custom.RandomMessageGenerator;

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
        m.setResponseSize(this.responseSize);

        DTNHost randomCarrierHost = RandomMessageGenerator.pickRandomCarrierHost(world);
        if (RandomMessageGenerator.MESSAGE_COUNT <= 20
                && SimClock.getTime() > RandomMessageGenerator.LAST_MESSAGE_CREATE_TIME + RandomMessageGenerator.MESSAGE_CREATE_DELAY
                ) {
            m = RandomMessageGenerator.generateMessage(from);
        }
        from.createNewMessage(m);
    }

    private List<String> getMessageShortestPath(String sourceCluster, String destinationCluster) {
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
