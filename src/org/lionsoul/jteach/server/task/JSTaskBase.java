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

	protected JSTaskBase(JServer server) {
		this.server = server;
		this.beanList = Collections.synchronizedList(server.copyBeanList());
	}

	/** initialize worker */
	protected boolean _before() {
		return true;
	}

	/** do the specified worker */
	protected abstract void _run();

	/** task overed callback */
	protected void onExit() {
		server.resetJSTask();
		server.lnPrintln(String.format("task %s stopped", this.getClass().getSimpleName()));
		server.printInputAsk();
	}

	@Override
	public void run() {
		server.lnPrintln(String.format("task %s is now running", this.getClass().getSimpleName()));
		// 1, run the task
		_run();
		// 2, task finished and call the exit callback
		onExit();
	}

	/** start the Task */
	public boolean start() {
		// run the before initialize worker
		if (!_before()) {
			return false;
		}

		JBean.threadPool.execute(this);
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

		server.println("task %s stopped", this.getClass().getName());
	}

	public final void setStatus(int status) {
		this.status = status;
	}

	public final int getStatus() {
		return status;
	}

}
