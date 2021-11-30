package org.lionsoul.jteach.server.task;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.ScreenMessage;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.server.JServer;


/**
 * Broadcast Sender Task.
 * @author chenxin - chenxin619315@gmail.com
 */
public class SBTask implements JSTaskInterface,Runnable {
	
	public static final String START_TIP = "Broadcast Thread Is Started.(Run stop To Stop It)";
	public static final String STOPING_TIP = "Broadcast Thread Is Stoping...";
	public static final String STOPED_TIP = "Broadcast Thread Is Stoped.";
	public static final String THREAD_NUMBER_TIP = "Thread Numbers: ";
	public static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();

	private volatile int TStatus = T_RUN;

	private final Robot robot;
	private final List<JBean> beanList;

	public SBTask(JServer server) throws AWTException {
		robot = new Robot();
		beanList = Collections.synchronizedList(server.copyBeanList());
	}

	@Override
	public void addClient(JBean bean) {
		try {
			boolean res = bean.offer(Packet.COMMAND_BROADCAST_START);
			if (res == false) {
				System.out.printf("failed to add new client %s\n", bean.getAddr());
			} else {
				beanList.add(bean);
				System.out.printf("add a new client %s\n", bean.getAddr());
			}
		} catch (IllegalAccessException e) {
			bean.reportClosedError();
		}
	}

	@Override
	public boolean start() {
		if (beanList.size() == 0) {
			System.out.println("Empty client list");
			return false;
		}

		System.out.println(START_TIP);

		// send broadcast start cmd to all the beans;
		final Iterator<JBean> it = beanList.iterator();
		while ( it.hasNext() ) {
			final JBean bean = it.next();
			try {
				bean.offer(Packet.COMMAND_BROADCAST_START);
			} catch (IllegalAccessException e) {
				bean.reportClosedError();
				it.remove();
			}
		}

		// start image catch thread
		JBean.threadPool.execute(this);
		return true;
	}

	@Override
	public void stop() {
		System.out.println(STOPING_TIP);
		setTSTATUS(T_STOP);

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

		System.out.println(STOPED_TIP);
	}

	@Override
	public void run() {
		// BufferedImage B_IMG = null;
		while ( getTSTATUS() == T_RUN ) {
			//load img
			final BufferedImage img = robot.createScreenCapture(
				new Rectangle(SCREEN_SIZE.width, SCREEN_SIZE.height)
			);

			/*
			 * we need to check the image
			 * if over 98% of the picture is the same
			 * and there is no necessary to send the picture
			 */
			// if ( B_IMG == null ) {
			// 	B_IMG = img;
			// } else if (JTeachIcon.ImageEquals(B_IMG, img) ) {
			// 	continue;
			// }


			/* encode the screen message */
			final Packet p;
			try {
				p = new ScreenMessage(MouseInfo.getPointerInfo().getLocation(), img).encode();
			} catch (IOException e) {
				System.out.printf("failed to decode screen image");
				continue;
			}

			// remember the current img as the last
			// image the for the next round
			// B_IMG = img;


			/* send the image data to the clients */
			synchronized (beanList) {
				final Iterator<JBean> it = beanList.iterator();
				while (it.hasNext()) {
					final JBean bean = it.next();
					try {
						// append the Message
						// to the current bean
						bean.offer(p);
					} catch (IllegalAccessException e) {
						bean.reportClosedError();
						it.remove();
					}
				}
			}

			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized int getTSTATUS() {
		return TStatus;
	}
	
	public synchronized void setTSTATUS(int s) {
		TStatus = s;
	}

}
