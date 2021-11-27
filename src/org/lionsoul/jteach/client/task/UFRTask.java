package org.lionsoul.jteach.client.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileSystemView;

import org.lionsoul.jteach.client.JClient;
import org.lionsoul.jteach.util.JClientCfg;
import org.lionsoul.jteach.util.JCmdTools;


/**
 * File Receive When Server started the File Upload Thread. <br />
 * 
 * @author chenxin <br />
 */
public class UFRTask extends JFrame implements JCTaskInterface {
	
	private static final long serialVersionUID = 1L;
	public static final String W_TILTE = "JTeach - FileUpload";
	public static final String INFO_LABEL_TEXT = "JTeach> Load File Info From Server.";
	public static final Dimension W_SIZE = new Dimension(450, 80);
	
	public static final int P_MIN = 0;
	public static final int P_MAX = 100;
	
	//private int sTatus = T_RUN;
	private JLabel infoLabel = null;
	private JProgressBar pBar = null;
	private Thread tThread = null;

	public UFRTask() {
		this.setTitle(W_TILTE);
		this.setAlwaysOnTop(true);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				stopCTask();
			}
		});
		this.setResizable(false);
		this.setSize(W_SIZE);
		initGUI();
		this.setLocationRelativeTo(null);
	}
	
	/**
	 * initialize the GUI 
	 */
	private void initGUI() {
		setLayout(new BorderLayout());
		Container c = getContentPane();
		infoLabel = new JLabel(INFO_LABEL_TEXT);
		infoLabel.setOpaque(true);
		infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
		infoLabel.setBounds(0, 5, getWidth(), 30);
		infoLabel.setBackground(JClientCfg.TIP_BG_COLOR);
		infoLabel.setForeground(JClientCfg.TIP_FRON_COLOR);
		c.add(infoLabel, BorderLayout.CENTER);
		
		pBar = new JProgressBar(P_MIN, P_MAX);
		pBar.setBorder(new LineBorder(Color.ORANGE, 1));
		pBar.setBackground(Color.WHITE);
		pBar.setStringPainted(true);
		c.add(pBar, BorderLayout.SOUTH);
	}
	
	/**
	 * set the value of ProgressBar pBar 
	 */
	private void setBarValue(final int v) {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				pBar.setValue(v);
			}
		});
	}
	
	private void setTipInfo(final String str) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				infoLabel.setText(str);
			}
		});
	}

	@Override
	public void startCTask(String...args) {
		JClient.getInstance().setTipInfo("File Receive Thread Is Working.");
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setVisible(true);
				requestFocus();
			}
		});
		//JClient.threadPool.execute(this);
		tThread = new Thread(this);
		tThread.start();
	}

	@Override
	public void stopCTask() {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				setVisible(false);
				dispose();
			}
		});
		/**
		 * when client mean to exit the file receive thread
		 * the byte[] load thread must be over at the same time
		 * tThread.interrupt could finish this 
		 */
		if ( tThread != null ) tThread.interrupt(); 
		JClient.getInstance().resetJCTask();
		JClient.getInstance().notifyCmdMonitor();
		JClient.getInstance().setTipInfo("File Receive Thread Is Overed!");
	}

	@Override
	public void run() {
		FileSystemView fsv = FileSystemView.getFileSystemView();
		DataInputStream reader = JClient.getInstance().getReader();
		if ( reader == null ) return;
		BufferedOutputStream bos = null;
		try {
			/**
			 * get the name of the file
			 * and the total byte of the file  
			 */
			String fileName = reader.readUTF();
			long filesize = reader.readLong();
			
			setTipInfo("File:"+fileName+", size:"+filesize/1024+"K");
			bos = new BufferedOutputStream(new FileOutputStream(fsv.getHomeDirectory()+"/"+fileName));
			
			/**
			 * byte array
			 * get the byte from socket and store them in byte array b
			 * then put them in bos BufferedOuputStream for save them in file
			 */
			final byte b[] = new byte[1024*JCmdTools.FILE_UPLOAD_ONCE_SIZE];
			long readLen = 0;
			long limiter = 0;
			while ( readLen < filesize ) {
				/**
				 * could exit the while throught thread.interrupt) 
				 */
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					break;
				}
				/**
				 * load full of byte[] b 
				 */
				int byteLoad = 0;
				if ( readLen + b.length < filesize ) limiter = b.length;
				else limiter = filesize - readLen;
				while ( byteLoad < limiter ) {
					int rSize = reader.read(b, byteLoad, b.length - byteLoad);
					if ( rSize > 0 )  byteLoad += rSize;
					else break;
				}
				readLen += byteLoad;
				bos.write(b, 0, byteLoad);
				//bos.flush();
				setBarValue((int) (readLen * P_MAX / filesize));
			}
			bos.flush();
			bos.close();
			tThread = null;
		} catch (IOException e) {
			try {
				if ( bos != null ) bos.close();
			} catch (IOException e1) {}
			JClient.getInstance().offLineClear();
		}
		setTipInfo("File receive thread was overed.");
		stopCTask();
	}
	
	/*public static void main(String args[]) {
		new UFRTask().startCTask();
		//FileSystemView fsv = FileSystemView.getFileSystemView();
		//System.out.println(fsv.getDefaultDirectory().getAbsolutePath());
		//System.getProperties().list(System.out);
	}*/

}
