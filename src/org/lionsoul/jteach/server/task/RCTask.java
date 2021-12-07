package org.lionsoul.jteach.server.task;

import java.io.IOException;
import java.sql.Time;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

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
			server.println("-+-i : a/integer: send command to all/ith client");
			return false;
		}

		if (args.equals(CmdUtil.SERVER_RCMD_ALL)) {
			bean = null;
			// send start symbol to all beans
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
		} else {
			if (!args.matches("^[0-9]{1,}$")) {
				server.println("invalid index %s for rc command", args);
				return false;
			}

			int index = Integer.parseInt(args);
			if ( index < 0 || index >= server.beanCount() ) {
				server.println("index %d out of bounds", index);
				return false;
			}

			bean = beanList.get(index);

			try {
				bean.offer(Packet.COMMAND_RCMD_SINGLE_EXECUTE);
			} catch (IllegalAccessException e) {
				server.println("failed start command execution on bean %s", bean.getHost());
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean _wait() {
		return true;
	}

	@Override
	public void _run() {
		String line;
		final Scanner reader = new Scanner(System.in);
		server.println("-+-%s JBean, Run %s to exit.-+-",
				bean == null ? "All" : "Single", EXIT_CMD_STR);

		while ( true ) {
			server.print("JTeach#RC>> ");
			line = reader.nextLine().trim().toLowerCase();
			if (line.equals("")) {
				server.println("type the command, or run %s to exit.", EXIT_CMD_STR);
				continue;
			}

			/* exit the remote command execute thread */
			if (line.equals(EXIT_CMD_STR)) {
				break;
			}

			final Packet d;
			try {
				d = Packet.valueOf(line);
			} catch (IOException e) {
				server.println(log.getError("failed to encode StringMessage %s", line));
				continue;
			}

			if (bean == null) {
				if (beanList.size() == 0) {
					server.println(log.getDebug("task overed due to empty clients"));
					break;
				}

				synchronized (beanList) {
					final Iterator<JBean> it = beanList.iterator();
					while ( it.hasNext() ) {
						final JBean bean = it.next();
						try {
							bean.offer(d, JBean.DEFAULT_OFFER_TIMEOUT_SECS, TimeUnit.SECONDS);
						} catch (IllegalAccessException | InterruptedException e) {
							server.println("client %s removed due to %s: %s",
									bean.getHost(), e.getClass().getName(), e.getMessage());
							it.remove();
						}
					}
				}
			} else {
				/* get and print the execution response */
				final Packet p;
				try {
					bean.offer(d, JBean.DEFAULT_OFFER_TIMEOUT_SECS, TimeUnit.SECONDS);
					p = bean.poll(JBean.DEFAULT_POLL_TIMEOUT_SECS, TimeUnit.SECONDS);
				} catch (InterruptedException | IllegalAccessException e) {
					server.println(log.getWarn("client %s aborted due to %s: %s",
							bean.getName(), e.getClass().getName(), e.getMessage()));
					break;
				}

				final StringMessage msg;
				try {
					msg = StringMessage.decode(p);
				} catch (IOException e) {
					server.println(log.getError("failed to decode the string packet"));
					continue;
				}

				if (msg.str.equals(CmdUtil.RCMD_NOREPLY_VAL)) {
					server.println("execution failed or empty return");
				} else {
					server.println(msg.str);
				}
			}
		}
	}

}