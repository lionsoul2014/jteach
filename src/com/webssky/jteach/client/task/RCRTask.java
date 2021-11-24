package com.webssky.jteach.client.task;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.webssky.jteach.client.JCWriter;
import com.webssky.jteach.client.JClient;
import com.webssky.jteach.util.JCmdTools;


/**
 * remote command execute handler class
 * @author chenxin - chenxin619315@gmail.com
 */
public class RCRTask implements JCTaskInterface {
	
	private int TStatus = T_RUN;
	private DataInputStream reader = null;
	private JCWriter writer = null;
	private String type = null;
	private static Runtime run = Runtime.getRuntime();

	public RCRTask() {}

	@Override
	public void run() {
		if ( reader == null ) return; 
		while ( getTStatus() == T_RUN ) {
			try {
				/* load symbol */
				final char symbol = reader.readChar();
				
				/* Command execute symbol */
				if ( symbol == JCmdTools.SEND_CMD_SYMBOL ) {
					int cmd = reader.readInt();
					if ( cmd == JCmdTools.SERVER_TASK_STOP_CMD ) {
						break;
					}
				} else if ( symbol != JCmdTools.SEND_DATA_SYMBOL ) {
					continue;
				}
				
				/*
				 * get the command string
				 * and execute the command 
				 */
				String command = reader.readUTF();
				final Process p = run.exec(command);

				final BufferedInputStream in = new BufferedInputStream(p.getInputStream());
				final BufferedReader br = new BufferedReader(new InputStreamReader(in));

				int counter = 0;
				String line;
				final StringBuffer buff = new StringBuffer();
				while ( (line = br.readLine()) != null ) {
					buff.append(line+"\n");
					counter++;
				}

				in.close();
				br.close();
				p.destroy();

				/*
				 * if there is no response
				 * send the empty tip
				 * else send the execution output
				 */
				if ( counter == 0 ) {
					writer.send(JCmdTools.RCMD_NOREPLY_VAL);
				} else {
					writer.send(buff.toString());
				}
			} catch (IOException e) {
				// offline clean the client ONLY
				// IF the really send is failed.
				// in case is the IOException from the command execution
				try {
					writer.send(JCmdTools.RCMD_NOREPLY_VAL);
				} catch (IOException ex) {
					// ex.printStackTrace();
					JClient.getInstance().offLineClear();
					break;
				}
			}
		}

		JClient.getInstance().resetJCTask();
		JClient.getInstance().notifyCmdMonitor();
		JClient.getInstance().setTipInfo("RCMD Execute Thread Is Overed!");
	}

	@Override
	public void startCTask(String...args) {
		reader = JClient.getInstance().getReader();
		if ( reader == null ) {
			return;
		}

		try {
			type = reader.readUTF().trim().toLowerCase();
			if ( type.equals(JCmdTools.SERVER_RMCD_SINGLE) ) writer = new JCWriter(); 
		} catch (IOException e) {
			/*JOptionPane.showMessageDialog(null, "Fail To Load Data From Server",
					"JTeach", JOptionPane.INFORMATION_MESSAGE);*/
		}

		JClient.getInstance().setTipInfo("RCMD Execute Thread Is Working.");
		JClient.threadPool.execute(this);
	}

	@Override
	public void stopCTask() {
		setTStatus(T_STOP);
	}
	
	private synchronized void setTStatus( int t ) {
		TStatus = t;
	}
	
	private synchronized int getTStatus() {
		return TStatus;
	}

}
