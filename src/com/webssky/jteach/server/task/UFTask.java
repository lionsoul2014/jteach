package com.webssky.jteach.server.task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFileChooser;

import com.webssky.jteach.server.JBean;
import com.webssky.jteach.server.JServer;
import com.webssky.jteach.util.JCmdTools;
import com.webssky.jteach.util.JServerLang;


/**
 * list all the online JBeans
 * @author chenxin - chenxin619315@gmail.com
 */
public class UFTask implements JSTaskInterface,Runnable {
	
	public static final String BIS_CREATE_ERROR = "Unable to create FileInputStream.";
	public static final String FILE_READ_ERROR = "Fail to read byte from file.";
	public static final String FILE_TRASMIT_START = "File trasimit started";
	
	public static final String STOPING_TIP = "File Upload Thread Is Stoping...";
	public static final String STOPED_TIP = "File Upload Thread Is Stoped.";
	
	public static final int POINT_LENGTH = 60;
	private File file = null;
	private int TStatus = T_RUN;
	private final List<JBean> beans;
	
	public UFTask(JServer server) {
		beans = server.copyBeanList();
	}

	@Override
	public void startTask() {
		JServer.threadPool.execute(this);
	}

	@Override
	public void stopTask() {
		System.out.println(STOPING_TIP);
		setTSTATUS(T_STOP);

		/* send stop command to all the beans */
		final Iterator<JBean> it = beans.iterator();
		while ( it.hasNext() ) {
			JBean b = it.next();
			try {
				b.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_TASK_STOP_CMD);
			} catch (IOException e) {
				it.remove();
				b.clear();
			}
		}

		System.out.println(STOPED_TIP);
	}
	
	@Override
	public void run() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(false);
		int _result = chooser.showOpenDialog(null);
		if ( _result != JFileChooser.APPROVE_OPTION ) {
			JServer.getInstance().resetJSTask();
			return;
		}
		file = chooser.getSelectedFile();
		BufferedInputStream bis = null;
		try {
			
			/* inform all the beans the information of the file name and size */
			for ( int j = 0; j < beans.size(); j++ ) {
				JBean bean = beans.get(j);
				bean.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_UPLOAD_START_CMD);
				bean.send(file.getName(), file.length());
			}
			
			/* create a buffer InputStream */
			bis = new BufferedInputStream(new FileInputStream(file));
			int len = 0;
			float readLen = 0;
			System.out.println("File Informationme:");
			System.out.println("-+---name:"+file.getName());
			System.out.println("-+---size:"+file.length()/1024+"K - "+file.length());
			System.out.println(FILE_TRASMIT_START);
			DecimalFormat format = new DecimalFormat("0.00");

			/*
			 * read b.length byte from the buffer InputStream
			 * then send the byte[] to all the JBeans
			 * till all the byte is send 
			 */
			byte b[] = new byte[1024*JCmdTools.FILE_UPLOAD_ONCE_SIZE];
			int counter = 0;
			Iterator<JBean> it = null;
			while ( (len = bis.read(b, 0, b.length)) > 0 ) {
				readLen += len;
				counter++;
				it = beans.iterator();
				while ( it.hasNext() ) {
					JBean bean = it.next();
					try {
						bean.send(b, len);
					} catch (IOException e) {
						it.remove();bean.clear();
					}
				}
				/*file transmifer progress show */
				if ( counter % POINT_LENGTH == 0 ) {
					System.out.println( (int) (readLen/1024)+"K - "
							+format.format(readLen/file.length()*100)+"%");
					counter = 0;
				}
				else if ( readLen == file.length() ) 
					System.out.println( (int) (readLen/1024)+"K - 100%"); 
				else System.out.print(".");
				if ( getTSTATUS() != T_RUN ) break;
			}
			System.out.println("File transmit completed.");
			bis.close();
		} catch (FileNotFoundException e) {
			System.out.println(BIS_CREATE_ERROR);
		} catch (IOException e) {
			System.out.println(FILE_READ_ERROR);
		} finally {
			if ( bis != null ) {
				try {bis.close();} catch (IOException e) {}
			}
		}
		JServer.getInstance().resetJSTask();
		JServerLang.INPUT_ASK();
	}
	
	public synchronized int getTSTATUS() {
		return TStatus;
	}
	
	public synchronized void setTSTATUS(int s) {
		TStatus = s;
	}

}
