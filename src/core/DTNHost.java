/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//import com.sun.tools.javac.util.StringUtils;
import custom.ArffReader;
import custom.ArffRegion;
import custom.CustomerProbabilityDistribution;
import custom.InfoMessage;
import custom.messagegenerator.RandomMessageGenerator;
import movement.MovementModel;
import movement.Path;
import org.apache.log4j.Logger;
import routing.MessageRouter;
import routing.util.RoutingInfo;

import static core.Constants.DEBUG;

/**
 * A DTN capable host.
 */
public class DTNHost implements Comparable<DTNHost> {
    private static int nextAddress = 0;
    private int address;

    private Coord location;    // where is the host
    private Coord destination;    // where is it going
    private Coord previousDestination=null;    // where is it going

    private MessageRouter router;
    private MovementModel movement;
    private Path path;
    private double speed;
    private double nextTimeToMove=200;
    private String name;
    private List<MessageListener> msgListeners;
    private List<MovementListener> movListeners;
    private List<NetworkInterface> net;
    private ModuleCommunicationBus comBus;

    private Map<String,Double> loggedMessages = new HashMap<>();
    private String currentCluster;
    private List<ArffRegion> allRegions;
    private ArffRegion currentPoint;
    private int currentPointIndex = 0;
    private boolean isTaxiOnReturnPath = false;
    private int futureRegionIndex;
    private boolean isTaxiStillOnStartPoint;
    private boolean isTaxiStillOnEndPoint;
    private boolean hasTaxiCustomer = true;
    private int destinationPointIndex=0;
    private int previousDestinationPointIndex=0;
    private Map<String, Double> contactHistoryMap = new HashMap<>();
    //TODO it should be changed when cluster count has changed.
    private static final Settings s = new Settings(); // don't use any namespace
    private static final int CLUSTER_COUNT = Integer.parseInt(s.getSetting("CLUSTER_COUNT"));

    private  int oldPointIndex=-1;

    private static final Logger LOGGER=Logger.getLogger("file");

    private List<String> passedRegions = new ArrayList<>();

    public ArffRegion getCurrentPoint() {
        return currentPoint;
    }

    public boolean isHasTaxiCustomer() {
        return hasTaxiCustomer;
    }

    public List<ArffRegion> getAllRegions() {
        return allRegions;
    }

    public int getCurrentPointIndex() {
        return currentPointIndex;
    }

    public Map<String,Double> getLoggedMessages() {
        return loggedMessages;
    }

    static {
        DTNSim.registerForReset(DTNHost.class.getCanonicalName());
        reset();
    }

    public String getName() {
        return name;
    }

    public String getCurrentCluster() {
        return currentCluster;
    }

    public void setCurrentCluster(String currentCluster) {
        this.currentCluster = currentCluster;
    }

    public Map<String, Double> getContactHistoryMap() {
        return contactHistoryMap;
    }

    public List<ArffRegion> getFutureRegions() {
        int cursor;
        if (this.isTaxiOnReturnPath) {
            cursor = Math.max(this.futureRegionIndex, 0);
            return this.allRegions.subList(cursor, this.currentPointIndex);
        } else {
            cursor = Math.min(this.futureRegionIndex, this.allRegions.size() - 1);
            return this.allRegions.subList(this.currentPointIndex, cursor);
        }
    }

