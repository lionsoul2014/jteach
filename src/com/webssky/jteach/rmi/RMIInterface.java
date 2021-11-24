package com.webssky.jteach.rmi;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * remote interface
 * @author chenxin - chenxin619315@gmail.com
 */
public interface RMIInterface extends Remote {
	/**
	 * get the Remote Screen shot
	 * @param screenRect
	 */
	public BufferedImage createScreenCapture(Rectangle screenRect) throws RemoteException;

	/**
	 * get the Color in the specified pixel
	 * @param x
	 * @param y
	 */
	public Color getPixelColor(int x, int y) throws RemoteException;
	
	/**
	 * handle the key press action
	 * @param keyCode
	 */
	public void keyPress(int keyCode) throws RemoteException;
	
	/**
	 * handle the key Release action
	 * @param keyCode
	 */
	public void keyRelease(int keyCode) throws RemoteException;
	
	/**
	 * handle the mouse move action
	 * @param x
	 * @param y
	 */
	public void mouseMove(int x, int y) throws RemoteException;
	
	/**
	 * handle the mouse press action
	 * @param buttons
	 */
	public void mousePress(int buttons) throws RemoteException;
	
	/**
	 * handle the mouse release action
	 * @param buttons
	 */
	public void mouseRelease(int buttons) throws RemoteException;
	
	/**
	 * handle the mouse wheel action
	 * @param wheelAmt
	 */
	public void mouseWheel(int wheelAmt) throws RemoteException;
}
