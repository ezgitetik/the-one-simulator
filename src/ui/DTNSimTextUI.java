/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package ui;

import core.DTNHost;
import core.Settings;
import core.SimClock;
import custom.messagegenerator.RandomMessageGenerator;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.DoubleStream;

/**
 * Simple text-based user interface.
 */
public class DTNSimTextUI extends DTNSimUI {

	private static final Settings s = new Settings(); // don't use any namespace
	private static final String msgTtl = s.getSetting("Group.msgTtl");
	private static final String transmitSpeed = s.getSetting("btInterface.transmitSpeed");
	private static final String transmitRange = s.getSetting("btInterface.transmitRange");
	private static final String messageGenerationType = s.getSetting("MESSAGE_GENERATION_TYPE");
	private static final String geomobcon = s.getSetting("GEOMOBCON");


	private long lastUpdateRt;	// real time of last ui update
	private long startTime; // simulation start time
	/** How often the UI view is updated (milliseconds) */
	public static final long UI_UP_INTERVAL = 60000;

	Logger LOGGER_DETAIL = Logger.getLogger("file");;

	protected void runSim() {
		double simTime = SimClock.getTime();
		double endTime = scen.getEndTime();

		print("Running simulation '" + scen.getName()+"'");

		startTime = System.currentTimeMillis();
		lastUpdateRt = startTime;

		while (simTime < endTime && !simCancelled){
			try {
				world.update();
			} catch (AssertionError e) {
				e.printStackTrace();
				done();
				return;
			}
			simTime = SimClock.getTime();
			this.update(false);
		}

		double duration = (System.currentTimeMillis() - startTime)/1000.0;

		simDone = true;
		done();
		this.update(true); // force final UI update

		int totalDeliveredMessage= world.getHosts().stream().mapToInt(host -> host.getLoggedMessages().size()).sum();
		LOGGER_DETAIL.info("TOTAL_MESSAGE_COUNT: "+ RandomMessageGenerator.MESSAGE_COUNT);
		LOGGER_DETAIL.info("DELIVERED_MESSAGE_COUNT: "+totalDeliveredMessage);
		LOGGER_DETAIL.info("DELIVERY_RATIO: "+ String.format("%.2f", (((double)totalDeliveredMessage / (double)RandomMessageGenerator.MESSAGE_COUNT) * 100)).replace(".",","));

		double totalDelayTimeAsSeconds= world.getHosts().stream().mapToDouble(host -> host.getLoggedMessages().values().stream().mapToDouble(Double::doubleValue).sum()).sum();
		double meanDelayTimeAsSeconds = totalDelayTimeAsSeconds/totalDeliveredMessage;
		LOGGER_DETAIL.info("DELAY_TIME_MINUTES: "+ String.format("%.2f", (meanDelayTimeAsSeconds/60)).replace(".",","));
		LOGGER_DETAIL.info("TTL: "+ msgTtl);
		LOGGER_DETAIL.info("TRANSMIT_RANGE: "+ transmitRange);
		LOGGER_DETAIL.info("TRANSMIT_SPEED: "+ transmitSpeed);
		LOGGER_DETAIL.info("MESSAGE_GENERATION_TYPE: "+ messageGenerationType);

		int totalHopCount= world.getHosts().stream().mapToInt(host -> host.getMessagePathMap().values().stream().mapToInt(Integer::intValue).sum()).sum();
		LOGGER_DETAIL.info("TOTAL_HOP_COUNT: "+ totalHopCount);
		LOGGER_DETAIL.info("AVERAGE_HOP_COUNT: " + String.format("%.2f",((double)totalHopCount/(double)totalDeliveredMessage)).replace(".",","));


		uniqueDeliveredMessage();
		int totalDeliveredMainMessages = uniqueDeliveredMessages.size();
		int totalMainMessageCount = RandomMessageGenerator.MAIN_MESSAGE_COUNT;
		LOGGER_DETAIL.info("TOTAL_MAIN_MESSAGE_COUNT: "+ totalMainMessageCount);
		LOGGER_DETAIL.info("TOTAL_DELIVERED_MAIN_MESSAGE_COUNT: "+ totalDeliveredMainMessages);
		LOGGER_DETAIL.info("DELIVERY_RATIO: "+ String.format("%.2f", (((double)totalDeliveredMainMessages / (double)totalMainMessageCount) * 100)).replace(".",","));

		double totalMainMessageDelayTimeAsSeconds = uniqueDeliveredMessages.values().stream().mapToDouble(Double::doubleValue).sum();
		double meanMessageDelayTimeAsSeconds = totalMainMessageDelayTimeAsSeconds/totalDeliveredMainMessages;

		LOGGER_DETAIL.info("MAIN_MESSAGE_DELAY_TIME_MINUTES: "+ String.format("%.2f", (meanMessageDelayTimeAsSeconds/60)).replace(".",","));


		if (geomobcon.equals("true")){
			LOGGER_DETAIL.info("PREDICTION : GEOMOBCON");
		} else{
			LOGGER_DETAIL.info("PREDICTION : Sequence Prediction");
		}

		print("Simulation done in " + String.format("%.2f", duration) + "s");

	}

	private Map<String, Double> uniqueDeliveredMessages = new HashMap<>();

	private void uniqueDeliveredMessage(){
		for (DTNHost host : world.getHosts()) {
			host.getDeliveredMainMessages().forEach((mainMessageId, elapsedTime)->{
				if (uniqueDeliveredMessages.containsKey(mainMessageId)
						&& uniqueDeliveredMessages.get(mainMessageId) > elapsedTime) {
					uniqueDeliveredMessages.put(mainMessageId, elapsedTime);
				} else if (!uniqueDeliveredMessages.containsKey(mainMessageId)) {
					uniqueDeliveredMessages.put(mainMessageId, elapsedTime);
				}
			});
		}
	}

	/**
	 * Updates user interface if the long enough (real)time (update interval)
	 * has passed from the previous update.
	 * @param forced If true, the update is done even if the next update
	 * interval hasn't been reached.
	 */
	private void update(boolean forced) {
		long now = System.currentTimeMillis();
		long diff = now - this.lastUpdateRt;
		double dur = (now - startTime)/1000.0;
		if (forced || (diff > UI_UP_INTERVAL)) {
			// simulated seconds/second calc
			double ssps = ((SimClock.getTime() - lastUpdate)*1000) / diff;
			print(String.format("%.1f %d: %.2f 1/s", dur,
					SimClock.getIntTime(),ssps));

			this.lastUpdateRt = System.currentTimeMillis();
			this.lastUpdate = SimClock.getTime();
		}
	}

	private void print(String txt) {
		System.out.println(txt);
	}

}
