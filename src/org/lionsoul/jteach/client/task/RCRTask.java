package org.lionsoul.jteach.client.task;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.lionsoul.jteach.client.JClient;
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.util.JCmdTools;


/**
 * remote command execute handler class
 * @author chenxin - chenxin619315@gmail.com
 */
public class RCRTask implements JCTaskInterface {
	
	private int TStatus = T_RUN;
	private static final Runtime run = Runtime.getRuntime();
	private static final Log log = Log.getLogger(RCRTask.class);

	private int cmd;
	private final JClient client;
	private final JBean bean;

	public RCRTask(JClient client) {
		this.client = client;
		this.bean = client.getBean();
	}

	@Override
	public void startCTask(String...args) {
		if (args.length > 0 && "single".equals(args[0])) {
			cmd = JCmdTools.COMMAND_RCMD_SINGLE_EXECUTION;
		} else {
			cmd = JCmdTools.COMMAND_RCMD_ALL_EXECUTION;
		}

		client.setTipInfo("RCMD Execute Thread Is Working.");
		JBean.threadPool.execute(this);
	}

	@Override
	public void stopCTask() {
		setTStatus(T_STOP);
	}

	@Override
	public void run() {
		while ( getTStatus() == T_RUN ) {
			try {
				/* load a packet */
				final Packet p = bean.take();

				/* Command execute symbol */
				if (p.symbol == JCmdTools.SYMBOL_SEND_CMD) {
					if (p.cmd == JCmdTools.COMMAND_TASK_STOP) {
						break;
					}
					log.debug("Ignore command %d", p.cmd);
					continue;
				} else if (p.symbol != JCmdTools.SYMBOL_SEND_DATA) {
					log.debug("Ignore symbol %s\n", p.symbol);
					continue;
				}

				/*
				 * get the command string
				 * and execute the command
				 */
				String command = String.valueOf(p.data);
				int counter = 0;
				final StringBuffer buff = new StringBuffer();
				final Process proc;
				try {
					proc = run.exec(command);
					final BufferedInputStream in = new BufferedInputStream(proc.getInputStream());
					final BufferedReader br = new BufferedReader(new InputStreamReader(in));

					String line;
					while ( (line = br.readLine()) != null ) {
						buff.append(line+"\n");
						counter++;
					}

					in.close();
					br.close();
				} catch (IOException e) {
					log.error("failed to exec command %s\n", command);
					continue;
				}

				proc.destroy();
				if (this.cmd == JCmdTools.COMMAND_RCMD_ALL_EXECUTION) {
					continue;
				}


				/*
				 * if there is no response
				 * send the empty tip
				 * else send the execution output
				 */
				try {
					if (counter == 0) {
						bean.send(Packet.valueOf(JCmdTools.RCMD_NOREPLY_VAL));
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

		client.resetJCTask();
		client.notifyCmdMonitor();
		client.setTipInfo("RCMD Execute Thread Is Overed!");
	}

	private synchronized void setTStatus( int t ) {
		TStatus = t;
	}
	
	private synchronized int getTStatus() {
		return TStatus;
	}

}
