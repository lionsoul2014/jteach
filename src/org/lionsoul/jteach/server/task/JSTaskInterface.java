package org.lionsoul.jteach.server.task;

import org.lionsoul.jteach.server.JBean;

/**
 * Task Interface for JTeach Server
 * @author chenxin - chenxin619315@gmail.com
 */
public interface JSTaskInterface {
	
	public static final int T_RUN = 1;
	public static final int T_STOP = 0;
	
	/** start the Task */
	public boolean start();

	/* add a new client */
	public void addClient(JBean bean);

	/** stop the working Task */
	public void stop();
}
