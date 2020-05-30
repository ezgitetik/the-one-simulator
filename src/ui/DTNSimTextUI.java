/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package ui;

import core.Settings;
import core.SimClock;
import custom.messagegenerator.RandomMessageGenerator;
import org.apache.log4j.Logger;

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
		LOGGER_DETAIL.info("DELIVERY_RATIO: "+ String.format("%.2f", (((double)totalDeliveredMessage / (double)RandomMessageGenerator.MESSAGE_COUNT) * 100))+ "%");

		double totalDelayTimeAsSeconds= world.getHosts().stream().mapToDouble(host -> host.getLoggedMessages().values().stream().mapToDouble(Double::doubleValue).sum()).sum();
		double meanDelayTimeAsSeconds = totalDelayTimeAsSeconds/totalDeliveredMessage;
		LOGGER_DETAIL.info("DELAY_TIME_MINUTES: "+ String.format("%.2f", (meanDelayTimeAsSeconds/60)));
		LOGGER_DETAIL.info("TTL: "+ msgTtl);
		LOGGER_DETAIL.info("TRANSMIT_RANGE: "+ transmitRange);
		LOGGER_DETAIL.info("TRANSMIT_SPEED: "+ transmitSpeed);
		LOGGER_DETAIL.info("MESSAGE_GENERATION_TYPE: "+ messageGenerationType);

		if (geomobcon.equals("true")){
			LOGGER_DETAIL.info("PREDICTION : GEOMOBCON");
		} else{
			LOGGER_DETAIL.info("PREDICTION : Sequence Prediction");
		}

		print("Simulation done in " + String.format("%.2f", duration) + "s");

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
