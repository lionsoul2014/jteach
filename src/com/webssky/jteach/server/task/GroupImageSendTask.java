package com.webssky.jteach.server.task;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.webssky.jteach.server.JBean;
import com.webssky.jteach.server.JServer;
import com.webssky.jteach.util.JCmdTools;

/**
 * send the screen image data to the pointed group
 * 		call by SBTask <br />
 * 
 * @author chenxin - chenxin619315@gmail.com <br />
 */
public class GroupImageSendTask implements Runnable {
	
	public static final int D_NEW = 1;
	public static final int D_OLD = -1;
	
	private Object DLOCK = new Object();
	private int d_val = D_OLD;
	
	private final List<JBean> beans;
	private Point mouse = null;
	private byte[] data = null;
	
	public GroupImageSendTask(List<JBean> b) {
		beans = b;
	}
	
	/**
	 * refresh the image data and mouse info.
	 * 
	 * @param p <br />
	 * @param d
	 */
	public void refresh(Point p, byte[] d) {
		if ( getDValue() == D_OLD ) { 
			d_val = D_NEW;
			mouse = p;
			data = d;
			JServer.threadPool.execute(this);
		}
	}
	
	/**
	 * send image byte data to all JBeans that belongs to this group. <br />
	 * 		stop send the image byte[] to the rest when one is offline. <br />
	 */
	@Override
	public void run() {
		final Iterator<JBean> it = beans.iterator();
		while ( it.hasNext() ) {
			JBean b = it.next();
			try {
				/*
				 * first load the heart beat symbol from the client.
				 * this is to make sure the client is still on line.
				 * if we didn't receive a symbol from in JCmdTools.SO_TIMEOUT milliseconds.
				 * that mean is client is off line.
				 */
				// b.getReader().readChar();
				b.send(JCmdTools.SEND_DATA_SYMBOL, mouse.x, mouse.y, data.length, data);
				// System.out.println("sended");
			} catch (IOException e) {
				it.remove();b.clear();
			}
		}
		setDValue(D_OLD);
	}
	
	public void setDValue( int val ) {
		synchronized( DLOCK ) {
			d_val = val;
		}
	}
	
	public int getDValue() {
		synchronized ( DLOCK ) {
			return d_val;
		}
	}
}
