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
import java.awt.Point;
import java.awt.Toolkit;
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
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.lionsoul.jteach.msg.CommandMessage;
import org.lionsoul.jteach.msg.Message;
import org.lionsoul.jteach.rmi.RMIInterface;
import org.lionsoul.jteach.server.JBean;
import org.lionsoul.jteach.server.JServer;
import org.lionsoul.jteach.util.JCmdTools;
import org.lionsoul.jteach.util.JServerLang;
import org.lionsoul.jteach.util.JTeachIcon;


/**
 * Image receive thread for Screen monitor
 * @author chenxin - chenxin619315@gmail.com
 */
public class SMTask extends JFrame implements JSTaskInterface,Runnable {

	private static final long serialVersionUID = 1L;
	public static final String W_TITLE = "JTeach - Remote Window";
	public static final String EMTPY_INFO = "Loading Image Resource From JBean";
	public static final Font IFONT = new Font("Arial", Font.BOLD, 18);
	public static Image MOUSE_CURSOR = JTeachIcon.Create("m_plan.png").getImage();
	public static Point MOUSE_POS = null;
	public static Dimension wSize = null;
	
	private int TStatus = T_RUN;

	private JBean bean = null;		// monitor machine
	private final List<JBean> beanList;

	private DataInputStream reader = null;
	private volatile BufferedImage B_IMG = null;

	private final ImageJPanel map;
	private Dimension MAP_SIZE = null;
	private RMIInterface RMIInstance = null;
	private String control = null;
	private String broadcast = null;
	
