package com.webssky.jteach.server.task;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.webssky.jteach.server.JBean;
import com.webssky.jteach.server.JServer;
import com.webssky.jteach.util.JCmdTools;


/**
 * Broadcast Sender Task. <br />
 * @author chenxin - chenxin619315@gmail.com <br />
 * {@link http://www.webssky.com} 
 */
public class SBTask implements JSTaskInterface,Runnable {
	
	public static final String START_TIP = "Broadcast Thread Is Started.(Run stop To Stop It)";
	public static final String STOPING_TIP = "Broadcast Thread Is Stoping...";
	public static final String STOPED_TIP = "Broadcast Thread Is Stoped.";
	public static final String THREAD_NUMBER_TIP = "Thread Numbers: ";
	public static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
	public static Object IMAGE_LOCK = new Object();
	
	private int TStatus = T_RUN;
	
	private Thread imgSendT = null;
	private LinkedList<BufferedImage> images = null;
	private Robot robot = null;
	private ArrayList<JBean> beans = null;
	private ArrayList<GroupImageSendTask> groupArr = new ArrayList<GroupImageSendTask>();
	
	public SBTask() {
		images = new LinkedList<BufferedImage>();
		try {
			robot = new Robot();
		} catch (AWTException e) {
			System.out.println("Create Robot Object Failed.");
		}
		beans = JServer.makeJBeansCopy();
		int g_numbers = (int) Math.ceil( (float) beans.size() / JServer.getInstance().getGroupOpacity());
		/*group all the beans*/
		for ( int j = 0; j < g_numbers; j++ ) {
			ArrayList<JBean> list = new ArrayList<JBean>();
			int start = j * JServer.getInstance().getGroupOpacity();
			int end = ( j + 1 ) * JServer.getInstance().getGroupOpacity();
			if ( end > beans.size() ) end = beans.size(); 
			for ( int i = start; i < end; i++ ) 
				list.add(beans.get(i));
			GroupImageSendTask task = new GroupImageSendTask(list);
			groupArr.add(task);
		}
	}
	
	/**
	 * @see JSTaskInterface#startTask() 
	 */
	@Override
	public void startTask() {
		System.out.println(START_TIP);
		//send broadcast start cmd to all the beans;
		JBeans_Cmd_Symbol(JCmdTools.SERVER_BROADCAST_START_CMD);
		//start image geter thread
		JServer.threadPool.execute(this);
		//start image send thread
		imgSendT = new Thread(new ImageSendTask());
		//imgSendT.setDaemon(true);
		imgSendT.start();
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
		BufferedImage img = null;
		BufferedImage B_IMG = null;
		while ( getTSTATUS() == T_RUN ) {
			//load img
			img = robot.createScreenCapture(new Rectangle(SCREEN_SIZE.width, SCREEN_SIZE.height));
			if ( B_IMG == null ) B_IMG = img;
			/*
			 * we need to check the image
			 * if over 98% of the picture is the same
			 * and there is no neccessay to send the picture 
			 */
			else if ( ImageEquals(B_IMG, img) ) continue;
			AddImage(img);
		}
		if ( imgSendT != null ) imgSendT.interrupt();
	}
	
	public void AddImage(BufferedImage img) {
		synchronized ( IMAGE_LOCK ) {
			images.addLast(img);
		}
	}
	public BufferedImage removeImage() {
		synchronized ( IMAGE_LOCK ) {
			if (images.size() == 0) return null;
			return images.removeFirst();
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
				B_IMG = removeImage();
				if ( B_IMG == null ) continue;
				
				/*Mouse Location Info */
				mouse = MouseInfo.getPointerInfo().getLocation();
				
				/*
				 * turn the BufferedImage to Byte and compress the byte data
				 * 		then send them to all the beans
				 */
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				try {
					JPEGCodec.createJPEGEncoder(bos).encode(B_IMG);
					data = bos.toByteArray();
					bos.flush();
				} catch (ImageFormatException e1) {
					continue;
				} catch (IOException e1) {
					continue;
				}
				
				/*
				 * send image data to all the beans.
				 * 		if a new bean add the BeanDB and want make it work
				 * 		you should stop the braodcast and restart the broadcast.
				 */
				synchronized ( IMAGE_LOCK ) {
					Iterator<GroupImageSendTask> it = groupArr.iterator();
					GroupImageSendTask task;
					while ( it.hasNext() ) {
						task = it.next();
						task.refresh(mouse, data);
					}
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
