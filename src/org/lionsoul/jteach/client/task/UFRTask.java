package org.lionsoul.jteach.client.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
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
	
	public static final String WTitle = "JTeach - FileUpload";
	public static final String InfoLabelText = "JTeach> Load File Info From Server.";
	public static final Dimension WSize = new Dimension(450, 80);
	private static final Log log = Log.getLogger(UFRTask.class);

	private final JFrame window;
	private final JLabel infoLabel;
	private final JProgressBar pBar;

	private FileInfoMessage file;
	private BufferedOutputStream bos;

	public UFRTask(JClient client) {
		super(client);
		this.window = new JFrame();
		this.infoLabel = new JLabel(InfoLabelText);
		this.pBar = new JProgressBar(0, 100);
		initGUI();
	}
	
	/** initialize the GUI */
	private void initGUI() {
		window.setTitle(WTitle);
		window.setAlwaysOnTop(true);
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setResizable(false);
		window.setSize(WSize);
		window.setLocationRelativeTo(null);
		window.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				stop();
			}
		});

		window.setLayout(new BorderLayout());
		final Container c = window.getContentPane();
		infoLabel.setSize(WSize);
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
		final FileSystemView fsv = FileSystemView.getFileSystemView();
		final Packet p;
		try {
			p = bean.take();
		} catch (InterruptedException ex) {
			log.warn("client %s take was interrupted", bean.getHost());
			return false;
		} catch (IllegalAccessException ex) {
			log.error(bean.getClosedError());
			return false;
		}

		try {
			file = FileInfoMessage.decode(p);
		} catch (IOException e) {
			log.error("failed to decode the file info message");
			return false;
		}

		setTipInfo("Receiving file " + file.name);
		log.debug("file: %s, size: %dKiB", file.name, (file.length / 1024));
		try {
			bos = new BufferedOutputStream(new FileOutputStream(
					fsv.getHomeDirectory() + "/" + file.name));
		} catch (FileNotFoundException e) {
			log.error("failed to create output %s: %s", e.getClass().getName(), e.getMessage());
			return false;
		}

		SwingUtilities.invokeLater(() -> {
			window.setVisible(true);
			window.requestFocus();
		});

		return true;
	}

	@Override
	public void _run() {
		try {
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
		} catch (IllegalAccessException e) {
			log.error(bean.getClosedError());
		} catch (InterruptedException e) {
			log.warn("aborted due to interrupted: %s", e.getMessage());
		} catch (IOException e) {
			log.error("aborted due to %s: %s", e.getClass().getName(), e.getMessage());
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