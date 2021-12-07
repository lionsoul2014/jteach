package org.lionsoul.jteach.util;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Scanner;

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
	public static final String SERVER_RCMD_ALL = "a";
	public static final String SERVER_RMCD_SINGLE = "o";
	public static final String RCMD_NOREPLY_VAL = "NULL";
	
	public static final int FILE_UPLOAD_ONCE_SIZE  = 50; //unit k
	public static final int _WIDTH = 54;
	public static final String SB = "sb";
	public static final String SM = "sm";
	public static final String UF = "uf";
	public static final String RC = "rc";
	public static final String LS = "ls";
	public static final String DELE = "dele";
	public static final String MENU = "menu";
	public static final String STOP = "stop";
	public static final String EXIT = "exit";
	
	//arguments name and value
	public static final String CMD_KEY = "cmd";
	
	/**if s=true the server will send an exit symbol to all the JBeans*/
	public static final String EXIT_CLOSE_KEY = "s";
	public static final String EXIT_CLOSE_VAL = "true";
	
	/* if i=a all the JBeans will remove, if var i is an integer, the ith one will remove*/
	public static final String DELETE_KEY = "i";
	public static final String DELETE_ALL_VAL = "a";
	
	public static final String SCREEN_MONITOR_KEY = "i";
	public static final String REMOTE_CONTROL_KEY = "c";
	public static final String REMOTE_CONTROL_VAL = "true";
	public static final String MONITOR_BROADCAST_KEY = "b";
	public static final String MONITOR_BROADCAST_VAL = "true";
	
	/**if i = a, the command to send to all the JBeans, if var i is an integer
	 * the command will sent to the ith one */
	public static final String RCMD_EXECUTE_KEY = "i";
	public static final String RCMD_EXECUTE_VAL = "a";
	
	
	/**host*/
	public static final String LOCALHOST = "127.0.0.1";
	public static final String HOST_LOCAl_KEY = "local";
	public static final String HOST_REMOTE_KEY = "remote";
	
	/**
	 * show the handler menu 
	 */
	public static void showCmdMenu() {
		StringBuffer info = new StringBuffer();
		info.append(CmdUtil.getBaseLine(_WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine(" JTeach - Multimedia Teaching Software", _WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine(" @email:chenxin619315@gmail.com - chenxin", _WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine("                                 ", _WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine(" JTeach Command Menu :", _WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine(" ---"+SB+": start the screen broadcast thread", _WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine(" ---"+SM+": monitor the specified JBean's screen", _WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine(" ---"+UF+": start the file upload thread", _WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine(" ---"+RC+": run command on all/specified online JBean", _WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine(" ---"+LS+": list all the online students", _WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine(" -"+DELE+": remove all/specified online JBean", _WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine(" -"+MENU+": show the function menu", _WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine(" -"+STOP+": stop the current working thread", _WIDTH)+"\n");
		info.append(CmdUtil.getFormatLine(" -"+EXIT+": exit the program", _WIDTH)+"\n");
		info.append(CmdUtil.getBaseLine(_WIDTH)+"\n");
		System.out.print(info);
	}
	
	public static String getFormatLine(String str, int w) {
		StringBuffer sb = new StringBuffer();
		sb.append("|");
		sb.append(str);
		for ( int j = 0; j < w - str.length(); j++ ) {
			sb.append(" ");
		}
		sb.append("|");
		return sb.toString();
	}
	
	public static String getBaseLine(int w) {
		StringBuffer sb = new StringBuffer();
		for ( int j = 0; j < w; j++ ) {
			if ( j == 0 ) sb.append("+");
			sb.append("-");
			if ( j == w - 1 ) sb.append("+"); 
		}
		return sb.toString();
	}
	
	public static String askForInput() {
		InputStream in = System.in;
		Scanner reader = new Scanner(in);
		String _input = reader.next();
		reader.close();
		return _input;
	}
	
	/**
	 * limit a string to a point length 
	 */
	public static String formatString(String str, int len, char letter) {
		if ( str.length() >= len ) return str;
		for ( int j = (len - str.length()); j > 0; j-- ) {
			str = letter+str;
		}
		return str;
	}
	
	/**
	 * parese command line arguments 
	 */
	public static HashMap<String, String> parseCMD(String str) {
		HashMap<String, String> arguments = new HashMap<String, String>();
		str = str.trim();
		if ( str.length() == 0 ) return arguments; 
		int cmdEnd = str.indexOf(' ', 0);
		if ( cmdEnd == -1 ) cmdEnd = str.length(); 
		/**command name*/
		arguments.put(CMD_KEY, str.substring(0, cmdEnd));
		
		/**the string after the clear of command name - arugments part*/
		str = str.substring(cmdEnd, str.length()).trim();
		
		/**
		 * search all the name and value arguments piar
		 * and store them in HashMap arguments 
		 */
		for ( int j = 0; j < str.length(); j++ ) {
			if ( str.charAt(j) == '-' ) {
				/**get the next space and the argument key*/
				int nEnd = str.indexOf(' ', ++j);
				if ( nEnd == -1 ) break; 
				String _key = str.substring(j, nEnd).toLowerCase();
				j = nEnd + 1;
				if ( j >= str.length() ) break; 
				
				/**get the next non-space*/
				while ( j < str.length() && str.charAt(j) == ' ' ) j++;
				if ( j >= str.length() ) break; 
				
				/**get the next space*/
				int vEnd = str.indexOf(' ', j);
				if ( vEnd == -1 ) vEnd = str.length();
				String _val = str.substring(j, vEnd).toLowerCase();
				j = vEnd;

				System.out.println("-+--key:"+_key+", value:"+_val+"--+-");
				/**if the argument pair is not exist and them to the HashMap*/
				if ( arguments.get(_key) == null ) arguments.put(_key, _val);
			}
		}
		return arguments;
	}
	
	/**
	 * get all the ip for the local machine 
	 */
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
	            	if ( ip.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$") == false ) continue; 
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
		} catch (SocketException e) {}  
		return hosts;
	}
	
	/**
	 * get the absolute parent path for the jar file. 
	 */
	public static String getJarHome(Object o) {
		String path = o.getClass().getProtectionDomain()
					.getCodeSource().getLocation().getFile();
		File jarFile = new File(path);
		return jarFile.getParentFile().getAbsolutePath();
	}
}
