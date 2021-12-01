package org.lionsoul.jteach.client.task;

import org.lionsoul.jteach.client.JClient;
import org.lionsoul.jteach.msg.JBean;

/**
 * Task Interface for JTeach Client
 * @author chenxin
 */
public abstract class JCTaskBase implements Runnable {
	public static final int T_RUN = 1;
	public static final int T_STOP = 0;

	/** task running status and default it to T_RUN */
	protected volatile int status = T_RUN;
	protected final JClient client;
	protected final JBean bean;

	protected JCTaskBase(JClient client) {
		this.client = client;
		this.bean = client.getBean();
	}

	/** start the working Task */
	public abstract void start(String...args);
	
	/** stop the working Task */
	public void stop() {
		setStatus(T_STOP);
	}

	public void setStatus(int s) {
		status = s;
	}

	public int getStatus() {
		return status;
	}

}
