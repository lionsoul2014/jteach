package com.webssky.jteach.server.task;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
	private final List<JBean> beanList;

	public RCTask(JServer server) {
		this.server = server;
		beanList = Collections.synchronizedList(server.copyBeanList());
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
			// send start symbol
			final Iterator<JBean> it = beanList.iterator();
			while (it.hasNext()) {
				final JBean b = it.next();
				try {
					b.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_RCMD_EXECUTE_CMD);
					b.send(JCmdTools.SERVER_RCMD_ALL);
				} catch (IOException e) {
					it.remove();b.clear();
				}
			}

			executeToAll();
		} else {
			if (str.matches("^[0-9]{1,}$") == false) {
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

			try {
				final JBean bean = beanList.get(index);
				bean.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_RCMD_EXECUTE_CMD);
				bean.send(JCmdTools.SERVER_RMCD_SINGLE);
				/* send the command to specified bean */
				execute(bean);
			} catch (IOException e) {
				CMD_SEND_ERROR("start command");
			}
		}

		JServer.getInstance().resetJSTask();
		System.out.println("Remote Command execute thread is stoped.");
	}

	@Override
	public void stopTask() {
		
	}
	
	/**
	 * run command to all the online machine 
	 */
	private void executeToAll() {
		String line;
		final Scanner reader = new Scanner(System.in);
		System.out.println("-+-All JBeans, Run "+EXIT_CMD_STR+" to exit.-+-");

		while (true) {
			JServerLang.RCMD_INPUT_ASK();
			line = reader.nextLine().trim().toLowerCase();
			if ( line.equals("") ) {
				System.out.println("enter the command, or run "+EXIT_CMD_STR+" to exit.");
				continue;
			}

			boolean exit = false;
			if (line.equals(EXIT_CMD_STR)) {
				exit = true;
			}

			final Iterator<JBean> it = beanList.iterator();
			while ( it.hasNext() ) {
				final JBean b = it.next();
				try {
					if (exit) {
						b.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_TASK_STOP_CMD);
					} else {
						b.send(JCmdTools.SEND_DATA_SYMBOL, line);
					}
				} catch (IOException e) {
					it.remove();b.clear();
				}
			}

			// check and exit the current task
			if (exit) {
				break;
			}
		}
	}
	
	
	/**
	 * run command to the specified mathine 
	 */
	private void execute(final JBean bean) throws IOException {
		String line;
		final Scanner reader = new Scanner(System.in);
		System.out.println("-+-Single JBean, Run "+EXIT_CMD_STR+" to exit.-+-");

		while ( true ) {
			JServerLang.RCMD_INPUT_ASK();
			line = reader.nextLine().trim().toLowerCase();
			if (line.equals("")) {
				System.out.println("enter the command, or run "+EXIT_CMD_STR+" to exit.");
				continue;
			}
			
			/* exit the remote command execute thread */
			if (line.equals(EXIT_CMD_STR)) {
				bean.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_TASK_STOP_CMD);
				break;
			} else {
				bean.send(JCmdTools.SEND_DATA_SYMBOL, line);

				/* get and print the execution response */
				final DataInputStream dis = bean.getReader();
				final String resLine = dis.readUTF();
				if (resLine.equals(JCmdTools.RCMD_NOREPLY_VAL)) {
					System.out.println("execution failed or empty return");
				} else {
					System.out.println(resLine);
				}
			}
		}
	}
	
	public static void CMD_SEND_ERROR(String str) {
		System.out.println("Fail to send "+str);
	}
	
}