package com.l2jserver.util;

import java.util.logging.LogManager;

/**
 * Specialized {@link LogManager} class.<br>
 * Prevents log devices to close before shutdown sequence so the shutdown sequence can make logging.
 *
 */
public class L2LogManager extends LogManager {
	
	public L2LogManager() {
		super();
	}
	
	@Override
	public void reset() {
		// do nothing
	}
	
	public void doReset() {
		super.reset();
	}
}