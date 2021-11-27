package org.lionsoul.jteach.server.task;

import java.io.IOException;
import java.util.*;

import org.lionsoul.jteach.msg.CommandMessage;
import org.lionsoul.jteach.msg.CommandStringMessage;
import org.lionsoul.jteach.msg.Message;
import org.lionsoul.jteach.msg.StringDataMessage;
import org.lionsoul.jteach.server.JBean;
import org.lionsoul.jteach.server.JServer;
import org.lionsoul.jteach.util.JCmdTools;
import org.lionsoul.jteach.util.JServerLang;


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
		// ignore the new client bean
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
				final JBean bean = it.next();
				try {
					bean.offer(new CommandStringMessage(
							JCmdTools.SERVER_RCMD_EXECUTE_CMD, JCmdTools.SERVER_RCMD_ALL));
				} catch (IllegalAccessException e) {
					bean.reportClosedError();
					it.remove();
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
				bean.offer(new CommandStringMessage(JCmdTools.SERVER_RCMD_EXECUTE_CMD, JCmdTools.SERVER_RMCD_SINGLE));
				execute(bean);
			} catch (IOException | IllegalAccessException e) {
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
		System.out.printf("-+-All JBeans, Run %s to exit.-+-\n", EXIT_CMD_STR);

		while (true) {
			JServerLang.RCMD_INPUT_ASK();
			line = reader.nextLine().trim().toLowerCase();
			if ( line.equals("") ) {
				System.out.printf("type the command, or run %s to exit.\n", EXIT_CMD_STR);
				continue;
			}

			if (beanList.size() == 0) {
				System.out.printf("Task %s overed due to empty clients\n", this.getClass().getName());
				break;
			}

			final boolean exit;
			final Message msg;
			if (line.equals(EXIT_CMD_STR)) {
				exit = true;
				msg = new CommandMessage(JCmdTools.SERVER_TASK_STOP_CMD);
			} else {
				exit = false;
				msg = new StringDataMessage(line);
			}

			synchronized (beanList) {
				final Iterator<JBean> it = beanList.iterator();
				while ( it.hasNext() ) {
					final JBean bean = it.next();
					try {
						bean.offer(msg);
					} catch (IllegalAccessException e) {
						bean.reportClosedError();
						it.remove();
					}
				}
			}

			// check and exit the current task
			if (exit) {
				break;
			}
		}
	}
	
	
	/**
	 * run command to the specified client
	 */
	private void execute(final JBean bean) throws IOException, IllegalAccessException {
		String line;
		final Scanner reader = new Scanner(System.in);
		System.out.printf("-+-Single JBean, Run %s to exit.-+-\n", EXIT_CMD_STR);

		while ( true ) {
			JServerLang.RCMD_INPUT_ASK();
			line = reader.nextLine().trim().toLowerCase();
			if (line.equals("")) {
				System.out.printf("type the command, or run %s to exit.\n", EXIT_CMD_STR);
				continue;
			}
			
			/* exit the remote command execute thread */
			if (line.equals(EXIT_CMD_STR)) {
				bean.offer(new CommandMessage(JCmdTools.SERVER_TASK_STOP_CMD));
				break;
			}

			bean.offer(new StringDataMessage(line));

			/* get and print the execution response */
			final Message msg = bean.poll();
			// final DataInputStream dis = bean.getReader();
			// final String resLine = dis.readUTF();
			// if (resLine.equals(JCmdTools.RCMD_NOREPLY_VAL)) {
			// 	System.out.println("execution failed or empty return");
			// } else {
			// 	System.out.println(resLine);
			// }
		}
	}
	
	public static void CMD_SEND_ERROR(String str) {
		System.out.println("Fail to send "+str);
	}
	
}