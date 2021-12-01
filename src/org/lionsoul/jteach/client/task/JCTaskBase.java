package org.lionsoul.jteach.client.task;

/**
 * Task Interface for JTeach Client
 * @author chenxin
 */
public abstract class JCTaskBase implements Runnable {
	public static final int T_RUN = 1;
	public static final int T_STOP = 0;

	protected volatile int status;

	/** start the working Task */
	public abstract void startCTask(String...args);
	
	/** stop the working Task */
	public void stopCTask() {
		setStatus(T_STOP);
	}

	public void setStatus(int s) {
		status = s;
	}

	public int getStatus() {
		return status;
	}

}
