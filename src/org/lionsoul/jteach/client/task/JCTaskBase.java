package org.lionsoul.jteach.client.task;

import org.lionsoul.jteach.client.JClient;
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.JBean;

/**
 * Task Interface for JTeach Client
 * @author chenxin
 */
public abstract class JCTaskBase implements Runnable {
	public static final int T_RUN = 1;
	public static final int T_STOP = 0;
	public static final Log log = Log.getLogger(JCTaskBase.class);

	/** task running status and default it to T_RUN */
	protected volatile int status = T_RUN;
	protected final JClient client;
	protected final JBean bean;
	private final Object lock = new Object();

	protected JCTaskBase(JClient client) {
		this.client = client;
		this.bean = client.getBean();
	}

	/** initialize worker */
	protected boolean _before(String... args) {
		return true;
	}

	/** return true for the start will wait until the task is over */
	protected boolean _wait() {
		return false;
	}

	/** do the specified worker */
	protected abstract void _run();

	/** task overed callback */
	protected void onExit() {
		client.resetJCTask();
		log.debug("task %s stopped", this.getClass().getName());
		client.notifyCmdMonitor();
	}

	@Override
	public void run() {
		log.debug("task %s is running ... ", this.getClass().getName());
		client.setTipInfo(String.format("task %s is now running", this.getClass().getSimpleName()));
		// 1, run the task
		_run();
		// 2, task finished and call the exit callback
		onExit();
		// 3, check and notify the wait lock
		if (_wait()) {
			synchronized (lock) {
				lock.notify();
			}
		}
	}

	/** start the working Task */
	public boolean start(String... args) {
		// run the before initialize worker
		if (!_before(args)) {
			return false;
		}

		JBean.threadPool.execute(this);
		if (_wait()) {
			synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					stop();
					return false;
				}
			}
		}

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