package org.lionsoul.jteach.server.task;

import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.server.JServer;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Task Interface for JTeach Server
 * @author chenxin - chenxin619315@gmail.com
 */
public abstract class JSTaskBase implements Runnable {
	
	public static final int T_RUN = 1;
	public static final int T_STOP = 0;
	private static final Log log = Log.getLogger(JSTaskBase.class);

	protected volatile int status = T_RUN;
	protected final JServer server;
	protected final List<JBean> beanList;

	protected final Object lock = new Object();
	protected volatile boolean fromStop = false;

	protected JSTaskBase(JServer server) {
		this.server = server;
		this.beanList = Collections.synchronizedList(new ArrayList<>());
	}

	/** before prepare task */
	protected boolean _before(List<JBean> beans) {
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
				boolean r = bean.offer(Packet.COMMAND_TASK_STOP, JBean.DEFAULT_OFFER_TIMEOUT_SECS, TimeUnit.SECONDS);
				if (!r) {
					log.error("client %s removed due to offer timeout", bean.getHost());
					it.remove();
				}
			} catch (IllegalAccessException e) {
				log.error(bean.getClosedError());
				it.remove();
			} catch (InterruptedException e) {
				log.warn("bean %s offer interrupted due to %s", bean.getHost(), e.getClass().getName());
				it.remove();
			}
		}

		server.resetJSTask();
		server.println(String.format("task %s stopped", this.getClass().getName()));
		if (!fromStop && !_wait()) {
			server.printInputAsk();
		}

		/* notify to exit the lock.wait */
		synchronized (lock) {
			lock.notifyAll();
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
	public boolean start(List<JBean> beans, Packet startPacket) {
		if (!_before(beans)) {
			return false;
		}

		if (beans.size() == 0) {
			server.println("empty task client list");
			return false;
		}

		// send the start command to all the beans
		if (startPacket == null) {
			beanList.addAll(beans);
		} else {
			final Iterator<JBean> it = beans.iterator();
			while (it.hasNext()) {
				final JBean bean = it.next();
				try {
					if (bean.offer(startPacket)) {
						log.debug("offer start packet to client %s succeed", bean.getHost());
						beanList.add(bean);
					}
				} catch (IllegalAccessException e) {
					log.error(bean.getClosedError());
					it.remove();
				}
			}
		}

		if (beanList.size() == 0) {
			server.println("empty global client list");
			return false;
		}

		server.println("task %s is now running", this.getClass().getName());
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
			/* notify the task has stopped */
			return false;
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

	/** send a specified Packet to the bean list */
	protected int send(Packet p) {
		int count = 0;
		synchronized (beanList) {
			final Iterator<JBean> it = beanList.iterator();
			while ( it.hasNext() ) {
				final JBean bean = it.next();
				try {
					if (bean.offer(p)) {
						count++;
					}
				} catch (IllegalAccessException e) {
					server.println("client %s removed due to %s: %s",
							bean.getHost(), e.getClass().getName(), e.getMessage());
					it.remove();
				}
			}
		}

		return count;
	}

	public final void setStatus(int status) {
		this.status = status;
	}

	public final int getStatus() {
		return status;
	}

}
