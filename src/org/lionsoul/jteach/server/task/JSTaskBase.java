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

	protected volatile int status = T_RUN;
	protected final JServer server;
	protected final List<JBean> beanList;
	protected final Object lock = new Object();
	protected volatile boolean fromStop = false;

	protected JSTaskBase(JServer server) {
		this.server = server;
		this.beanList = Collections.synchronizedList(server.copyBeanList());
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
	protected void _exit() {
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

		server.resetJSTask();
		server.println(String.format("task %s stopped", this.getClass().getName()));

		/* notify to exit the lock.wait */
		if (fromStop || _wait()) {
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}

	@Override
	public void run() {
		// 1, run the task
		_run();
		// 2, task finished and call the exit callback
		_exit();
	}

	/** start the Task */
	public boolean start() {
		// run the before initialize worker
		if (!_before()) {
			server.resetJSTask();
			return false;
		}

		server.println(String.format("task %s is now running", this.getClass().getName()));
		JBean.threadPool.execute(this);
		if (_wait()) {
			synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					server.println("start lock.wait interrupted");
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
		fromStop = true;
		setStatus(T_STOP);
		synchronized (lock) {
			try {
				lock.wait();
			} catch (InterruptedException e) {
				server.println("stop lock.wait interrupted");
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
