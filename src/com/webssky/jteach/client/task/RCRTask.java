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
 * {@link http://www.webssky.com} 
 */
public class RCRTask implements JCTaskInterface {
	
	private int TStatus = T_RUN;
	private DataInputStream reader = null;
	private JCWriter writer = null;
	private String type = null;
	private static Runtime run = Runtime.getRuntime();
	private Process p = null;
	
	public RCRTask() {}

	@Override
	public void run() {
		if ( reader == null ) return; 
		while ( getTStatus() == T_RUN ) {
			try {
				/**load symbol*/
				char symbol = reader.readChar();
				
				/**
				 * Command execute symbol 
				 */
				if ( symbol == JCmdTools.SEND_CMD_SYMBOL ) {
					int cmd = reader.readInt();
					if ( cmd == JCmdTools.SERVER_TASK_STOP_CMD ) break;
				}
				else if ( symbol != JCmdTools.SEND_DATA_SYMBOL ) continue;
				
				/**
				 * get the command string
				 * and execute the command 
				 */
				String command = reader.readUTF();
				p = run.exec(command); 
				BufferedInputStream in = new BufferedInputStream(p.getInputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				
				StringBuffer buff = new StringBuffer();
				String line = null;
				int counter = 0;
				while ( (line = br.readLine()) != null ) {
					buff.append(line+"\n");
					counter++;
				}
				in.close();
				br.close();
				p.destroy();
				
				/**
				 * if there is ne response
				 * send the empty tip
				 * else send the execute output 
				 */
				if ( counter == 0 ) writer.send(JCmdTools.RCMD_NOREPLY_VAL);
				else writer.send(buff.toString());
			} catch (IOException e) {
				JClient.getInstance().offLineClear();
				break;
			}
		}
		JClient.getInstance().resetJCTask();
		JClient.getInstance().notifyCmdMonitor();
		JClient.getInstance().setTipInfo("RCMD Execute Thread Is Overed!");
	}

	@Override
	public void startCTask(String...args) {
		reader = JClient.getInstance().getReader();
		if ( reader == null ) return;
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
