package org.lionsoul.jteach.server.task;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.msg.StringMessage;
import org.lionsoul.jteach.server.JServer;
import org.lionsoul.jteach.util.JCmdTools;
import org.lionsoul.jteach.util.JServerLang;


/**
 * thread for run command on online remote machine.
 *
 * @author chenxin - chenxin619315@gmail.com
 */
public class RCTask implements JSTaskInterface {
	
	public static final String EXIT_CMD_STR = ":exit";
	public static final Log log = Log.getLogger(SBTask.class);

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
	public boolean start() {
		String str = server.getArguments().get(JCmdTools.RCMD_EXECUTE_KEY);
		if ( str == null ) {
			JServerLang.RCMD_EXECUTE_EMPTY_ARGUMENTS();
			return false;
		}
		
		/* send command to all the JBeans */
		if ( str.equals(JCmdTools.RCMD_EXECUTE_VAL) ) {
			// send start symbol
			final Iterator<JBean> it = beanList.iterator();
			while (it.hasNext()) {
				final JBean bean = it.next();
				try {
					bean.offer(Packet.COMMAND_RCMD_ALL_EXECUTE);
				} catch (IllegalAccessException e) {
					bean.reportClosedError();
					it.remove();
				}
			}
			executeToAll();
		} else {
			if (str.matches("^[0-9]{1,}$") == false) {
				log.error("invalid index %s for rc command", str);
				return false;
			}

			int index = Integer.parseInt(str);
			if ( index < 0 || index >= server.beanCount() ) {
				log.error("index %d out of bounds", index);
				return false;
			}

			final JBean bean = beanList.get(index);
			try {
				bean.offer(Packet.COMMAND_RCMD_SINGLE_EXECUTE);
				execute(bean);
			} catch (IOException | IllegalAccessException e) {
				log.error("failed start command execution on bean %s", bean.getHost());
				return false;
			}
		}

		log.debug("remote command execute thread is stopped.");
		return true;
	}

	@Override
	public void stop() {
		
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
				log.debug("task is overed due to empty clients");
				break;
			}

			final boolean exit;
			final Packet p;
			if (line.equals(EXIT_CMD_STR)) {
				exit = true;
				p = Packet.COMMAND_TASK_STOP;
			} else {
				exit = false;
				try {
					p = Packet.valueOf(line);
				} catch (IOException e) {
					log.error("failed to encode StringMessage %s\n", line);
					continue;
				}
			}

			synchronized (beanList) {
				final Iterator<JBean> it = beanList.iterator();
				while ( it.hasNext() ) {
					final JBean bean = it.next();
					try {
						bean.offer(p);
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
				bean.offer(Packet.COMMAND_TASK_STOP);
				break;
			}

			bean.offer(Packet.valueOf(line));

			/* get and print the execution response */
			final Packet p;
			try {
				p = bean.take();
			} catch (InterruptedException e) {
				log.warn("client %s message take was interrupted", bean.getName());
				continue;
			}

			final StringMessage msg;
			try {
				msg = StringMessage.decode(p);
			} catch (IOException e) {
				log.error("failed to decode the string packet");
				continue;
			}

			if (msg.str.equals(JCmdTools.RCMD_NOREPLY_VAL)) {
				System.out.println("execution failed or empty return");
			} else {
				System.out.println(msg.str);
			}
		}
	}

}