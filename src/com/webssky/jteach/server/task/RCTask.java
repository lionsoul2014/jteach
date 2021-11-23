package com.webssky.jteach.server.task;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import com.webssky.jteach.server.JBean;
import com.webssky.jteach.server.JServer;
import com.webssky.jteach.util.JCmdTools;
import com.webssky.jteach.util.JServerLang;


/**
 * thread for run command on online remote machine
 * @author chenxin - chenxin619315@gmail.com
 * {@link http://www.webssky.com} 
 */
public class RCTask implements JSTaskInterface {
	
	public static final String EXIT_CMD_STR = ":exit";
	
	private ArrayList<JBean> beans = null;
	private JBean bean = null;
	
	public RCTask() {}

	@Override
	public void startTask() {
		String str = JServer.getInstance().getArguments().get(JCmdTools.RCMD_EXECUTE_KEY);
		if ( str == null ) {
			JServerLang.RCMD_EXECUTE_EMPTY_ARGUMENTS();
			JServer.getInstance().resetJSTask();
			return;
		}
		
		/** send command to all the JBeans */
		if ( str.equals(JCmdTools.RCMD_EXECUTE_VAL) ) {
			beans = JServer.makeJBeansCopy();
			executeCMDToAll();
		}
		else {
			if ( str.matches("^[0-9]{1,}$") == false ) {
				System.out.println("Invalid index for rc command.");
				JServer.getInstance().resetJSTask();
				return;
			} 
			int index = Integer.parseInt(str);
			if ( index < 0 || index >= JServer.getInstance().getJBeans().size() ) {
				System.out.println("index out of bounds.");
				JServer.getInstance().resetJSTask();
				return;
			}
			/** send the command to spesified one*/
			bean = JServer.getInstance().getJBeans().get(index);
			executeCMDToSingle();
		}
		JServer.getInstance().resetJSTask();
		System.out.println("Remote Command execute thread is stoped.");
	}

	@Override
	public void stopTask() {
		
	}
	
	private void jbeans_cmd_sendAll(String cmd) {
		Iterator<JBean> it = beans.iterator();
		while ( it.hasNext() ) {
			JBean b = it.next();
			try {
				b.send(JCmdTools.SEND_DATA_SYMBOL, cmd);
			} catch (IOException e) {
				it.remove();b.clear();
			}
		}
	}
	
	/**
	 * run command to all the online machine 
	 */
	private void executeCMDToAll() {
		if ( beans == null ) return; 
		//send start symbol
		Iterator<JBean> it = beans.iterator();
		while ( it.hasNext() ) {
			JBean b = it.next();
			try {
				b.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_RCMD_EXECUTE_CMD);
				b.send(JCmdTools.SERVER_RCMD_ALL);
			} catch (IOException e) {
				it.remove();b.clear();
			}
		}
		
		InputStream in = System.in;
		Scanner reader = new Scanner(in);
		String line = null;
		System.out.println("-+-All JBeans, Run "+EXIT_CMD_STR+" to exit.-+-");
		while ( true ) {
			JServerLang.RCMD_INPUT_ASK();
			line = reader.nextLine().trim().toLowerCase();
			if ( line.equals("") ) {
				System.out.println("enter the command, or run "+EXIT_CMD_STR+" to exit.");
				continue;
			} 
			
			/**
			 * exit the remote command execute thread 
			 */
			if ( line.equals(EXIT_CMD_STR) ) {
				Iterator<JBean> ite = beans.iterator();
				while ( ite.hasNext() ) {
					JBean b = ite.next();
					try {
						b.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_TASK_STOP_CMD);
					} catch (IOException e) {
						ite.remove();b.clear();
					}
				}
				break;
			}
			/**
			 * valid command
			 * send the command to all the JBeans
			 */
			else jbeans_cmd_sendAll(line);
		}
		
		reader.close();
	}
	
	
	private void bean_sendCMD(String cmd) {
		try {
			bean.send(JCmdTools.SEND_DATA_SYMBOL, cmd);
			/**
			 * command execute response 
			 */
			DataInputStream dis = bean.getReader();
			String line = dis.readUTF();
			if ( line.equals(JCmdTools.RCMD_NOREPLY_VAL) ) return; 
			System.out.println(line);
		} catch (IOException e) {
			CMD_SEND_ERROR("execute command");
		}
	}
	
	/**
	 * run command to the specified mathine 
	 */
	private void executeCMDToSingle() {
		if ( bean == null ) return;
		try {
			bean.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_RCMD_EXECUTE_CMD);
			bean.send(JCmdTools.SERVER_RMCD_SINGLE);
		} catch (IOException e) {
			CMD_SEND_ERROR("start command");
			return;
		}
		
		InputStream in = System.in;
		Scanner reader = new Scanner(in);
		String line = null;
		System.out.println("-+-Single JBean, Run "+EXIT_CMD_STR+" to exit.-+-");
		while ( true ) {
			JServerLang.RCMD_INPUT_ASK();
			line = reader.nextLine().trim().toLowerCase();
			if ( line.equals("") ) {
				System.out.println("enter the command, or run "+EXIT_CMD_STR+" to exit.");
				continue;
			}
			
			/**
			 * exit the remote command execute thread 
			 */
			if ( line.equals(EXIT_CMD_STR) ) {
				try {
					bean.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_TASK_STOP_CMD);
				} catch (IOException e) {
					CMD_SEND_ERROR("stop command");
				}
				break;
			} else bean_sendCMD(line);
		}
		
		reader.close();
	}
	
	public static void CMD_SEND_ERROR(String str) {
		System.out.println("Fail to send "+str);
	}
	
}
