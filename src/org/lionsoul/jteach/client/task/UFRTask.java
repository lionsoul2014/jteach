package org.lionsoul.jteach.client.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
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
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.FileInfoMessage;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.util.JClientCfg;
import org.lionsoul.jteach.util.CmdUtil;


/**
 * File Receive When Server started the File Upload Thread.
 * 
 * @author  chenxin<chenxin619315@gmail.com>
 */
public class UFRTask extends JCTaskBase {
	
	public static final String W_TILTE = "JTeach - FileUpload";
	public static final String INFO_LABEL_TEXT = "JTeach> Load File Info From Server.";
	public static final Dimension W_SIZE = new Dimension(450, 80);
	private static final Log log = Log.getLogger(UFRTask.class);
	
	private final JFrame window;
	private final JLabel infoLabel;
	private final JProgressBar pBar;

	public UFRTask(JClient client) {
		super(client);
		this.window = new JFrame();
		this.infoLabel = new JLabel(INFO_LABEL_TEXT);
		this.pBar = new JProgressBar(0, 100);
		initGUI();
	}
	
	/** initialize the GUI */
	private void initGUI() {
		window.setTitle(W_TILTE);
		window.setAlwaysOnTop(true);
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setResizable(false);
		window.setSize(W_SIZE);
		window.setLocationRelativeTo(null);
		window.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				stop();
			}
		});

		window.setLayout(new BorderLayout());
		final Container c = window.getContentPane();
		infoLabel.setSize(W_SIZE);
		infoLabel.setOpaque(true);
		infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
		infoLabel.setBounds(0, 5, window.getWidth(), 30);
		infoLabel.setBackground(JClientCfg.TIP_BG_COLOR);
		infoLabel.setForeground(JClientCfg.TIP_FRON_COLOR);
		c.add(infoLabel, BorderLayout.CENTER);
		
		pBar.setBorder(new LineBorder(Color.ORANGE, 1));
		pBar.setBackground(Color.WHITE);
		pBar.setStringPainted(true);
		c.add(pBar, BorderLayout.SOUTH);
	}
	
	/** set the value of ProgressBar pBar */
	private void setBarValue(final int v) {
		SwingUtilities.invokeLater(() -> pBar.setValue(v));
	}
	
	private void setTipInfo(final String str) {
		SwingUtilities.invokeLater(() -> infoLabel.setText(str));
	}

	@Override
	public boolean _before(String...args) {
		SwingUtilities.invokeLater(() -> {
			window.setVisible(true);
			window.requestFocus();
		});
		return true;
	}

	@Override
	public void _run() {
		FileSystemView fsv = FileSystemView.getFileSystemView();
		try {
			final Packet p = bean.take();
			final FileInfoMessage file;
			try {
				file = FileInfoMessage.decode(p);
			} catch (IOException e) {
				log.error("failed to decode the file info message");
				stop();
				return;
			}

			setTipInfo("Receiving file " + file.name);
			log.debug("file: %s, size: %dKiB", file.name, (file.length / 1024));
			final BufferedOutputStream bos = new BufferedOutputStream(
					new FileOutputStream(fsv.getHomeDirectory() + "/" + file.name));

			
			/* byte array
			 * get the byte from socket and store them in byte array b
			 * then put them in bos BufferedOutputStream for to save them in file */
			long readLen = 0;
			while (readLen < file.length) {
				/* check the running status */
				if (getStatus() == T_STOP) {
					log.debug("task stop");
					break;
				}

				/* load data packet */
				final Packet cp = bean.take();
				if (!cp.isSymbol(CmdUtil.SYMBOL_SEND_DATA)) {
					log.debug("Ignore symbol %s", cp.symbol);
					continue;
				}

				bos.write(cp.input, 0, cp.length);
				readLen += cp.length;

				//bos.flush();
				setBarValue((int)((readLen/(double)file.length) * 100));
			}

			bos.flush();
			bos.close();
		} catch (IOException e) {
			log.error("task is overed due to %s", e.getClass().getName());
		} catch (IllegalAccessException e) {
			log.error(bean.getClosedError());
		} catch (InterruptedException e) {
			log.warn("bean.take was interrupted");
		}
	}

	@Override
	public void _exit() {
		SwingUtilities.invokeLater(() -> {
			window.setVisible(false);
			window.dispose();
		});
		super._exit();
	}

}