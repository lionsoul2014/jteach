package org.lionsoul.jteach.client.task;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.lionsoul.jteach.client.JClient;
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.ScreenMessage;
import org.lionsoul.jteach.util.CmdUtil;
import org.lionsoul.jteach.util.ImageUtil;


/**
 * Task for Broadcast Receive when server started the Broadcast Send Thread
 * 
 * @author chenxin<chenxin619315@gmail.com>
 */
public class SBRTask extends JCTaskBase {

	/* Lang package */
	public static final String title = "JTeach - Remote Window";
	public static final String EMTPY_INFO = "Loading Image Resource From Server";
	public static final Font IFONT = new Font("Arial", Font.BOLD, 18);
	public static Image MOUSE_IMG = ImageUtil.Create("m_pen.png").getImage();
	public static final Log log = Log.getLogger(SBRTask.class);

	private final JFrame window;
	private final ImageJPanel imgJPanel;
	private volatile ScreenMessage screen = null;
	private final Dimension screenSize;
	private final Insets insetSize;

	public SBRTask(JClient client) {
		super(client);
		this.window = new JFrame();
		this.imgJPanel = new ImageJPanel();
		this.screenSize = window.getToolkit().getScreenSize();
		this.insetSize = window.getToolkit().getScreenInsets(window.getGraphicsConfiguration());
		initGUI();
	}

	private void initGUI() {
		window.setTitle(title);
		window.setUndecorated(true);
		window.setAlwaysOnTop(true);
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setSize(screenSize);
		// this.setBounds(0, 0, screenSize.width, screenSize.height);
		// this.setExtendedState(JFrame.MAXIMIZED_VERT);
		window.setLocationRelativeTo(null);
		window.setResizable(false);
		window.setLayout(new BorderLayout());
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				/* stop the JCTask and dispose the window */
				//stopCTask();
				//_dispose();
			}
		});
		window.getContentPane().add(imgJPanel, BorderLayout.CENTER);
		log.debug("screen size: {w: %d, h: %d}, insets: {t: %d, r: %d, b: %d, l: %d}\n",
				screenSize.width, screenSize.height,
				insetSize.top, insetSize.right, insetSize.bottom, insetSize.left);
	}
	
	/**
	 * Remote BufferedImage show JPanel.
	 * paint the BufferedImage Load from the socket.
	 */
	private class ImageJPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;

		public ImageJPanel() {}
		
		@Override
		public void update(Graphics g) {
			paintComponent(g);
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());

			/* Draw the waiting typo */
			if ( screen == null ) {
				g.setColor(Color.WHITE);
				g.setFont(IFONT);
				FontMetrics m = getFontMetrics(IFONT);
				g.drawString(EMTPY_INFO, (getWidth() - m.stringWidth(EMTPY_INFO))/2, getHeight()/2);
				return;
			}
			
			/* Draw the image */
			final int dst_w = getWidth() - insetSize.left - insetSize.right;
			final int dst_h = getHeight() - insetSize.top - insetSize.bottom;
			final BufferedImage img = ImageUtil.resize_2(screen.img, dst_w, dst_h);
			g.drawImage(img, 0, 0, dst_w, dst_h, null);

			/* Draw the Mouse */
			final int x = Math.round(screen.mouse.x * ((float)dst_w/screen.img.getWidth()));
			final int y = Math.round(screen.mouse.y * (float)dst_h/screen.img.getHeight());
			g.drawImage(MOUSE_IMG, x, y, null);
		}
	}
	
	private void repaintImageJPanel() {
		SwingUtilities.invokeLater(() -> imgJPanel.repaint());
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
		while ( getStatus() == T_RUN ) {
			try {
				final Packet p = bean.take();

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

				/* repaint the ImageJPanel */
				repaintImageJPanel();
			} catch (IllegalAccessException e) {
				log.error("task is overed due to %s", e.getClass().getName());
				break;
			} catch (InterruptedException e) {
				log.warn("bean.take was interrupted");
			}
		}
	}

	@Override
	public void onExit() {
		SwingUtilities.invokeLater(() -> {
			window.setVisible(false);
			window.dispose();
		});
		super.onExit();
	}

}