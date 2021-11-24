package com.webssky.jteach.server.task;

/**
 * Task Interface for JTeach Server
 * @author chenxin - chenxin619315@gmail.com
 */
public interface JSTaskInterface {
	
	public static final int T_RUN = 1;
	public static final int T_STOP = 0;
	
	/**
	 * start the working Task 
	 */
	public void startTask();
	
	/**
	 * stop the working Task 
	 */
	public void stopTask();
}
