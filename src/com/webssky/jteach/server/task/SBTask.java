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


/**
 * Broadcast Sender Task. <br />
 * @author chenxin - chenxin619315@gmail.com <br />
 * {@link <a href="http://www.webssky.com">http://www.webssky.com</a>}
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
	private final ArrayList<JBean> beans;
	private Thread imgSendT;
	
	public SBTask() throws AWTException {
		robot = new Robot();
		beans = JServer.makeJBeansCopy();
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
			if ( b.getSocket() == null ) continue;
			if ( b.getSocket().isClosed() ) continue;
			try {
				if ( cmd == JCmdTools.SERVER_BROADCAST_START_CMD )
					b.getSocket().setSoTimeout(JCmdTools.SO_TIMEOUT);
				else 
					b.getSocket().setSoTimeout(0);
				b.send(JCmdTools.SEND_CMD_SYMBOL, cmd);
			} catch ( SocketException e ) {
				System.out.println("Fail to send the socket timeout->"+b);
			} catch (IOException e) {
				e.printStackTrace();
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
			BufferedImage img = robot.createScreenCapture(new Rectangle(SCREEN_SIZE.width, SCREEN_SIZE.height));
			if ( B_IMG == null ) B_IMG = img;
			/*
			 * we need to check the image
			 * if over 98% of the picture is the same
			 * and there is no necessary to send the picture 
			 */
			else if ( ImageEquals(B_IMG, img) ) {
				continue;
			}
			
			
			/*
			 * turn the BufferedImage to Byte and compress the byte data
			 * then send them to all the beans */
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
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
			BufferedImage B_IMG = null;
			Point mouse = null;
			byte[] data = null;
			while ( getTSTATUS() == T_RUN ) {
				final MsgItem msg;
				try {
					msg = msgQueue.take();
				} catch (InterruptedException e) {
					continue;
				}

				/*
				 * send image data to all the beans.
				 * 	if a new bean add the BeanDB and want make it work
				 * 	you should stop the broadcast and restart the broadcast.
				*/
				Iterator<JBean> it = beans.iterator();
				while (it.hasNext()) {
					JBean b = it.next();
					try {
						/*
						 * first load the heart beat symbol from the client.
						 * this is to make sure the client is still on line.
						 * if we didn't receive a symbol from in JCmdTools.SO_TIMEOUT milliseconds.
						 * that mean is client is off line.
						 */
						b.send(JCmdTools.SEND_DATA_SYMBOL, msg.x, msg.y, msg.data.length, msg.data);
					} catch (IOException e) {
						e.printStackTrace();
						// it.remove();b.clear();
						continue;
					}
				}


				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
	}
	
	public static boolean ImageEquals(BufferedImage image1, BufferedImage image2) {
		int w1 = image1.getWidth();
		int h1 = image2.getHeight();
		int w2 = image1.getWidth();
		int h2 = image2.getHeight();
		if (w1 != w2 || h1 != h2) return false;
		for (int i = 0; i < w1; i += 4) {
			for (int j = 0; j < h1; j += 4) {
				if (image1.getRGB(i, j) != image2.getRGB(i, j))
					return false;
			}
		}
		return true;
	}
	
	public synchronized int getTSTATUS() {
		return TStatus;
	}
	
	public synchronized void setTSTATUS(int s) {
		TStatus = s;
	}
}
