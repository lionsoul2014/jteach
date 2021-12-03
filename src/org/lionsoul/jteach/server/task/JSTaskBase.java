package org.lionsoul.jteach.server.task;

import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.server.JServer;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Task Interface for JTeach Server
 * @author chenxin - chenxin619315@gmail.com
 */
public abstract class JSTaskBase implements Runnable {
	
	public static final int T_RUN = 1;
	public static final int T_STOP = 0;

	protected volatile int status;
	protected final JServer server;
	protected final List<JBean> beanList;
	protected final Thread wThread;
	protected final Object lock = new Object();

	protected JSTaskBase(JServer server) {
		this.server = server;
		this.beanList = Collections.synchronizedList(server.copyBeanList());
		this.wThread = new Thread(this);
	}

	/** initialize worker */
	protected boolean _before() {
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
		server.resetJSTask();
		server.println(String.format("task %s stopped", this.getClass().getName()));
		server.printInputAsk();
	}

	@Override
	public void run() {
		server.lnPrintln(String.format("task %s is now running", this.getClass().getSimpleName()));
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

	/** start the Task */
	public boolean start() {
		// run the before initialize worker
		if (!_before()) {
			server.resetJSTask();
			return false;
		}

		wThread.start();
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

	/* add a new client */
	public void addClient(JBean bean) {
		// default to do nothing
	}

	/** stop the working Task */
	public void stop() {
		server.println("stop task %s ... ", this.getClass().getName());
		setStatus(T_STOP);

		// send broadcast stop cmd to all the beans;
		final Iterator<JBean> it = beanList.iterator();
		while ( it.hasNext() ) {
			final JBean bean = it.next();
			try {
				bean.offer(Packet.COMMAND_TASK_STOP);
			} catch (IllegalAccessException e) {
				bean.reportClosedError();
				it.remove();
			}
		}
	}

	public final void setStatus(int status) {
		this.status = status;
	}

	public final int getStatus() {
		return status;
	}

}
