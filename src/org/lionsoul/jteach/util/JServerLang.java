package org.lionsoul.jteach.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.lionsoul.jteach.server.JServer;


/**
 * language package for JTeach Server
 *
 * @author chenxin - chenxin619315@gmail.com
 */
public class JServerLang {
	
	public static final void SERVER_INIT() {
		System.out.println("Initialize Server...");
	}
	
	public static final void SERVER_INIT_FAILED() {
		System.out.println("Failed To Initialize The Server, Make Sure Port "+ JServer.PORT+" Is Valid.");
	}
	
	public static final void MONITOR_INFO() throws UnknownHostException {
		String host = InetAddress.getLocalHost().getHostAddress();
		/* get the linux's remote host*/
		if ( JServer.OS.equals("LINUX") ) {
			HashMap<String, String> ips = JCmdTools.getNetInterface();
			String remote = ips.get(JCmdTools.HOST_REMOTE_KEY);
			if ( remote != null ) host = remote;
		}
		System.out.println("Monitor - Host: "+host+", Port: "+JServer.PORT);
	}
	
	public static final void GETINFO_FAILED() {
		System.out.println("Failed To Get Information From LocalHost");
	}
	
	public static final void INPUT_ASK() {
		System.out.print("JTeach>> ");
	}
	
	public static final void RCMD_INPUT_ASK() {
		System.out.print("JTeach#RC>> ");
	}

	public static final void SERVER_ACCEPT_ERROR() {
		System.out.println("Server Monitor Error, Please Restart Program.");
	}
	
	public static final void UNKNOW_COMMAND() {
		System.out.println("Unknow Command!");
	}
	
	public static final void PROGRAM_OVERED() {
		System.out.println("Bye!");
	}
	
	
	public static final void EMPTY_JBENAS() {
		System.out.println("Empty Set.");
	}
	
	/**
	 * remove 
	 */
	public static final void DELETE_JBEAN_EMPTY_ARGUMENTS() {
		System.out.println("-+-i : a/Integer: Remove The All/ith JBean");
	}
	
	/**
	 * screen monitor 
	 */
	public static final void SCREEN_MONITOR_EMPTY_ARGUMENTS() {
		System.out.println("-+-i : Integer: Monitor the specifier JBean's screen");
	}
	
	/**
	 * remote command execute 
	 */
	public static final void RCMD_EXECUTE_EMPTY_ARGUMENTS() {
		System.out.println("-+-i : a/Integer: Send command to all/ith JBean");
	}
}
