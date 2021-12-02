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

	private JBean bean;
	public RCTask(JServer server) {
		super(server);
	}

	@Override
	public boolean _before() {
		final String args = server.getArguments().get(CmdUtil.RCMD_EXECUTE_KEY);
		if ( args == null ) {
			System.out.println("-+-i : a/integer: send command to all/ith client");
			return false;
		}

		if (args.equals(CmdUtil.COMMAND_RCMD_ALL_EXECUTION)) {
			bean = null;
		} else {
			if (args.matches("^[0-9]{1,}$") == false) {
				JServer.setTipInfo("invalid index %s for rc command", args);
				return false;
			}

			int index = Integer.parseInt(args);
			if ( index < 0 || index >= server.beanCount() ) {
				JServer.setTipInfo("index %d out of bounds", index);
				return false;
			}

			bean = beanList.get(index);
		}

		return true;
	}

	@Override
	public void _run() {

		/* send command to all the JBeans */
		if (bean == null) {
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
			try {
				bean.offer(Packet.COMMAND_RCMD_SINGLE_EXECUTE);
				execute(bean);
			} catch (IOException | IllegalAccessException e) {
				JServer.setTipInfo("failed start command execution on bean %s", bean.getHost());
				return;
			}
		}
	}

	@Override
	public void stop() {
		// do nothing here
	}
	
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

}