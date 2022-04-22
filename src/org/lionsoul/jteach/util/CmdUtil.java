package org.lionsoul.jteach.util;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * Offer Some Interface to show message in the console
 * @author chenxin - chenxin619315@gmail.com
 */
public class CmdUtil {
	
	/**
	 * Commands and Symbol
	 */
	public static final byte SYMBOL_SEND_CMD = 'C';
	public static final byte SYMBOL_SEND_DATA = 'D';
	public static final byte SYMBOL_SEND_ARP = 'P';
	public static final byte SYMBOL_SEND_HBT = 'H';
	public static final byte SYMBOL_SOCKET_CLOSED = 'S';
	public static final byte[] SYMBOL_LIST = new byte[] {
		SYMBOL_SEND_CMD,
		SYMBOL_SEND_DATA,
		SYMBOL_SEND_ARP,
		SYMBOL_SEND_HBT,
		SYMBOL_SOCKET_CLOSED
	};

	public static final boolean validSymbol(byte symbol) {
		for (byte b: SYMBOL_LIST) {
			if (b == symbol) {
				return true;
			}
		}
		return false;
	}


	public static final int COMMAND_NULL = -1;
	public static final int COMMAND_EXIT = 0;
	public static final int COMMAND_TASK_STOP = 1;
	public static final int COMMAND_TASK_START = 2;
	public static final int COMMAND_TASK_RUNNING = 3;
	public static final int COMMAND_TASK_PAUSED = 4;
	public static final int COMMAND_TASK_EXITED = 4;
	public static final int COMMAND_BROADCAST_START = 2;
	public static final int COMMAND_UPLOAD_START = 3;
	public static final int COMMAND_SCREEN_MONITOR = 4;
	public static final int COMMAND_RCMD_SINGLE_EXECUTION = 5;
	public static final int COMMAND_RCMD_ALL_EXECUTION = 6;
	public static final boolean validCommand(byte cmd) {
		if (cmd >= COMMAND_NULL && cmd <= COMMAND_RCMD_ALL_EXECUTION) {
			return true;
		}
		return false;
	}

	/* task type */
	public static final short TASK_BROADCAST = 1;
	public static final short TASK_FILE_UPLOAD = 2;
	public static final short TASK_SCREEN_MONITOR = 3;
	public static final short TASK_RCMD_SINGLE_EXECUTION = 4;
	public static final short TASK_RCMD_ALL_EXECUTION = 5;
	public static final boolean validTask(short task) {
		if (task >= TASK_BROADCAST && task <= TASK_RCMD_ALL_EXECUTION) {
			return true;
		}
		return false;
	}

	/* screen monitor and remote control key */
	public static final String MONITOR_AND_CONTROL = "MAC";
	public static final String MONITOR_NOT_CONTROL = "MNC";
	public static final String RMI_SERVER_READY = "RSR";
	public static final String RMI_OBJ = "RMI_OBJ";
	public static final int RMI_PORT = 55568;
	
	/* remote command execute key */
	public static final String RCMD_NOREPLY_VAL = "NULL";

	//arguments name and value
	public static final String CMD_KEY = "cmd";
	
	/**host*/
	public static final String LOCALHOST = "127.0.0.1";
	public static final String HOST_LOCAl_KEY = "local";
	public static final String HOST_REMOTE_KEY = "remote";
	
	/** limit a string to a point length */
	public static String formatString(String str, int len, char letter) {
		if ( str.length() >= len ) return str;
		for ( int j = (len - str.length()); j > 0; j-- ) {
			str = letter+str;
		}
		return str;
	}
	
	/** get all the ip for the local machine */
	public static HashMap<String, String> getNetInterface() {
		HashMap<String, String> hosts = new HashMap<>();
		Enumeration<NetworkInterface> netI;
		try {
			netI = NetworkInterface.getNetworkInterfaces();
			while (netI.hasMoreElements()) {
	            NetworkInterface ni = netI.nextElement();   
	            Enumeration<InetAddress> ipEnum = ni.getInetAddresses();  
	            while (ipEnum.hasMoreElements()) {
	            	String ip = ipEnum.nextElement().getHostAddress().trim();
	            	if (!ip.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$")) continue;
	            	//System.out.println(ip);
	            	if ( ip.equals(LOCALHOST) ) {
	            		if ( hosts.get(HOST_LOCAl_KEY) != null )  continue;
	            		hosts.put(HOST_LOCAl_KEY, ip);
	            	} else {
	            		if ( hosts.get(HOST_REMOTE_KEY) != null ) continue;
	            		hosts.put(HOST_REMOTE_KEY, ip);
	            	}
	            }
			}
		} catch (SocketException ignored) {}
		return hosts;
	}
	
	/** get the absolute parent path for the jar file.  */
	public static String getJarHome(Object o) {
		String path = o.getClass().getProtectionDomain()
					.getCodeSource().getLocation().getFile();
		File jarFile = new File(path);
		return jarFile.getParentFile().getAbsolutePath();
	}

}
