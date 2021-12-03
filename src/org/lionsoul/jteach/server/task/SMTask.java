package org.lionsoul.jteach.server.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.ScreenMessage;
import org.lionsoul.jteach.rmi.RMIInterface;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.server.JServer;
import org.lionsoul.jteach.util.CmdUtil;
import org.lionsoul.jteach.util.ImageUtil;


/**
 * Image receive thread for Screen monitor
 * @author chenxin - chenxin619315@gmail.com
 */
public class SMTask extends JSTaskBase {

	public static final String W_TITLE = "JTeach - Remote Window";
	public static final String EMTPY_INFO = "Loading Image Resource From JBean";
	public static final Font IFONT = new Font("Arial", Font.BOLD, 18);
	public static final Image MOUSE_CURSOR = ImageUtil.Create("m_plan.png").getImage();
	public static final Log log = Log.getLogger(UFTask.class);

	private final JFrame window;
	private final ImageJPanel map;

	private final Dimension screenSize;
	private volatile ScreenMessage screen = null;

	private JBean bean = null;		// monitor machine

	private RMIInterface RMIInstance = null;
	private String control = null;
	private String broadcast = null;
	
	public SMTask(JServer server) {
		super(server);
		this.window = new JFrame();
		this.map = new ImageJPanel();
		this.screenSize = window.getToolkit().getScreenSize();
		initGUI();
	}

