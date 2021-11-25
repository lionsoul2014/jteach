package com.webssky.jteach.server.task;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import javax.imageio.ImageIO;

import com.sun.imageio.plugins.wbmp.WBMPImageReader;
import com.webssky.jteach.server.JBean;
import com.webssky.jteach.server.JServer;
import com.webssky.jteach.util.JCmdTools;
import com.webssky.jteach.util.JTeachIcon;
import javafx.concurrent.Task;


/**
 * Broadcast Sender Task. <br />
 * @author chenxin - chenxin619315@gmail.com <br />
 */
public class SBTask implements JSTaskInterface,Runnable {
	
	public static final String START_TIP = "Broadcast Thread Is Started.(Run stop To Stop It)";
	public static final String STOPING_TIP = "Broadcast Thread Is Stoping...";
	public static final String STOPED_TIP = "Broadcast Thread Is Stoped.";
	public static final String THREAD_NUMBER_TIP = "Thread Numbers: ";
	public static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
	
	private volatile int TStatus = T_RUN;

	private final Robot robot;
	private final List<TaskBean> beanList;

	public SBTask(JServer server) throws AWTException {
		robot = new Robot();
		beanList = Collections.synchronizedList(new ArrayList<>());
		for (JBean b : server.copyBeanList()) {
			beanList.add(new TaskBean(b));
		}
	}

	@Override
	public void addClient(JBean bean) {
		try {
			cmdSend(bean, JCmdTools.SERVER_BROADCAST_START_CMD);
			final TaskBean tBean = new TaskBean(bean);
			beanList.add(tBean);
			tBean.start();
			System.out.printf("add a new client %s\n", bean.getIP());
		} catch (IOException e) {
			System.out.printf("failed to add new client %s\n", bean.getIP());
		}
	}
	
	@Override
	public void startTask() {
		System.out.println(START_TIP);

		// send broadcast start cmd to all the beans;
		cmdSendAll(JCmdTools.SERVER_BROADCAST_START_CMD);

		// start all the task beans
		synchronized (beanList) {
			final Iterator<TaskBean> it = beanList.iterator();
			while (it.hasNext()) {
				it.next().start();
			}
		}

		// start image catch thread
		JServer.threadPool.execute(this);
	}

	@Override
	public void stopTask() {
		System.out.println(STOPING_TIP);
		setTSTATUS(T_STOP);

		/* stop all the task beans */
		synchronized (beanList) {
			final Iterator<TaskBean> it = beanList.iterator();
			while (it.hasNext()) {
				it.next().stop();
			}
		}

		// send broadcast stop cmd to all the beans;
		cmdSendAll(JCmdTools.SERVER_TASK_STOP_CMD);
		System.out.println(STOPED_TIP);
	}

	/* start the broadcast task for the specified bean */
	public void cmdSend(final JBean bean, final int cmd) throws IOException {
		// if ( cmd == JCmdTools.SERVER_BROADCAST_START_CMD ) {
		// 	b.getSocket().setSoTimeout(JCmdTools.SO_TIMEOUT);
		// } else {
		// 	b.getSocket().setSoTimeout(JCmdTools.SO_TIMEOUT);
		// }
		bean.getSocket().setSoTimeout(JCmdTools.SO_TIMEOUT);
		bean.send(JCmdTools.SEND_CMD_SYMBOL, cmd);
	}

	/* send symbol to all beans */
	private void cmdSendAll(int cmd) {
		Iterator<TaskBean> it = beanList.iterator();
		while ( it.hasNext() ) {
			final TaskBean tBean = it.next();
			if ( tBean.bean.getSocket() == null
					|| tBean.bean.getSocket().isClosed() ) {
				tBean.stop();
				it.remove();
				continue;
			}

			try {
				cmdSend(tBean.bean, cmd);
			} catch (IOException e) {
				tBean.bean.reportSendError();
				it.remove();tBean.bean.clear();
			}
		}
	}

	@Override
	public void run() {
		BufferedImage B_IMG = null;
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
			if ( B_IMG == null ) {
				B_IMG = img;
			} else if (JTeachIcon.ImageEquals(B_IMG, img) ) {
				continue;
			}
			
			
			/*
			 * turn the BufferedImage to Byte and compress the byte data
			 * then send them to all the beans */
			final byte[] data;
			final ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				// JPEGCodec.createJPEGEncoder(bos).encode(B_IMG);
				ImageIO.write(img, "jpeg", bos);
				data = bos.toByteArray();
				bos.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
				continue;
			}
			

			/*Mouse Location Info */
			final Point mouse = MouseInfo.getPointerInfo().getLocation();
			final MsgItem msg = new MsgItem(data, mouse.x, mouse.y);

			// remember the current img as the last
			// image the for the next round
			B_IMG = img;


			/* send the image data to the clients */
			synchronized (beanList) {
				final Iterator<TaskBean> it = beanList.iterator();
				while (it.hasNext()) {
					final TaskBean tBean = it.next();
					if (tBean.bean.getSocket() == null
							|| tBean.bean.getSocket().isClosed()) {
						tBean.stop();
						it.remove();
						continue;
					}

					// append the Message
					// to the current bean
					tBean.addMsg(msg);
				}
			}

			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	private class MsgItem {
		byte[] data;
		int x;
		int y;
		
		MsgItem(byte[] data, int x, int y) {
			this.data = data;
			this.x = x;
			this.y = y;
		}
	}

	/* ImageSendTask class */
	private class TaskBean implements Runnable {
		final JBean bean;
		private volatile int status = T_STOP;
		private volatile int counter;
		private final String ip;
		private final BlockingQueue<MsgItem> msgQueue;

		public TaskBean(final JBean bean) {
			this.bean = bean;
			this.counter = 0;
			this.ip = bean.getIP();
			msgQueue = new LinkedBlockingDeque<>(10);
		}

		public void addMsg(MsgItem msg) {
			msgQueue.offer(msg);
		}

		public void start() {
			if (status != T_RUN) {
				status = T_RUN;
				JServer.threadPool.execute(this);
			}
		}

		public void stop() {
			status = T_STOP;
		}

		@Override
		public void run() {
			boolean clearBean = false;
			while (status == T_RUN) {
				try {
					final MsgItem msg = msgQueue.take();

					/* receive the heartbeat from the client
					 * to make sure the client is now still alive */
					bean.getReader().readChar();
					bean.send(JCmdTools.SEND_DATA_SYMBOL, msg.x, msg.y, msg.data.length, msg.data);
				} catch (SocketTimeoutException e) {
					System.out.printf("client %s read timeout count %d\n", ip, counter);
					counter++;
					if (counter > 3) {
						clearBean = true;
						break;
					}
				} catch (EOFException e) {
					continue;
				} catch (IOException e) {
					clearBean = true;
					break;
				} catch (InterruptedException e) {
					System.out.println("queue.take were interrupted\n");
				}
			}

			/* clear the bean */
			status = T_STOP;
			if (clearBean) {
				bean.clear();
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
