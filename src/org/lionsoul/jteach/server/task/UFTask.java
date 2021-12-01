package org.lionsoul.jteach.server.task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;

import javax.swing.JFileChooser;

import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.FileInfoMessage;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.server.JServer;
import org.lionsoul.jteach.util.CmdUtil;


/**
 * list all the online JBeans
 * @author chenxin - chenxin619315@gmail.com
 */
public class UFTask extends JSTaskBase {
	
	public static final String BIS_CREATE_ERROR = "Unable to create FileInputStream.";
	public static final String FILE_READ_ERROR = "Fail to read byte from file.";
	public static final String FILE_TRASMIT_START = "File trasimit started";
	
	public static final String STOPING_TIP = "File Upload Thread Is Stoping...";
	public static final String STOPED_TIP = "File Upload Thread Is Stoped.";
	public static final Log log = Log.getLogger(UFTask.class);
	
	public static final int POINT_LENGTH = 60;

	public UFTask(JServer server) {
		super(server);
	}

	@Override
	public void addClient(JBean bean) {
		// Ignore the new client bean
	}

	@Override
	public boolean start() {
		if (beanList.size() == 0) {
			log.debug("task abort due to empty client list");
			return false;
		}

		JBean.threadPool.execute(this);
		return true;
	}

	@Override
	public void stop() {
		System.out.println(STOPING_TIP);
		setStatus(T_STOP);

		/* send stop command to all the beans */
		final Iterator<JBean> it = beanList.iterator();
		while (it.hasNext()) {
			final JBean b = it.next();
			try {
				b.offer(Packet.COMMAND_TASK_STOP);
			} catch (IllegalAccessException e) {
				b.reportClosedError();
				it.remove();
			}
		}

		System.out.println(STOPED_TIP);
	}

	@Override
	public void run() {
		final JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(false);
		final int _result = chooser.showOpenDialog(null);
		if ( _result != JFileChooser.APPROVE_OPTION ) {
			server.stopJSTask();
			return;
		}

		// create the input stream
		final File file = chooser.getSelectedFile();
		final BufferedInputStream bis;
		try {
			bis = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			return;
		}

		final Packet p;
		try {
			p = new FileInfoMessage(file.length(), file.getName()).encode();
		} catch (IOException e) {
			log.error("failed to encode the file info message {%s, %d}", file.getName(), file.length());
			return;
		}

		/* inform all the beans the information of the file name and size */
		synchronized (beanList) {
			final Iterator<JBean> it = beanList.iterator();
			while (it.hasNext()) {
				final JBean b = it.next();
				try {
					b.offer(Packet.COMMAND_UPLOAD_START);
					b.offer(p);
				} catch (IllegalAccessException e) {
					b.reportClosedError();
					it.remove();
				}
			}
		}

		try {
			/* create a buffer InputStream */
			System.out.println("File Information:");
			System.out.println("-+---name:"+file.getName());
			System.out.println("-+---size:"+file.length()/1024+"K - "+file.length());
			System.out.println(FILE_TRASMIT_START);
			DecimalFormat format = new DecimalFormat("0.00");

			/*
			 * read b.length byte from the buffer InputStream
			 * then send the byte[] to all the JBeans
			 * till all the bytes were sent out
			 */
			float readLen = 0;
			int counter = 0, len = 0;
			byte b[] = new byte[1024* CmdUtil.FILE_UPLOAD_ONCE_SIZE];
			while ( (len = bis.read(b, 0, b.length)) > 0 ) {
				readLen += len;
				counter++;

				// do the chunked data send
				boolean checkSize = false;
				synchronized (beanList) {
					final Iterator<JBean> it = beanList.iterator();
					while (it.hasNext()) {
						final JBean bean = it.next();
						try {
							bean.offer(Packet.valueOf(b));
						} catch (IllegalAccessException e) {
							checkSize = true;
							bean.reportClosedError();
							it.remove();
						}
					}
				}

				// check the bean list size
				if (checkSize && beanList.size() == 0) {
					log.debug("task is overed due to empty client list");
					break;
				}


				/* file transmission progress bar */
				if ( counter % POINT_LENGTH == 0 ) {
					System.out.println( (int) (readLen/1024)+"K - "
							+format.format(readLen/file.length()*100)+"%");
					counter = 0;
				} else if ( readLen == file.length() ) {
					System.out.println((int) (readLen / 1024) + "K - 100%");
				} else {
					System.out.print(".");
				}

				if ( getStatus() != T_RUN ) {
					break;
				}
			}
			log.info("file transfer completed.");
			bis.close();
		} catch (FileNotFoundException e) {
			System.out.println(BIS_CREATE_ERROR);
		} catch (IOException e) {
			System.out.println(FILE_READ_ERROR);
		}

		// check and close the buffer input stream
		if ( bis != null ) {
			try {
				bis.close();
			} catch (IOException e) {
			}
		}

		server.stopJSTask();
		JServer.printInputAsk();
	}
	
}
