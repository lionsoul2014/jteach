package org.lionsoul.jteach.client.task;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.lionsoul.jteach.client.JClient;
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.StringMessage;
import org.lionsoul.jteach.util.CmdUtil;


/**
 * remote command execute handler class
 * @author chenxin - chenxin619315@gmail.com
 */
public class RCRTask extends JCTaskBase {
	
	private static final Runtime run = Runtime.getRuntime();
	private static final Log log = Log.getLogger(RCRTask.class);

	private int cmd;

	public RCRTask(JClient client) {
		super(client);
	}

	@Override
	public boolean _before(String...args) {
		if (args.length > 0 && "single".equals(args[0])) {
			cmd = CmdUtil.COMMAND_RCMD_SINGLE_EXECUTION;
		} else {
			cmd = CmdUtil.COMMAND_RCMD_ALL_EXECUTION;
		}
		return true;
	}

	@Override
	public void _run() {
		while ( getStatus() == T_RUN ) {
			try {
				/* load a packet */
				final Packet p = bean.take();

				/* Command execute symbol */
				if (p.symbol == CmdUtil.SYMBOL_SEND_CMD) {
					if (p.cmd == CmdUtil.COMMAND_TASK_STOP) {
						break;
					}
					log.debug("Ignore command %d", p.cmd);
					continue;
				} else if (p.symbol != CmdUtil.SYMBOL_SEND_DATA) {
					log.debug("Ignore symbol %s\n", p.symbol);
					continue;
				}

				/*
				 * get the command string
				 * and execute the command
				 */
				final StringMessage msg;
				try {
					msg = StringMessage.decode(p);
				} catch (IOException e) {
					log.error("failed to decode string message due to %s", e.getClass());
					continue;
				}

				int counter = 0;
				final StringBuffer buff = new StringBuffer();
				final Process proc;
				try {
					log.debug("try to execute command '%s'", msg.str);
					proc = run.exec(msg.str);
					final BufferedInputStream in = new BufferedInputStream(proc.getInputStream());
					final BufferedReader br = new BufferedReader(new InputStreamReader(in));

					String line;
					while ( (line = br.readLine()) != null ) {
						buff.append(line+"\n");
						counter++;
					}

					in.close();
					br.close();
					proc.destroy();
				} catch (IOException e) {
					log.error("failed to exec command %s due to %s\n", msg.str, e.getClass().getName());
					counter = 1;
					buff.append(log.getError("failed to exec command %s due to %s", msg.str, e.getClass().getName()));
					// send the error message back if failed to exec the command
				}

				if (this.cmd == CmdUtil.COMMAND_RCMD_ALL_EXECUTION) {
					continue;
				}


				/*
				 * if there is no response
				 * send the empty tip
				 * else send the execution output
				 */
				try {
					if (counter == 0) {
						bean.send(Packet.valueOf(CmdUtil.RCMD_NOREPLY_VAL));
					} else {
						bean.send(Packet.valueOf(buff.toString()));
					}
				} catch (IOException e) {
					log.error("failed to decode the string with %s", e.getClass().getName());
				}
			} catch (IllegalAccessException e) {
				bean.reportClosedError();
				break;
			} catch (InterruptedException e) {
				log.warn("bean.take were interrupted");
			}
		}
	}

}