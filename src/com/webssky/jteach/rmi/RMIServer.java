package com.webssky.jteach.rmi;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javax.swing.JOptionPane;

/**
 * remote server object
 * @author chenxin - chenxin619315@gmail.com
 */
public class RMIServer extends UnicastRemoteObject implements RMIInterface {
	
	private static final long serialVersionUID = 1L;
	public static final Dimension S_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
	private Robot robot = null;
	private static RMIServer _instance = null;
	
	public static RMIServer getInstance() throws RemoteException {
		if ( _instance == null ) _instance = new RMIServer();
		return _instance;
	}
	
	protected RMIServer() throws RemoteException {
		try {
			robot = new Robot();
		} catch (AWTException e) {
			JOptionPane.showMessageDialog(null, "Fail to create robot object.",
					"JTeach", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	public BufferedImage createScreenCpature() throws RemoteException {
		return createScreenCapture(new Rectangle(S_SIZE.width, S_SIZE.height));
	}

	@Override
	public BufferedImage createScreenCapture(Rectangle screenRect)
			throws RemoteException {
		if ( robot == null ) return null; 
		return robot.createScreenCapture(screenRect);
	}

	@Override
	public Color getPixelColor(int x, int y) throws RemoteException {
		if ( robot == null ) return null; 
		return robot.getPixelColor(x, y);
	}

	@Override
	public void keyPress(int keyCode) throws RemoteException {
		if ( robot == null ) return;
		robot.keyPress(keyCode);
	}

	@Override
	public void keyRelease(int keyCode) throws RemoteException {
		if ( robot == null ) return;
		robot.keyRelease(keyCode);
	}

	@Override
	public void mouseMove(int x, int y) throws RemoteException {
		if ( robot == null ) return;
		robot.mouseMove(x, y);
	}

	@Override
	public void mousePress(int buttons) throws RemoteException {
		if ( robot == null ) return;
		robot.mousePress(buttons);
	}

	@Override
	public void mouseRelease(int buttons) throws RemoteException {
		if ( robot == null ) return;
		robot.mouseRelease(buttons);
	}

	@Override
	public void mouseWheel(int wheelAmt) throws RemoteException {
		if ( robot == null ) return;
		robot.mouseWheel(wheelAmt);
	}

}