	private void initGUI() {
		window.setTitle(W_TITLE);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		final Insets screenInsets = window.getToolkit().getScreenInsets(window.getGraphicsConfiguration());
		final Dimension wSize = new Dimension(screenSize.width, screenSize.height - screenInsets.bottom - screenInsets.top);
		window.setSize(screenSize);
		window.setUndecorated(true);
		window.setLayout(new BorderLayout());
		final Container c = window.getContentPane();

		//add the map to the JScrollPane
		JScrollPane vBox = new JScrollPane(map);
		vBox.setSize(wSize);
		vBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		vBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		c.add(vBox, BorderLayout.CENTER);
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setResizable(false);
		window.addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				map.requestFocus();
			}
			@Override
			public void windowLostFocus(WindowEvent e) {}
		});
	}

	private void repaintImageJPanel() {
		SwingUtilities.invokeLater(() -> map.repaint());
	}
	
	private void _dispose() {
		RMIInstance = null;
		window.setVisible(false);
		window.dispose();
	}
	
	/**
	 * create a quote to the RMIServer 
	 * @throws NotBoundException 
	 * @throws RemoteException 
	 * @throws MalformedURLException 
	 */
	private void getRMIInstance(String host) throws MalformedURLException,
		RemoteException, NotBoundException {
		String path = System.getProperty("user.dir").replaceAll("\\\\", "/");
		System.setProperty("java.security.policy", "file:///" + path + "/security.policy");
		System.setSecurityManager(new RMISecurityManager());
		String address = "rmi://" + host + ":" + CmdUtil.RMI_PORT + "/" + CmdUtil.RMI_OBJ;
		RMIInstance = (RMIInterface) Naming.lookup(address);
	}

	@Override
	public boolean _before() {
		String str = server.getArguments().get(CmdUtil.SCREEN_MONITOR_KEY);
		if ( str == null ) {
			server.println("-+-i : Integer: Monitor the specifier client's screen");
			return false;
		}

		if (!str.matches("^[0-9]{1,}$")) {
			server.println("invalid client index %s", str);
			return false;
		}

		int index = Integer.parseInt(str);
		if (index < 0 || index >= beanList.size()) {
			server.println("index %d out of bounds", index);
			return false;
		}
		
		bean = beanList.get(index);

		// get the control arguments
		control = server.getArguments().get(CmdUtil.REMOTE_CONTROL_KEY);
		if ( control != null && control.equals(CmdUtil.REMOTE_CONTROL_VAL) ) {
			try {
				getRMIInstance(bean.getHost());
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				server.println("failed to load RMIServer instance due to %s", e.getClass().getName());
				return false;
			}
		}

		/* send start symbol and the image data receive thread */
		try {
			bean.offer(Packet.COMMAND_SCREEN_MONITOR);
			JBean.threadPool.execute(this);
			SwingUtilities.invokeLater(() -> {
				window.setTitle(W_TITLE+"["+bean.getHost()+"]");
				window.setVisible(true);
			});
		} catch (IllegalAccessException e) {
			server.println(bean.getClosedError());
			return false;
		}

		/* get monitor broadcast arguments */
		broadcast = server.getArguments().get(CmdUtil.MONITOR_BROADCAST_KEY);
		if (broadcast != null && broadcast.equals(CmdUtil.MONITOR_BROADCAST_VAL)) {
			beanList.remove(index);
			final Iterator<JBean> it = beanList.iterator();
			while ( it.hasNext() ) {
				final JBean b = it.next();
				try {
					b.offer(Packet.COMMAND_BROADCAST_START);
				} catch (IllegalAccessException e) {
					b.reportClosedError();
					it.remove();
				}
			}
		}

		return true;
	}

	@Override
	public void stop() {
		if (bean != null) {
			try {
				bean.offer(Packet.COMMAND_TASK_STOP);
			} catch (IllegalAccessException e) {
				server.println(bean.getClosedError());
			}
		}
		super.stop();
	}

	@Override
	public void _run() {
		while ( getStatus() == T_RUN ) {
			try {
				/* load symbol */
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

				/* repaint the Image JPanel */
				repaintImageJPanel();
			} catch (InterruptedException e) {
				server.println(log.getWarn("client %s message take were interrupted", bean.getName()));
			} catch (IllegalAccessException e) {
				server.println(bean.getClosedError());
				break;
			}
		}
	}

	public void onExit() {
		_dispose();
		super.onExit();
	}


	/** screen image show JPanel */
	private class ImageJPanel extends JPanel implements
			MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
		private static final long serialVersionUID = 1L;

		public ImageJPanel() {
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
			addKeyListener(this);
			setFocusable(true);
			setFocusTraversalKeysEnabled(false);
		}

		@Override
		public void update(Graphics g) {
			paintComponent(g);
		}

		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			if (screen == null) {
				g.setColor(Color.WHITE);
				g.setFont(IFONT);
				final FontMetrics m = getFontMetrics(IFONT);
				g.drawString(EMTPY_INFO, (getWidth() - m.stringWidth(EMTPY_INFO))/2, getHeight()/2);
			} else {
				g.drawImage(screen.img, 0, 0, null);
				/* Draw the Mouse */
				g.drawImage(MOUSE_CURSOR, screen.mouse.x, screen.mouse.y, null);
			}
		}

		/**
		 * mouse listener area
		 */
		@Override
		public void mouseClicked(MouseEvent e) {}
		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}
		@Override
		public void keyTyped(KeyEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {
			if ( RMIInstance == null ) {
				return;
			}

			int button = -1;
			switch ( e.getButton() ) {
				/**Button 1*/
				case MouseEvent.BUTTON1:
					button = InputEvent.BUTTON1_MASK;
					break;
				case MouseEvent.BUTTON2:
					button = InputEvent.BUTTON2_MASK;
					break;
				case MouseEvent.BUTTON3:
					button = InputEvent.BUTTON3_MASK;
					break;
			}

			try {
				RMIInstance.mousePress(button);
			} catch (RemoteException e1) {
				e1.printStackTrace();
				System.out.println("-+**Error: mouse press execute.");
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if ( RMIInstance == null ) {
				return;
			}

			int button = -1;
			switch ( e.getButton() ) {
				/**Button 1*/
				case MouseEvent.BUTTON1:
					button = InputEvent.BUTTON1_MASK;
					break;
				case MouseEvent.BUTTON2:
					button = InputEvent.BUTTON2_MASK;
					break;
				case MouseEvent.BUTTON3:
					button = InputEvent.BUTTON3_MASK;
					break;
			}

			try {
				RMIInstance.mouseRelease(button);
			} catch (RemoteException e1) {
				System.out.println("-+**Error: mouse release execute.");
			}
		}


		@Override
		public void mouseMoved(MouseEvent e) {
			if ( RMIInstance == null ) return;

			try {
				RMIInstance.mouseMove( e.getX(),  e.getY());
			} catch (RemoteException e1) {
				System.out.println("-+**Error: mouse move execute.");
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if ( RMIInstance == null ) return;
			try {
				RMIInstance.mouseMove(e.getX(), e.getY());
			} catch (RemoteException e1) {
				System.out.println("-+**Error: mouse drag execute.");
			}
		}


		/**
		 * mouse wheel listener area
		 */
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if ( RMIInstance == null ) return;
			try {
				RMIInstance.mouseWheel(e.getWheelRotation());
			} catch (RemoteException e1) {
				System.out.println("-+**Error: mouse wheel execute.");
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if ( RMIInstance == null ) return;
			try {
				//System.out.println("after:"+e.getKeyCode());
				RMIInstance.keyPress(e.getKeyCode());
			} catch (RemoteException e1) {
				System.out.println("-+**Error: key press execute.");
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if ( RMIInstance == null ) return;
			try {
				RMIInstance.keyRelease(e.getKeyCode());
			} catch (RemoteException e1) {
				System.out.println("-+**Error: key release execute.");
			}
		}
	}

}
