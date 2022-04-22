package org.lionsoul.jteach.rmi;

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
	 */
	BufferedImage createScreenCapture(Rectangle screenRect) throws RemoteException;

	/**
	 * get the Color in the specified pixel
	 */
	Color getPixelColor(int x, int y) throws RemoteException;
	
	/**
	 * handle the key press action
	 */
	void keyPress(int keyCode) throws RemoteException;
	
	/**
	 * handle the key Release action
	 */
	void keyRelease(int keyCode) throws RemoteException;
	
	/**
	 * handle the mouse move action
	 */
	void mouseMove(int x, int y) throws RemoteException;
	
	/**
	 * handle the mouse press action
	 */
	void mousePress(int buttons) throws RemoteException;
	
	/**
	 * handle the mouse release action
	 */
	void mouseRelease(int buttons) throws RemoteException;
	
	/**
	 * handle the mouse wheel action
	 */
	void mouseWheel(int wheelAmt) throws RemoteException;
}
