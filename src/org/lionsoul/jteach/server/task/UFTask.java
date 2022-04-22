package org.lionsoul.jteach.server.task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFileChooser;

import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.FileInfoMessage;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.server.JServer;


/**
 * list all the online JBeans
 * @author chenxin - chenxin619315@gmail.com
 */
public class UFTask extends JSTaskBase {
	
	public static final Log log = Log.getLogger(UFTask.class);
	public static final int POINT_LENGTH = 60;

	private File file;
	private Packet p;
	private BufferedInputStream bis;

	public UFTask(JServer server) {
		super(server);
	}

	@Override public boolean _before(List<JBean> beans) {
		final JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(false);
		final int _result = chooser.showOpenDialog(null);
		if ( _result != JFileChooser.APPROVE_OPTION ) {
			return false;
		}

		// create the input stream
		file = chooser.getSelectedFile();
		try {
			bis = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			server.println("task aborted due to %s: %s", e.getClass().getName(), e.getMessage());
			return false;
		}

		try {
			p = new FileInfoMessage(file.length(), file.getName()).encode();
		} catch (IOException e) {
			server.println(log.getError("failed to encode the file info message {%s, %d}",
					file.getName(), file.length()));
			return false;
		}

		return true;
	}

	@Override public void _run() {
		/* inform all the beans the information of the file name and size */
		synchronized (beanList) {
			final Iterator<JBean> it = beanList.iterator();
			while (it.hasNext()) {
				final JBean b = it.next();
				try {
					/* if failed to offer the file info packet
					* remove the bean from the global task bean list */
					if (!b.offer(p)) {
						it.remove();
					}
				} catch (IllegalAccessException e) {
					server.println(b.getClosedError());
					it.remove();
				}
			}
		}

		/* start to send the file content */
		server.println("File Information:");
		server.println("-+---name: %s", file.getName());
		server.println("-+---size: %dKiB - %d", file.length()/1024, file.length());
		server.println("sending file %s ... ", file.getAbsolutePath());

		try {
			/*
			 * read b.length byte from the buffer InputStream
			 * then send the byte[] to all the JBeans
			 * till all the bytes were sent out
			 */
			double readLen = 0;
			int counter = 0, len = 0;
			byte b[] = new byte[1024 * 64];
			while ( (len = bis.read(b, 0, b.length)) > 0 ) {
				readLen += len;
				counter++;

				final Packet dp = Packet.valueOf(b, 0, len);

				// do the chunked data send
				boolean checkSize = false;
				synchronized (beanList) {
					final Iterator<JBean> it = beanList.iterator();
					while (it.hasNext()) {
						final JBean bean = it.next();
						try {
							bean.put(dp);
						} catch (IllegalAccessException e) {
							checkSize = true;
							server.println("client %s removed due to %s: %s",
									bean.getHost(), e.getClass().getName(), e.getMessage());
							it.remove();
						}
					}
				}

				// check the bean list size
				if (checkSize && beanList.size() == 0) {
					server.println("task is overed due to empty client list");
					break;
				}


				/* file transmission progress bar */
				if ( counter % POINT_LENGTH == 0 ) {
					server.println("%dKiB - %f%%", (int)(readLen/1024), readLen/ file.length()*100);
					counter = 0;
				} else if ( readLen == file.length() ) {
					server.println("%dKiB - 100%%", (int)(readLen/1024));
				} else {
					server.print(".");
				}

				if ( getStatus() != T_RUN ) {
					break;
				}
			}
			server.println("file send completed");
			bis.close();
		} catch (IOException e) {
			server.println(log.getError("aborted due to %s", e.getClass().getName()));
		}

		// check and close the buffer input stream
		if ( bis != null ) {
			try {
				bis.close();
			} catch (IOException ignored) {
			}
		}
	}
	
}
