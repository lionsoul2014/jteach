package org.lionsoul.jteach.server.task;

import java.io.IOException;
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
	public boolean _wait() {
		return true;
	}

	@Override
	public void _run() {
		String line;
		final Scanner reader = new Scanner(System.in);
		server.println("-+-%s JBean, Run %s to exit.-+-",
				beanList.size() > 1 ? "All" : "Single", EXIT_CMD_STR);

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


			if (beanList.size() == 0) {
				server.println(log.getDebug("task overed due to empty clients"));
				break;
			}

			final int okCount = send(d);

			/* get the execution response only if there is one client */
			if (beanList.size() == 1 && okCount == 1) {
				final JBean bean = beanList.get(0);
				final Packet p;
				try {
					p = bean.take();
				} catch (InterruptedException | IllegalAccessException e) {
					beanList.remove(0);
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