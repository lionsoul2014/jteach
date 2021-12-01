package org.lionsoul.jteach.server.task;

import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;

import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.msg.StringMessage;
import org.lionsoul.jteach.server.JServer;
import org.lionsoul.jteach.util.CmdUtil;


/**
 * thread for run command on online remote machine.
 *
 * @author chenxin - chenxin619315@gmail.com
 */
public class RCTask extends JSTaskBase {
	
	public static final String EXIT_CMD_STR = ":exit";
	public static final Log log = Log.getLogger(SBTask.class);

	public RCTask(JServer server) {
		super(server);
	}

	@Override
	public void addClient(JBean bean) {
		// ignore the new client bean
	}

	@Override
	public boolean start() {
		String str = server.getArguments().get(CmdUtil.RCMD_EXECUTE_KEY);
		if ( str == null ) {
			System.out.println("-+-i : a/integer: send command to all/ith client");
			return false;
		}
		
		/* send command to all the JBeans */
		if ( str.equals(CmdUtil.RCMD_EXECUTE_VAL) ) {
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

		// return false to notify to stop the JSTask
		return true;
	}

	@Override
	public void stop() {}
	
	/**
	 * run command to all the online machine 
	 */
	private void executeToAll() {
		String line;
		final Scanner reader = new Scanner(System.in);
		System.out.printf("-+-All JBeans, Run %s to exit.-+-\n", EXIT_CMD_STR);

		while (true) {
			printInputAsk();
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
			printInputAsk();
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

			if (msg.str.equals(CmdUtil.RCMD_NOREPLY_VAL)) {
				System.out.println("execution failed or empty return");
			} else {
				System.out.println(msg.str);
			}
		}
	}

	public static final void printInputAsk() {
		System.out.print("JTeach#RC>> ");
	}

	@Override
	public void run() {

	}
}