    /**
     * Creates a new DTNHost.
     *
     * @param msgLs        Message listeners
     * @param movLs        Movement listeners
     * @param groupId      GroupID of this host
     * @param interf       List of NetworkInterfaces for the class
     * @param comBus       Module communication bus object
     * @param mmProto      Prototype of the movement model of this host
     * @param mRouterProto Prototype of the message router of this host
     */
    public DTNHost(List<MessageListener> msgLs,
                   List<MovementListener> movLs,
                   String groupId,
                   List<NetworkInterface> interf,
                   ModuleCommunicationBus comBus,
                   MovementModel mmProto,
                   MessageRouter mRouterProto,
                   String hostName) {
        this.comBus = comBus;
        this.location = new Coord(0, 0);
        this.address = getNextAddress();

        this.name = hostName;

        this.net = new ArrayList<NetworkInterface>();

        for (NetworkInterface i : interf) {
            NetworkInterface ni = i.replicate();
            if (ni != null) {
                ni.setHost(this);
                net.add(ni);
            }
        }

        // TODO - think about the names of the interfaces and the nodes
        //this.name = groupId + ((NetworkInterface)net.get(1)).getAddress();

        this.msgListeners = msgLs;
        this.movListeners = movLs;

        // create instances by replicating the prototypes
        this.movement = mmProto.replicate();
        if (this.movement != null) {
            this.movement.setComBus(comBus);
            this.movement.setHost(this);
        }

        setRouter(mRouterProto.replicate());
        // (36345.90,16272.90)

        if (movement != null) {
            this.location = movement.getInitialLocation();

            this.nextTimeToMove = movement.nextPathAvailable();
        }

        this.path = null;

        if (movLs != null) { // inform movement listeners about the location
            for (MovementListener l : movLs) {
                l.initialLocation(this, this.location);
            }
        }

        if (this.name.startsWith("taxi-")) {
            try {
                this.allRegions = ArffReader.getArffRegionListByFileName(this.name + "-second.wkt");
                IntStream.range(0, CLUSTER_COUNT).forEach(cluster -> this.contactHistoryMap.put("cluster" + cluster, 0D));
                //  this.nextTimeToMove = 0;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a new network interface address and increments the address for
     * subsequent calls.
     *
     * @return The next address.
     */
    private synchronized static int getNextAddress() {
        return nextAddress++;
    }

    /**
     * Reset the host and its interfaces
     */
    public static void reset() {
        nextAddress = 0;
    }

    /**
     * Returns true if this node is actively moving (false if not)
     *
     * @return true if this node is actively moving (false if not)
     */
    public boolean isMovementActive() {
        return this.movement.isActive();
    }

    /**
     * Returns true if this node's radio is active (false if not)
     *
     * @return true if this node's radio is active (false if not)
     */
    public boolean isRadioActive() {
        // Radio is active if any of the network interfaces are active.
        for (final NetworkInterface i : this.net) {
            if (i.isActive()) return true;
        }
        return false;
    }

    /**
     * Set a router for this host
     *
     * @param router The router to set
     */
    private void setRouter(MessageRouter router) {
        if (router != null) {
            router.init(this, msgListeners);
            this.router = router;
        }
    }

    /**
     * Returns the router of this host
     *
     * @return the router of this host
     */
    public MessageRouter getRouter() {
        return this.router;
    }

    /**
     * Returns the network-layer address of this host.
     */
    public int getAddress() {
        return this.address;
    }

    /**
     * Returns this hosts's ModuleCommunicationBus
     *
     * @return this hosts's ModuleCommunicationBus
     */
    public ModuleCommunicationBus getComBus() {
        return this.comBus;
    }

    /**
     * Informs the router of this host about state change in a connection
     * object.
     *
     * @param con The connection object whose state changed
     */
    public void connectionUp(Connection con) {
        this.router.changedConnection(con);
    }

    public void connectionDown(Connection con) {
        this.router.changedConnection(con);
    }

    /**
     * Returns a copy of the list of connections this host has with other hosts
     *
     * @return a copy of the list of connections this host has with other hosts
     */
    public List<Connection> getConnections() {
        List<Connection> lc = new ArrayList<Connection>();

        for (NetworkInterface i : net) {
            lc.addAll(i.getConnections());
        }

        return lc;
    }

    /**
     * Returns the current location of this host.
     *
     * @return The location
     */
    public Coord getLocation() {
        return this.location;
    }

    /**
     * Returns the Path this node is currently traveling or null if no
     * path is in use at the moment.
     *
     * @return The path this node is traveling
     */
    public Path getPath() {
        return this.path;
    }


    /**
     * Sets the Node's location overriding any location set by movement model
     *
     * @param location The location to set
     */
    public void setLocation(Coord location) {
        this.location = location.clone();
    }

    /**
     * Sets the Node's name overriding the default name (groupId + netAddress)
     *
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the messages in a collection.
     *
     * @return Messages in a collection
     */
    public Collection<Message> getMessageCollection() {
        return this.router.getMessageCollection();
    }

    /**
     * Returns the number of messages this node is carrying.
     *
     * @return How many messages the node is carrying currently.
     */
    public int getNrofMessages() {
        return this.router.getNrofMessages();
    }

    /**
     * Returns the buffer occupancy percentage. Occupancy is 0 for empty
     * buffer but can be over 100 if a created message is bigger than buffer
     * space that could be freed.
     *
     * @return Buffer occupancy percentage
     */
    public double getBufferOccupancy() {
        long bSize = router.getBufferSize();
        long freeBuffer = router.getFreeBufferSize();
        return 100 * ((bSize - freeBuffer) / (bSize * 1.0));
    }

    /**
     * Returns routing info of this host's router.
     *
     * @return The routing info.
     */
    public RoutingInfo getRoutingInfo() {
        return this.router.getRoutingInfo();
    }

    /**
     * Returns the interface objects of the node
     */
    public List<NetworkInterface> getInterfaces() {
        return net;
    }

    /**
     * Find the network interface based on the index
     */
    public NetworkInterface getInterface(int interfaceNo) {
        NetworkInterface ni = null;
        try {
            ni = net.get(interfaceNo - 1);
        } catch (IndexOutOfBoundsException ex) {
            throw new SimError("No such interface: " + interfaceNo +
                    " at " + this);
        }
        return ni;
    }

    /**
     * Find the network interface based on the interfacetype
     */
    protected NetworkInterface getInterface(String interfacetype) {
        for (NetworkInterface ni : net) {
            if (ni.getInterfaceType().equals(interfacetype)) {
                return ni;
            }
        }
        return null;
    }

    /**
     * Force a connection event
     */
    public void forceConnection(DTNHost anotherHost, String interfaceId,
                                boolean up) {
        NetworkInterface ni;
        NetworkInterface no;

        if (interfaceId != null) {
            ni = getInterface(interfaceId);
            no = anotherHost.getInterface(interfaceId);

            assert (ni != null) : "Tried to use a nonexisting interfacetype " + interfaceId;
            assert (no != null) : "Tried to use a nonexisting interfacetype " + interfaceId;
        } else {
            ni = getInterface(1);
            no = anotherHost.getInterface(1);

            assert (ni.getInterfaceType().equals(no.getInterfaceType())) :
                    "Interface types do not match.  Please specify interface type explicitly";
        }

        if (up) {
            ni.createConnection(no);
        } else {
            ni.destroyConnection(no);
        }
    }

    /**
     * for tests only --- do not use!!!
     */
    public void connect(DTNHost h) {
        if (DEBUG) Debug.p("WARNING: using deprecated DTNHost.connect" +
                "(DTNHost) Use DTNHost.forceConnection(DTNHost,null,true) instead");
        forceConnection(h, null, true);
    }

    /**
     * Updates node's network layer and router.
     *
     * @param simulateConnections Should network layer be updated too
     */
    public void update(boolean simulateConnections) {
        if (!isRadioActive()) {
            // Make sure inactive nodes don't have connections
            tearDownAllConnections();
            return;
        }

        if (simulateConnections) {
            for (NetworkInterface i : net) {
                i.update();
            }
        }
        this.router.update();
    }

    /**
     * Tears down all connections for this host.
     */
    private void tearDownAllConnections() {
        for (NetworkInterface i : net) {
            // Get all connections for the interface
            List<Connection> conns = i.getConnections();
            if (conns.size() == 0) continue;

            // Destroy all connections
            List<NetworkInterface> removeList =
                    new ArrayList<NetworkInterface>(conns.size());
            for (Connection con : conns) {
                removeList.add(con.getOtherInterface(i));
            }
            for (NetworkInterface inf : removeList) {
                i.destroyConnection(inf);
            }
        }
    }

    /**
     * Moves the node towards the next waypoint or waits if it is
     * not time to move yet
     *
     * @param timeIncrement How long time the node moves
     */
    public void move(double timeIncrement) {
        double possibleMovement;


        double distance;
        double dx, dy;

        Logger LOGGER_ADMIN = Logger.getLogger("admin");;
        Logger LOGGER_STDOUT = Logger.getLogger("stdout");



        if (!isMovementActive() || SimClock.getTime() < this.nextTimeToMove) {
            return;
        }

        if(this.getName().equalsIgnoreCase("taxi-528")){
            System.out.print("");
        }

        if (this.destination == null) {
            if (!setNextWaypoint()) {
                return;
            }
        }

        possibleMovement = timeIncrement * speed;
        distance = this.location.distance(this.destination);

        while (possibleMovement >= distance) {
            // node can move past its next destination
            this.location.setLocation(this.destination); // snap to destination
            possibleMovement -= distance;
            if (!setNextWaypoint()) { // get a new waypoint
                this.destination = null; // No more waypoints left, therefore the destination must be null
                return; // no more waypoints left
            }
            distance = this.location.distance(this.destination);
        }

        // move towards the point for possibleMovement amount
        dx = (possibleMovement / distance) * (this.destination.getX() -
                this.location.getX());
        dy = (possibleMovement / distance) * (this.destination.getY() -
                this.location.getY());
        this.location.translate(dx, dy);

        this.currentPoint = this.getCurrentPointFromAllRegions();
        this.currentCluster=this.currentPoint.getRegion();

        if(this.getName().equalsIgnoreCase("taxi-815")){
            if(this.passedRegions.size() == 0){
                this.passedRegions.add(this.currentCluster);
                //System.out.println(this.currentCluster);
            } else {
                if(!this.passedRegions.get(this.passedRegions.size()-1).equalsIgnoreCase(this.currentCluster)){
                    this.passedRegions.add(this.currentCluster);
                    //System.out.println(this.currentCluster);
                }
            }

            System.out.print("previous destination wkt point: "+(this.previousDestination!=null? this.previousDestination.getxRoute():"null")+" "+(this.previousDestination!=null?this.previousDestination.getyRoute():"null"));
            System.out.print(" next destination wkt point: "+this.destination.getxRoute()+" "+this.destination.getyRoute());
            System.out.print(" current point: "+this.location.getxRoute()+" "+this.location.getyRoute());
            System.out.print(" wkt point: "+this.currentPoint.getxPoint()+" "+this.currentPoint.getyPoint());
            System.out.print(" current cluster: "+this.currentCluster);
            System.out.println(" current pointIndex: "+this.currentPointIndex);

        }

        if (isTaxiOnReturnPath) {
            if ((this.currentPointIndex == this.allRegions.size() - 1 && !this.isTaxiStillOnEndPoint)
                    || this.currentPointIndex <= this.futureRegionIndex) {
                int count = getFutureRegionCount();
                this.futureRegionIndex = this.currentPointIndex - count;
                setTaxiStillOnEndPoint();

            }

        } else {
            if ((this.currentPointIndex == 0 && !this.isTaxiStillOnStartPoint)
                    || this.futureRegionIndex == 0
                    || this.currentPointIndex >= this.futureRegionIndex) {
                int count = getFutureRegionCount();
                this.futureRegionIndex = count + this.currentPointIndex;
                setTaxiStillOnStartPoint();
            }
        }



        if (!this.currentPoint.getRegion().equalsIgnoreCase(this.currentCluster)) {
            likelihoodConUpdate();
        }
        if (this.getMessageCollection().stream().map(Message::isWatched).collect(Collectors.toList()).contains(true)) {
            String cluster = this.currentPoint.getRegion();
            List<Message> watchedMessages = this.getMessageCollection().stream().filter(Message::isWatched).collect(Collectors.toList());
            watchedMessages.forEach(watchedMessage -> {
                List<String> toGoRegions = watchedMessage.getToGoRegions();
                if (toGoRegions.get(toGoRegions.size() - 1).equalsIgnoreCase(cluster)) {
                    watchedMessage.setDeliveredTime(SimClock.getTime());

                    watchedMessage.setTtl(1);
                    if(!loggedMessages.keySet().contains(watchedMessage.getId())){
                        LOGGER_ADMIN.info(watchedMessage.getId() + "," + watchedMessage.getElapsedTimeAsMinutesString());

                        LOGGER.info(SimClock.getTimeString()+" "
                                + InfoMessage.MESSAGE_ARRIVED
                                + "', messageId: '" + watchedMessage.getId()
                                + "', toGoRegions: '" + watchedMessage.getToGoRegions().stream().collect(Collectors.joining(","))
                                + "', totalTime: "+ watchedMessage.getElapsedTimeAsMinutesString() + " minutes.");

                        LOGGER.info(SimClock.getTimeString()+" "
                                + "MESSAGE_ARRIVED_BY_TAXI"
                                + "', messageId: '" + watchedMessage.getId()
                                + "', taxiIds: '" + watchedMessage.getHops().stream().map(DTNHost::getName).collect(Collectors.joining(","))
                                + "', totalTime: "+ watchedMessage.getElapsedTimeAsMinutesString() + " minutes.");

                    }
                    loggedMessages.put(watchedMessage.getId(),watchedMessage.getElapsedTimeAsMinutes());
                }
            });

        }
    }

    public void move() {

        Logger LOGGER_ADMIN = Logger.getLogger("admin");;



        if (!isMovementActive()) {
            return;
        }

        if (this.oldPointIndex == -1){
            this.currentPoint = this.allRegions.get(this.currentPointIndex);
            likelihoodConUpdate();
        }

        this.oldPointIndex = this.currentPointIndex;
        int pointIndex = this.currentPointIndex;
        for (int i = this.currentPointIndex; i < this.allRegions.size(); i++) {
            double clock=SimClock.getTime();
            double taxiClock=this.allRegions.get(i).getTimeInSecond();

            if (clock >= taxiClock) {
                pointIndex = i;
            } else {
                break;
            }
        }
        this.currentPointIndex = pointIndex;

        Coord coord=new Coord();
        coord.setxRoute(this.allRegions.get(this.currentPointIndex).getxPoint());
        coord.setyRoute(this.allRegions.get(this.currentPointIndex).getyPoint());
        coord.setX(coord.getxRoute()-Coord.xOffset);
        coord.setY(Coord.yOffset-coord.getyRoute());
        this.destination = coord;
        this.location=this.destination;
        this.currentPoint = this.allRegions.get(this.currentPointIndex);
        this.currentCluster = this.currentPoint.getRegion();

        if (!this.allRegions.get(oldPointIndex).getRegion().equalsIgnoreCase(this.currentCluster)) {
            likelihoodConUpdate();
        }

        //System.out.println("second:"+SimClock.getTime()+", taxi time:"+this.allRegions.get(this.currentPointIndex).getTimeInSecond()+", taxi:"+this.name+", point:"+coord.getxRoute()+" "+coord.getyRoute());

        if (isTaxiOnReturnPath) {
            if ((this.currentPointIndex == this.allRegions.size() - 1 && !this.isTaxiStillOnEndPoint)
                    || this.currentPointIndex <= this.futureRegionIndex) {
                int count = getFutureRegionCount();
                this.futureRegionIndex = this.currentPointIndex - count;
                setTaxiStillOnEndPoint();
            }

        } else {
            if ((this.currentPointIndex == 0 && !this.isTaxiStillOnStartPoint)
                    || this.futureRegionIndex == 0
                    || this.currentPointIndex >= this.futureRegionIndex) {
                int count = getFutureRegionCount();
                this.futureRegionIndex = count + this.currentPointIndex;
                setTaxiStillOnStartPoint();
            }
        }

        Message randomMessage = RandomMessageGenerator.generateMessage(this);
        if (randomMessage != null) {
            this.createNewMessage(randomMessage);
        }

        if (this.getMessageCollection().stream().map(Message::isWatched).collect(Collectors.toList()).contains(true)) {
            String cluster = this.currentPoint.getRegion();
            List<Message> watchedMessages = this.getMessageCollection().stream().filter(Message::isWatched).collect(Collectors.toList());
            watchedMessages.forEach(watchedMessage -> {
                List<String> toGoRegions = watchedMessage.getToGoRegions();
                if (toGoRegions.get(toGoRegions.size() - 1).equalsIgnoreCase(cluster)) {
                    watchedMessage.setDeliveredTime(SimClock.getTime());
                    watchedMessage.setTtl(1);
                    if(!loggedMessages.keySet().contains(watchedMessage.getId())){
                        LOGGER_ADMIN.info(watchedMessage.getId() + "," + watchedMessage.getElapsedTimeAsMinutesString());
                        LOGGER.info(SimClock.getTimeString()+" "
                                + InfoMessage.MESSAGE_ARRIVED
                                + "', messageId: '" + watchedMessage.getId()
                                + "', toGoRegions: '" + watchedMessage.getToGoRegions().stream().collect(Collectors.joining(","))
                                + "', totalTime: "+ watchedMessage.getElapsedTimeAsMinutesString() + " minutes.");
                        loggedMessages.put(watchedMessage.getId(),watchedMessage.getElapsedTimeAsMinutes());
                    }

                }
            });

        }
    }

    private boolean moveToNextPoint() {
        boolean moveToNextPoint = false;
        int pointIndex = this.currentPointIndex;
        for (int i = this.currentPointIndex; i < this.allRegions.size(); i++) {
            if (SimClock.getTime() >= this.allRegions.get(i).getTimeInSecond()) {
                pointIndex = i;
            } else {
                break;
            }
        }
        /*if (pointIndex != this.currentPointIndex) {
            this.currentPointIndex = pointIndex;
            moveToNextPoint = true;
        }
        return moveToNextPoint;*/

        this.currentPointIndex = pointIndex;
        return true;
    }

    private void likelihoodConUpdate() {
        this.contactHistoryMap.entrySet().forEach(contactHistory -> {
            if (contactHistory.getKey().equalsIgnoreCase(this.currentPoint.getRegion())) {
                contactHistory.setValue(contactHistory.getValue() + (1 - contactHistory.getValue()) * 0.75);
            } else {
                double elapsedMinutes = SimClock.getTime() / 60;
                //System.out.println("elapsedMinutes: " + elapsedMinutes);
                contactHistory.setValue(contactHistory.getValue() * Math.pow(0.98, elapsedMinutes));
            }
        });
        /*LOGGER.info(SimClock.getTimeString()+" "
                + InfoMessage.LIKELIHOOD_CONTACT_HISTORY_UPDATE
                + ", taxiName: '" + this.getName());*/
    }

    private int getFutureRegionCount() {
        if (hasTaxiCustomer) {
            //System.out.println("customer:true");
            hasTaxiCustomer = false;
            return CustomerProbabilityDistribution.getFutureRegionCountForCustomer();
        } else {
            //System.out.println("customer:false");
            hasTaxiCustomer = true;
            return CustomerProbabilityDistribution.getFutureRegionCountForWithoutCustomer();
        }
    }

    private void setTaxiStillOnStartPoint() {
        this.isTaxiStillOnStartPoint = this.currentPointIndex == 0;
    }

    private void setTaxiStillOnEndPoint() {
        this.isTaxiStillOnEndPoint = this.currentPointIndex == this.allRegions.size() - 1;
    }

    private ArffRegion getCurrentPointFromAllRegions() {
        Map<Double, ArffRegionIndex> hypotenuseDistances = new HashMap<>();

        if (this.currentPoint == null) {
            this.currentPointIndex = 0;
        }

        setTaxisDirection();
        // TODO: cursor + 1
        //int cursor = getCursor();
        double hypo;

        if (this.destination==null){
            this.destinationPointIndex = 0;
        }else{
            this.destinationPointIndex = this.getIndexFromAllArffPoints(this.destination.getxRoute(),this.destination.getyRoute(),this.destinationPointIndex);
        }

        if (this.previousDestination==null){
            this.previousDestinationPointIndex=0;
        }else{
            this.previousDestinationPointIndex=this.getIndexFromAllArffPoints(this.previousDestination.getxRoute(),this.previousDestination.getyRoute(),this.previousDestinationPointIndex);
        }

        System.out.println("previous Index:"+this.previousDestinationPointIndex
                +", nextIndex: "+this.destinationPointIndex
                +", previous x:"+this.previousDestination.getxRoute()+" "+ this.previousDestination.getyRoute()
                +", next x:"+this.destination.getxRoute()+" "+ this.destination.getyRoute()+", taxi:"+this.name);

        for (int i = this.previousDestinationPointIndex; i <= this.destinationPointIndex; i++) {
            hypo =  Point2D.distance(this.location.getxRoute(), this.location.getyRoute(),
                    this.allRegions.get(i).getxPoint(), this.allRegions.get(i).getyPoint());
            /*hypo = Math.hypot(this.location.getxRoute() - this.allRegions.get(i).getxPoint()
                    , this.location.getyRoute() - this.allRegions.get(i).getyPoint());*/
            hypotenuseDistances.put(hypo, new ArffRegionIndex(i, this.allRegions.get(i)));
        }

        OptionalDouble key = hypotenuseDistances.keySet().stream().mapToDouble(v -> v).min();
        this.currentPointIndex = hypotenuseDistances.get(key.getAsDouble()).getIndex();
        if(this.getName().equalsIgnoreCase("taxi-528")){
            //System.out.println("Current point index: "+ this.currentPointIndex);
        }
        return hypotenuseDistances.get(key.getAsDouble()).getArffRegion();
    }

    private void setTaxisDirection() {
        if (this.currentPointIndex == this.allRegions.size() - 1) {
            if (!this.isTaxiOnReturnPath) {
                System.out.println("it is on return path, going on end to start...");
            }
            this.isTaxiOnReturnPath = true;
        } else if (this.currentPointIndex == 0) {
            if (this.isTaxiOnReturnPath) {
                //System.out.println("going on start to end...");
            }
            this.isTaxiOnReturnPath = false;
        }
    }

    public boolean isTaxiOnReturnPath(){
        return this.isTaxiOnReturnPath;
    }

    private int getCursor() {
        int cursor;
        if (this.isTaxiOnReturnPath) {
            cursor = this.currentPointIndex - 5;
            if(cursor<0) cursor=0;
        } else {
            cursor = this.currentPointIndex;
        }
        return cursor;
    }

    private int getIndexFromAllArffPoints(double xRoute, double yRoute, int startIndex){
        /*AtomicInteger pointIndex = new AtomicInteger();
         IntStream.range(0,this.allRegions.size()).forEach(index -> {
            if(this.allRegions.get(index).getxPoint().equals(xRoute)
                    && this.allRegions.get(index).getyPoint().equals(yRoute)){
                pointIndex.set(index);
                return;
            }
        });
         return pointIndex.get();*/

        DecimalFormat df = new DecimalFormat("#.###");
        int pointIndex = 0;
        if (this.isTaxiOnReturnPath){
            for (int i = startIndex; i >= 0; i--) {
                if (df.format(this.allRegions.get(i).getxPoint()).equals(df.format(xRoute))
                        && df.format(this.allRegions.get(i).getyPoint()).equals(df.format(yRoute))) {
                    pointIndex = i;
                    break;
                }
            }
        }
        else{
            for (int i = startIndex; i < this.allRegions.size(); i++) {
                if (df.format(this.allRegions.get(i).getxPoint()).equals(df.format(xRoute))
                        && df.format(this.allRegions.get(i).getyPoint()).equals(df.format(yRoute))) {
                    pointIndex = i;
                    break;
                }
            }
        }


       return pointIndex;
    }

    /**
     * Sets the next destination and speed to correspond the next waypoint
     * on the path.
     *
     * @return True if there was a next waypoint to set, false if node still
     * should wait
     */
    private boolean setNextWaypoint() {
        if (path == null) {
            path = movement.getPath();
        }

        if (path == null || !path.hasNext()) {
            this.nextTimeToMove = movement.nextPathAvailable();
            this.path = null;
            return false;
        }


        Coord currentCoord=path.getNextWaypoint();
        if (currentCoord != previousDestination){
            this.previousDestination = this.destination;
        }
        this.destination=currentCoord;
        //this.destination = path.getNextWaypoint();
        this.speed = path.getSpeed();

        if (this.movListeners != null) {
            for (MovementListener l : this.movListeners) {
                l.newDestination(this, this.destination, this.speed);
            }
        }

        return true;
    }

    /**
     * Sends a message from this host to another host
     *
     * @param id Identifier of the message
     * @param to Host the message should be sent to
     */
    public void sendMessage(String id, DTNHost to) {
        this.router.sendMessage(id, to);
    }

    /**
     * Start receiving a message from another host
     *
     * @param m    The message
     * @param from Who the message is from
     * @return The value returned by
     * {@link MessageRouter#receiveMessage(Message, DTNHost)}
     */
    public int receiveMessage(Message m, DTNHost from) {
        int retVal = this.router.receiveMessage(m, from);

        if (retVal == MessageRouter.RCV_OK) {
            m.addNodeOnPath(this);    // add this node on the messages path
        }

        return retVal;
    }

    /**
     * Requests for deliverable message from this host to be sent trough a
     * connection.
     *
     * @param con The connection to send the messages trough
     * @return True if this host started a transfer, false if not
     */
    public boolean requestDeliverableMessages(Connection con) {
        return this.router.requestDeliverableMessages(con);
    }

    /**
     * Informs the host that a message was successfully transferred.
     *
     * @param id   Identifier of the message
     * @param from From who the message was from
     */
    public void messageTransferred(String id, DTNHost from) {
        this.router.messageTransferred(id, from);
    }

    /**
     * Informs the host that a message transfer was aborted.
     *
     * @param id             Identifier of the message
     * @param from           From who the message was from
     * @param bytesRemaining Nrof bytes that were left before the transfer
     *                       would have been ready; or -1 if the number of bytes is not known
     */
    public void messageAborted(String id, DTNHost from, int bytesRemaining) {
        this.router.messageAborted(id, from, bytesRemaining);
    }

    /**
     * Creates a new message to this host's router
     *
     * @param m The message to create
     */
    public void createNewMessage(Message m) {
        this.router.createNewMessage(m);
    }

    /**
     * Deletes a message from this host
     *
     * @param id   Identifier of the message
     * @param drop True if the message is deleted because of "dropping"
     *             (e.g. buffer is full) or false if it was deleted for some other reason
     *             (e.g. the message got delivered to final destination). This effects the
     *             way the removing is reported to the message listeners.
     */
    public void deleteMessage(String id, boolean drop) {
        this.router.deleteMessage(id, drop);
    }

    /**
     * Returns a string presentation of the host.
     *
     * @return Host's name
     */
    public String toString() {
        return name;
    }

    /**
     * Checks if a host is the same as this host by comparing the object
     * reference
     *
     * @param otherHost The other host
     * @return True if the hosts objects are the same object
     */
    public boolean equals(DTNHost otherHost) {
        return this == otherHost;
    }

    /**
     * Compares two DTNHosts by their addresses.
     *
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(DTNHost h) {
        return this.getAddress() - h.getAddress();
    }

}

class ArffRegionIndex {
    private int index;
    private ArffRegion arffRegion;

    public ArffRegionIndex(int index, ArffRegion arffRegion) {
        this.index = index;
        this.arffRegion = arffRegion;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ArffRegion getArffRegion() {
        return arffRegion;
    }

    public void setArffRegion(ArffRegion arffRegion) {
        this.arffRegion = arffRegion;
    }
}
