package com.webssky.jteach.rmi;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * remote interface
 * @author chenxin - chenxin619315@gmail.com
 * {@link http://www.webssky.com}
 */
public interface RMIInterface extends Remote {
	/**
	 * get the Remote Screen shot
	 * @param Rectangle 
	 */
	public BufferedImage createScreenCapture(Rectangle screenRect) throws RemoteException;

	/**
	 * get the Color in the specified pixel
	 * @param int
	 * @param int 
	 */
	public Color getPixelColor(int x, int y) throws RemoteException;
	
	/**
	 * handle the key press action
	 * @param int
	 */
	public void keyPress(int keyCode) throws RemoteException;
	
	/**
	 * handle the key Release action
	 * @param int 
	 */
	public void keyRelease(int keyCode) throws RemoteException;
	
	/**
	 * handle the mouse move action
	 * @param int
	 * @param int 
	 */
	public void mouseMove(int x, int y) throws RemoteException;
	
	/**
	 * handle the mouse press action
	 * @param int 
	 */
	public void mousePress(int buttons) throws RemoteException;
	
	/**
	 * handle the mouse release action
	 * @param int
	 */
	public void mouseRelease(int buttons) throws RemoteException;
	
	/**
	 * handle the mouse wheel action
	 * @param int 
	 */
	public void mouseWheel(int wheelAmt) throws RemoteException;
}
