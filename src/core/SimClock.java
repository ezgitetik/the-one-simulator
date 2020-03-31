/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import java.util.concurrent.TimeUnit;

/**
 * Wall clock for checking the simulation time.
 */
public class SimClock {
	private static double clockTime = 0.0;
	private static SimClock clock = null;

	private SimClock() {}

	static {
		DTNSim.registerForReset(SimClock.class.getCanonicalName());
		reset();
	}

	/**
	 * Get the instance of the class that can also change the time.
	 * @return The instance of this clock
	 */
	public static SimClock getInstance() {
		if (clock == null) {
			clock = new SimClock();
		}
		return clock;
	}

	/**
	 * Returns the current time (seconds since start)
	 * @return Time as a double
	 */
	public static double getTime() {
		return clockTime;
	}

	public static String getTimeString(){
		return String.format("%02d:%02d:%02d", TimeUnit.SECONDS.toHours((long)clockTime),
				TimeUnit.SECONDS.toMinutes((long)clockTime) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours((long)clockTime)),
				TimeUnit.SECONDS.toSeconds((long)clockTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes((long)clockTime)));
	}

	/**
	 * Returns the current time rounded to the nearest integer
	 * @return Time as integer
	 */
	public static int getIntTime() {
		return (int)Math.round(clockTime);
	}

	/**
	 * Returns a string presentation of the sim time shown with the given amount
	 * of decimals
	 * @param decimals The number of decimals to show
	 * @return The sim time
	 */
	public static String getFormattedTime(int decimals) {
		return String.format("%." + decimals + "f", clockTime);
	}

	/**
	 * Advances the time by n seconds
	 * @param time Nrof seconds to increase the time
	 */
	public void advance(double time) {
		clockTime += time;
	}

	/**
	 * Sets the time of the clock.
	 * @param time the time to set
	 */
	public void setTime(double time) {
		clockTime = time;
	}

	/**
	 * Returns the current simulation time in a string
	 * @return the current simulation time in a string
	 */
	public String toString() {
		return "SimTime: " + clockTime;
	}

	/**
	 * Resets the static fields of the class
	 */
	public static void reset() {
		clockTime = 0;
	}

	public static void main(String[] args) {

	}
}
