package com.webssky.jteach.server.task;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import com.webssky.jteach.server.JBean;
import com.webssky.jteach.server.JServer;
import com.webssky.jteach.util.JCmdTools;
import com.webssky.jteach.util.JServerLang;


/**
 * thread for run command on online remote machine
 * @author chenxin - chenxin619315@gmail.com
 */
public class RCTask implements JSTaskInterface {
	
	public static final String EXIT_CMD_STR = ":exit";
	private final JServer server;

	public RCTask(JServer server) {
		this.server = server;
	}

	@Override
	public void addClient(JBean bean) {

	}

	@Override
	public void startTask() {
		String str = JServer.getInstance().getArguments().get(JCmdTools.RCMD_EXECUTE_KEY);
		if ( str == null ) {
			JServerLang.RCMD_EXECUTE_EMPTY_ARGUMENTS();
			JServer.getInstance().resetJSTask();
			return;
		}
		
		/* send command to all the JBeans */
		if ( str.equals(JCmdTools.RCMD_EXECUTE_VAL) ) {
			executeToAll();
		} else {
			if ( str.matches("^[0-9]{1,}$") == false ) {
				System.out.println("Invalid index for rc command.");
				JServer.getInstance().resetJSTask();
				return;
			}

			int index = Integer.parseInt(str);
			if ( index < 0 || index >= server.beanCount() ) {
				System.out.println("index out of bounds.");
				JServer.getInstance().resetJSTask();
				return;
			}

			/* send the command to spesified one*/
			execute(index);
		}

		JServer.getInstance().resetJSTask();
		System.out.println("Remote Command execute thread is stoped.");
	}

	@Override
	public void stopTask() {
		
	}
	
	private void _sendToAll(String cmd) {
		final List<JBean> beanList = server.getBeanList();
		synchronized (beanList) {
			final Iterator<JBean> it = beanList.iterator();
			while ( it.hasNext() ) {
				final JBean b = it.next();
				try {
					b.send(JCmdTools.SEND_DATA_SYMBOL, cmd);
				} catch (IOException e) {
					it.remove();b.clear();
				}
			}
		}
	}
	
	/**
	 * run command to all the online machine 
	 */
	private void executeToAll() {
		if ( server.beanCount() == 0 ) {
			return;
		}

		// send start symbol
		final List<JBean> beanList = server.getBeanList();
		synchronized (beanList) {
			final Iterator<JBean> it = beanList.iterator();
			while ( it.hasNext() ) {
				final JBean b = it.next();
				try {
					b.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_RCMD_EXECUTE_CMD);
					b.send(JCmdTools.SERVER_RCMD_ALL);
				} catch (IOException e) {
					it.remove();b.clear();
				}
			}
		}

		String line;
		final Scanner reader = new Scanner(System.in);
		System.out.println("-+-All JBeans, Run "+EXIT_CMD_STR+" to exit.-+-");
		while ( true ) {
			JServerLang.RCMD_INPUT_ASK();
			line = reader.nextLine().trim().toLowerCase();
			if ( line.equals("") ) {
				System.out.println("enter the command, or run "+EXIT_CMD_STR+" to exit.");
				continue;
			} 
			
			/* exit the remote command execute thread */
			if ( line.equals(EXIT_CMD_STR) ) {
				synchronized (beanList) {
					final Iterator<JBean> _it = beanList.iterator();
					while ( _it.hasNext() ) {
						final JBean b = _it.next();
						try {
							b.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_TASK_STOP_CMD);
						} catch (IOException e) {
							_it.remove();
							b.clear();
						}
					}
				}
				break;
			}
			/* send the command to all the JBeans */
			else {
				_sendToAll(line);
			}
		}

		// Do not close the reader
		// reader.close();
	}
	
	
	private void _send(String cmd, int index) {
		final JBean bean = server.getBean(index);
		try {
			bean.send(JCmdTools.SEND_DATA_SYMBOL, cmd);

			/* get and print the execution response */
			final DataInputStream dis = bean.getReader();
			String line = dis.readUTF();
			if ( line.equals(JCmdTools.RCMD_NOREPLY_VAL) ) {
				System.out.println("execution failed or empty return");
			} else {
				System.out.println(line);
			}
		} catch (IOException e) {
			server.removeBean(index);
			CMD_SEND_ERROR("execute command");
		}
	}
	
	/**
	 * run command to the specified mathine 
	 */
	private void execute(int index) {
		final JBean bean = server.getBean(index);

		try {
			bean.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_RCMD_EXECUTE_CMD);
			bean.send(JCmdTools.SERVER_RMCD_SINGLE);
		} catch (IOException e) {
			CMD_SEND_ERROR("start command");
			return;
		}

		String line;
		final Scanner reader = new Scanner(System.in);
		System.out.println("-+-Single JBean, Run "+EXIT_CMD_STR+" to exit.-+-");
		while ( true ) {
			JServerLang.RCMD_INPUT_ASK();
			line = reader.nextLine().trim().toLowerCase();
			if ( line.equals("") ) {
				System.out.println("enter the command, or run "+EXIT_CMD_STR+" to exit.");
				continue;
			}
			
			/* exit the remote command execute thread */
			if ( line.equals(EXIT_CMD_STR) ) {
				try {
					bean.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_TASK_STOP_CMD);
				} catch (IOException e) {
					CMD_SEND_ERROR("stop command");
				}
				break;
			} else {
				_send(line, index);
			}
		}

		// Do not close the reader
		// reader.close();
	}
	
	public static void CMD_SEND_ERROR(String str) {
		System.out.println("Fail to send "+str);
	}
	
}
