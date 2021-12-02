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

	/** initialize worker */
	protected boolean _before(String... args) {
		return true;
	}

	/** do the specified worker */
	protected abstract void _run();

	/** task overed callback */
	protected void onExit() {
		client.resetJCTask();
		client.notifyCmdMonitor();
	}

	@Override
	public void run() {
		// 1, run the task
		_run();
		// 2, task finished and call the exit callback
		onExit();
	}

	/** start the working Task */
	public boolean start(String... args) {
		// run the before initialize worker
		if (!_before(args)) {
			return false;
		}

		JBean.threadPool.execute(this);
		return true;
	}
	
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
