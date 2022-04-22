package org.lionsoul.jteach.server.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.lionsoul.jteach.capture.ScreenCapture;
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.ScreenMessage;
import org.lionsoul.jteach.server.JServer;
import org.lionsoul.jteach.util.CmdUtil;
import org.lionsoul.jteach.util.ImageUtil;


/**
 * Image receive thread for Screen monitor
 * @author chenxin - chenxin619315@gmail.com
 */
public class SMTask extends JSTaskBase {

	public static final String WTitle = "JTeach - Remote Window";
	public static final String EmptyInfo = "Loading Image Resource From JBean";
	public static final Font IFont = new Font("Arial", Font.BOLD, 18);
	public static final Image CURSOR = ImageUtil.Create("cursor_02.png").getImage();
	public static final Log log = Log.getLogger(UFTask.class);

	private final JFrame window;
	private final ImageJPanel mapJPanel;
	private final Dimension screenSize;
	private final Insets insetSize;
	private volatile ScreenMessage screen = null;

	public SMTask(JServer server) {
		super(server);
		this.window = new JFrame();
		this.mapJPanel = new ImageJPanel();
		this.screenSize = window.getToolkit().getScreenSize();
		this.insetSize  = window.getToolkit().getScreenInsets(window.getGraphicsConfiguration());
		initGUI();
	}

	private void initGUI() {
		window.setTitle(WTitle);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.setSize(screenSize);
		window.setUndecorated(true);
		window.setLayout(new BorderLayout());
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setResizable(false);
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				/* stop the JCTask and dispose the window */
				//stopCTask();
				//_dispose();
			}
		});
		window.getContentPane().add(mapJPanel, BorderLayout.CENTER);
	}

	private void repaintImageJPanel() {
		SwingUtilities.invokeLater(() -> mapJPanel.repaint());
	}

	@Override public boolean _before(List<JBean> beans) {
		SwingUtilities.invokeLater(() -> {
			window.setVisible(true);
			window.requestFocus();
		});
		return true;
	}
	
	@Override
	public void _run() {
		while ( getStatus() == T_RUN ) {
			try {
				/* load symbol */
				final Packet p = beanList.get(0).take();

				/* Check the symbol type */
				if (p.symbol == CmdUtil.SYMBOL_SEND_CMD) {
					if (p.cmd == CmdUtil.COMMAND_TASK_STOP) {
						log.debug("task is overed by stop command");
						break;
					}
					log.debug("Ignore command %d", p.cmd);
					continue;
				} else if (p.symbol != CmdUtil.SYMBOL_SEND_DATA) {
					log.debug("Ignore symbol %s", p.symbol);
					continue;
				}

				/* decode the packet to the ScreenMessage */
				try {
					screen = ScreenMessage.decode(p);
				} catch (IOException e) {
					log.error("failed to decode the screen message");
					continue;
				}

				/* repaint the Image JPanel */
				repaintImageJPanel();
			} catch (InterruptedException | IllegalAccessException e) {
				server.println(log.getError("client %s aborted due to %s: %s",
						beanList.get(0).getName(), e.getClass().getName(), e.getMessage()));
				break;
			}
		}
	}

	/** screen image show JPanel */
	private class ImageJPanel extends JPanel {

		public ImageJPanel() {
			setFocusable(true);
			setFocusTraversalKeysEnabled(false);
		}

		@Override
		public void update(Graphics g) {
			paintComponent(g);
		}

		@Override
		protected void paintComponent(Graphics g) {
			final ScreenMessage msg = screen;
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			if (msg == null) {
				g.setColor(Color.WHITE);
				g.setFont(IFont);
				final FontMetrics m = getFontMetrics(IFont);
				g.drawString(EmptyInfo, (getWidth() - m.stringWidth(EmptyInfo))/2, getHeight()/2);
				return;
			}

			/* draw the screen image */
			final int dst_w = getWidth() - insetSize.left - insetSize.right;
			final int dst_h = getHeight() - insetSize.top - insetSize.bottom;
			final BufferedImage img = ImageUtil.resize_2(msg.img, dst_w, dst_h);
			g.drawImage(img, 0, 0, null);

			/* draw the mouse */
			if (msg.driver == ScreenCapture.ROBOT_DRIVER) {
				final int x = Math.round(msg.mouse.x * ((float)dst_w/msg.img.getWidth()));
				final int y = Math.round(msg.mouse.y * ((float)dst_h/msg.img.getHeight()));
				g.drawImage(CURSOR, x, y, null);
			}
		}
	}

	@Override public void _exit() {
		SwingUtilities.invokeLater(() -> {
			window.setVisible(false);
			window.dispose();
		});
		super._exit();
	}

}
