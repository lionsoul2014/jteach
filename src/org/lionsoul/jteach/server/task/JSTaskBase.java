package org.lionsoul.jteach.server.task;

import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.server.JServer;

import java.util.Collections;
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
	
	/** start the Task */
	public abstract boolean start();

	/* add a new client */
	public abstract void addClient(JBean bean);

	/** stop the working Task */
	public void stop() {
		setStatus(T_STOP);
	}

	public final void setStatus(int status) {
		this.status = status;
	}

	public final int getStatus() {
		return status;
	}

}