	public SMTask(JServer server) {
		this.setTitle(W_TITLE);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
		wSize = new Dimension(JServer.SCREEN_SIZE.width,
				JServer.SCREEN_SIZE.height - screenInsets.bottom - screenInsets.top);
		this.setSize(wSize);
		setLayout(new BorderLayout());
		Container c = getContentPane();
		
		//add the map to the JScrollPane
		map = new ImageJPanel();
		JScrollPane vBox = new JScrollPane(map);
		vBox.setSize(wSize);
		vBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		vBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		c.add(vBox, BorderLayout.CENTER);
		//this.setUndecorated(true);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setResizable(false);
		this.addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				map.requestFocus();
			}
			@Override
			public void windowLostFocus(WindowEvent e) {}
		});

		beanList = Collections.synchronizedList(server.copyBeanList());
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
			if ( B_IMG == null ) {
				g.setColor(Color.WHITE);
				g.setFont(IFONT);
				FontMetrics m = getFontMetrics(IFONT);
				g.drawString(EMTPY_INFO,
						(getWidth() - m.stringWidth(EMTPY_INFO))/2, getHeight()/2);
			} else {
				g.drawImage(B_IMG, 0, 0, null);
				/* Draw the Mouse */
				g.drawImage(MOUSE_CURSOR, MOUSE_POS.x, MOUSE_POS.y, null);
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
	
	private void repaintImageJPanel() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				map.setPreferredSize(MAP_SIZE);
				map.setSize(MAP_SIZE);
				map.repaint();
			}
		});
	}
	
	private void _dispose() {
		RMIInstance = null;
		setVisible(false);
		dispose();
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
		String address = "rmi://" + host + ":" + JCmdTools.RMI_PORT + "/" + JCmdTools.RMI_OBJ;
		RMIInstance = (RMIInterface) Naming.lookup(address);
	}

	@Override
	public void addClient(JBean bean) {

	}
	
	@Override
	public void startTask() {
		String str = JServer.getInstance().getArguments().get(JCmdTools.SCREEN_MONITOR_KEY);
		if ( str == null ) {
			JServerLang.SCREEN_MONITOR_EMPTY_ARGUMENTS();
			JServer.getInstance().resetJSTask();
			return;
		}

		if ( str.matches("^[0-9]{1,}$") == false ) {
			System.out.printf("Invalid client index %s for task %s\n", str, this.getClass().getName());
			JServer.getInstance().resetJSTask();
			return;
		}

		int index = Integer.parseInt(str);
		if (index < 0 || index >= beanList.size()) {
			System.out.println("Index out of bounds.");
			JServer.getInstance().resetJSTask();
			return;
		}
		
		final JBean bean = beanList.get(index);

		// get the control arguments
		control = JServer.getInstance().getArguments().get(JCmdTools.REMOTE_CONTROL_KEY);
		if ( control != null && control.equals(JCmdTools.REMOTE_CONTROL_VAL) ) {
			try {
				getRMIInstance(bean.getAddr());
			} catch (MalformedURLException e) {
				System.out.println("Fail to load RMIServer instance.");
			} catch (RemoteException e) {
				System.out.println("Fail to load RMIServer instance.");
			} catch (NotBoundException e) {
				System.out.println("Fail to load RMIServer instance.");
			}
		}
		
		// get monitor broadcast arguments
		broadcast = JServer.getInstance().getArguments().get(JCmdTools.MONITOR_BROADCAST_KEY);
		if (broadcast != null && broadcast.equals(JCmdTools.MONITOR_BROADCAST_VAL)) {
			beanList.remove(index);
			final Iterator<JBean> it = beanList.iterator();
			while ( it.hasNext() ) {
				final JBean b = it.next();
				try {
					// b.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_BROADCAST_START_CMD);
					b.offer(new CommandMessage(JCmdTools.SERVER_BROADCAST_START_CMD));
				} catch (IllegalAccessException e) {
					b.reportClosedError();
					it.remove();
				}
			}
		}
		
		/* send start symbol and the image data receive thread */
		try {
			// bean.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_SCREEN_MONITOR_CMD);
			bean.offer(new CommandMessage(JCmdTools.SERVER_SCREEN_MONITOR_CMD));
			JServer.threadPool.execute(this);
			SwingUtilities.invokeLater(new Runnable(){
				@Override
				public void run() {
					setTitle(W_TITLE+"["+bean.getAddr()+"]");
					setVisible(true);
				}
			});
		} catch (IllegalAccessException e) {
			bean.reportClosedError();
			JServer.getInstance().resetJSTask();
		}
	}

	@Override
	public void stopTask() {
		setTStatus(T_STOP);

		try {
			// bean.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_TASK_STOP_CMD);
			bean.offer(new CommandMessage(JCmdTools.SERVER_TASK_STOP_CMD));
		} catch (IllegalAccessException e) {
			bean.reportClosedError();
		}
		
		// send stop symbol
		final Iterator<JBean> it = beanList.iterator();
		while (it.hasNext()) {
			final JBean b = it.next();
			try {
				// b.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_TASK_STOP_CMD);
				b.offer(new CommandMessage(JCmdTools.SERVER_TASK_STOP_CMD));
			} catch (IllegalAccessException e) {
				b.reportClosedError();
				it.remove();
			}
		}

		System.out.println("Screen monitor thread stopped!");
	}

	@Override
	public void run() {
		while ( getTStatus() == T_RUN ) {
			try {
				/* load symbol */
				final Message msg = bean.take();

				/*format the byte data to a BufferedImage*/
				B_IMG = ImageIO.read(new ByteArrayInputStream(new byte[0]));
				if (MAP_SIZE == null) {
					MAP_SIZE = new Dimension(B_IMG.getWidth(), B_IMG.getHeight());
				}

				/*repaint the Image JPanel*/
				repaintImageJPanel();
			} catch (InterruptedException e) {
				System.out.printf("client %s message take were interrupted\n", bean.getName());
			} catch (IOException e) {
				System.out.printf("client %s offline due to IOException\n", bean.getName());
				break;
			} catch (IllegalAccessException e) {
				bean.reportClosedError();
				break;
			}
		}

		_dispose();
	}
	
	private int getTStatus() {
		return TStatus;
	}
	
	private void setTStatus(int s) {
		TStatus = s;
	}

}
