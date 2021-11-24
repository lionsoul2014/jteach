package com.webssky.jteach.server.task;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import javax.imageio.ImageIO;

import com.webssky.jteach.client.JCWriter;
import com.webssky.jteach.server.JBean;
import com.webssky.jteach.server.JServer;
import com.webssky.jteach.util.JCmdTools;
import com.webssky.jteach.util.JTeachIcon;


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
	
	private int TStatus = T_RUN;

	private final BlockingQueue<MsgItem> msgQueue;
	private final Robot robot;
	private final List<JBean> beans;
	private Thread imgSendT;
	
	public SBTask(JServer server) throws AWTException {
		robot = new Robot();
		beans = server.copyBeanList();
		msgQueue = new LinkedBlockingDeque<>(10);
	}
	
	/**
	 * @see JSTaskInterface#startTask() 
	 */
	@Override
	public void startTask() {
		System.out.println(START_TIP);
		//send broadcast start cmd to all the beans;
		JBeans_Cmd_Symbol(JCmdTools.SERVER_BROADCAST_START_CMD);

		//start image send thread
		imgSendT = new Thread(new ImageSendTask());
		// mgSendT.setDaemon(true);
		imgSendT.start();

		//start image catch thread
		JServer.threadPool.execute(this);
	}

	/**
	 * @see JSTaskInterface#stopTask()
	 */
	@Override
	public void stopTask() {
		System.out.println(STOPING_TIP);
		setTSTATUS(T_STOP);
		//send broadcast stop cmd to all the beans;
		JBeans_Cmd_Symbol(JCmdTools.SERVER_TASK_STOP_CMD);
		System.out.println(STOPED_TIP);
	}
	
	/**
	 * send symbol to all beans 
	 */
	private void JBeans_Cmd_Symbol(int cmd) {
		Iterator<JBean> it = beans.iterator();
		while ( it.hasNext() ) {
			JBean b = it.next();
			if ( b.getSocket() == null ) {
				continue;
			}

			if ( b.getSocket().isClosed() ) {
				continue;
			}

			try {
				// if ( cmd == JCmdTools.SERVER_BROADCAST_START_CMD ) {
				// 	b.getSocket().setSoTimeout(JCmdTools.SO_TIMEOUT);
				// } else {
				// 	b.getSocket().setSoTimeout(JCmdTools.SO_TIMEOUT);
				// }
				b.getSocket().setSoTimeout(JCmdTools.SO_TIMEOUT);
				b.send(JCmdTools.SEND_CMD_SYMBOL, cmd);
			} catch ( SocketException e ) {
				System.out.println("Fail to send the socket timeout->"+b);
			} catch (IOException e) {
				// e.printStackTrace();
				b.reportSendError();
				it.remove();b.clear();
			}
		}
	}

	@Override
	public void run() {
		byte[] data = null;
		BufferedImage B_IMG = null;
		while ( getTSTATUS() == T_RUN ) {
			//load img
			final BufferedImage img = robot.createScreenCapture(
				new Rectangle(SCREEN_SIZE.width, SCREEN_SIZE.height)
			);
			if ( B_IMG == null ) {
				B_IMG = img;
			}
			/*
			 * we need to check the image
			 * if over 98% of the picture is the same
			 * and there is no necessary to send the picture 
			 */
			else if (JTeachIcon.ImageEquals(B_IMG, img) ) {
				continue;
			}
			
			
			/*
			 * turn the BufferedImage to Byte and compress the byte data
			 * then send them to all the beans */
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
			msgQueue.offer(new MsgItem(data, mouse.x, mouse.y));
			
			// remember the current img as the last
			// image the for the next round
			B_IMG = img;

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


	/**
	 * Screen Image send Thread. <br />
	 * @author chenxin 
	 */
	private class ImageSendTask implements Runnable {
		@Override
		public void run() {
			while ( getTSTATUS() == T_RUN ) {
				final MsgItem msg;
				try {
					msg = msgQueue.take();

					/*
					 * send image data to all the beans.
					 * 	if a new bean add the BeanDB and want make it work
					 * 	you should stop the broadcast and restart the broadcast.
					 */
					Iterator<JBean> it = beans.iterator();
					while (it.hasNext()) {
						final JBean b = it.next();
						try {
							/* receive the heartbeat from the client
							* to make sure the client is now still alive */
							b.getReader().readChar();
							b.send(JCmdTools.SEND_DATA_SYMBOL, msg.x, msg.y, msg.data.length, msg.data);
						} catch (IOException e) {
							// e.printStackTrace();
							b.reportSendError();
							it.remove();b.clear();
						}
					}
				} catch (InterruptedException e) {
					// e.printStackTrace();
					System.out.println("queue.take were interrupted\n");
				}

				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
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
