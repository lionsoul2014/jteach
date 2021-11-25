package com.webssky.jteach.client;

import java.awt.AWTException;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.webssky.jteach.client.task.JCTaskInterface;
import com.webssky.jteach.client.task.RCRTask;
import com.webssky.jteach.client.task.SBRTask;
import com.webssky.jteach.client.task.SMSTask;
import com.webssky.jteach.client.task.UFRTask;
import com.webssky.jteach.rmi.RMIServer;
import com.webssky.jteach.util.JClientCfg;
import com.webssky.jteach.util.JCmdTools;
import com.webssky.jteach.util.JTeachIcon;


/**
 * JTeach Client <br />
 * 
 * @author chenxin - chenxin619315@gmail.com <br />
 */
public class JClient extends JFrame {
	
	private static final long serialVersionUID = 1L;
	public static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
	public static ExecutorService threadPool = Executors.newCachedThreadPool();
	public static final int T_OVER = -1;
	public static final int T_STOP = 0;
	public static final int T_RUN = 1;
	public static Object Lock = new Object();
	public static final ImageIcon TRAY_ICON = JTeachIcon.Create("ws-tray.png");
	public static TrayIcon tray = null;
	public static final String OS = System.getProperty("os.name").toUpperCase();
	
	public static int PORT = 55535;
	private static RMIServer RMIInstance = null;
	private static JClient _instance = null;
	
	/**
	 * GUI Component area
	 */
	private JLabel tipLabel = null;
	private JTextField serverTextField = null;
	private JTextField portTextField = null;
	private JButton connectButton = null;

	private int T_STATUS = T_RUN;
	private Socket socket = null;
	private DataInputStream reader = null;
	private DataOutputStream writer = null;
	private volatile JCTaskInterface JCTask = null;

	public static JClient getInstance() {
		if ( _instance == null ) {
			_instance = new JClient();
		}
		return _instance;
	}
	
	private JClient() {
		this.setTitle(JClientCfg.W_TITLE);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close();
			}
			public void windowIconified(WindowEvent e) {
				// JClient.tray();
			}
		});
		setSize(JClientCfg.W_SIZE);
		setResizable(false);
		initGUI();
		setLocationRelativeTo(null);
		JCmdTools.getNetInterface();
	}
	
	/**
	 * initialize the GUI 
	 */
	private void initGUI() {
		setLayout(null);
		Container c = getContentPane();
		tipLabel = new JLabel(JClientCfg.TIP_INIT_TEXT);
		tipLabel.setFont(JClientCfg.LABLE_FONT);
		tipLabel.setHorizontalAlignment(SwingConstants.CENTER);
		tipLabel.setBounds(8, 5, 320, 30);
		tipLabel.setOpaque(true);
		tipLabel.setBackground(JClientCfg.TIP_BG_COLOR);
		tipLabel.setForeground(JClientCfg.TIP_FRON_COLOR);
		c.add(tipLabel);
		
		//server ip
		JLabel serverLable = new JLabel(JClientCfg.SERVER_LABLE_TEXT);
		serverLable.setBounds(15, 55, 80, 24);
		serverLable.setFont(JClientCfg.LABLE_FONT);
		serverLable.setForeground(JClientCfg.LABEL_FRONT_COLOR);
		c.add(serverLable);
		
		serverTextField = new JTextField();
		serverTextField.setBounds(100, 55, 200, 24);
		serverTextField.addActionListener(LoginActionListener.getInstance());
		c.add(serverTextField);
		
		//server port
		JLabel portLabel = new JLabel(JClientCfg.PORT_LABLE_TEXT);
		portLabel.setBounds(15, 90, 80, 24);
		portLabel.setFont(JClientCfg.LABLE_FONT);
		portLabel.setForeground(JClientCfg.LABEL_FRONT_COLOR);
		c.add(portLabel);
		
		portTextField = new JTextField(PORT+"");
		portTextField.setBounds(100, 90, 200, 24);
		portTextField.addActionListener(LoginActionListener.getInstance());
		c.add(portTextField);
		
		connectButton = new JButton(JClientCfg.CONNECT_BUTTON_TEXT);
		connectButton.setBounds(30, 125, 100, 24);
		c.add(connectButton);
		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				threadPool.execute(new Thread(() -> connect()));
			}
		});
		
		JButton aboutButton = new JButton(JClientCfg.ABOUT_BUTTON_TEXT);
		aboutButton.setBounds(200, 125, 100, 24);
		c.add(aboutButton);
		aboutButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null, JClientCfg.ABOUT_INFO, "JTeach:",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
	}
	
	public JButton getConnectionJButton() {
		return connectButton;
	}

	/* check and reconnect to the server */
	public void connect() {
		if (socket != null) {
			return;
		}

		String ip = serverTextField.getText().trim();
		if (ip.equals("")) {
			JOptionPane.showMessageDialog(null, "Ask The Boss For Server IP First.");
		}

		try {
			InetAddress.getByName(ip);
		} catch (UnknownHostException e2) {
			JOptionPane.showMessageDialog(null, "Invalid Server IP.");
		}

		String port = portTextField.getText().trim();
		if ( ! port.equals("") && port.matches("/[0-9]+/") ) {
			PORT = Integer.parseInt(port);
		}

		/* keep trying for server times */
		boolean timeOut = false;
		int counter = 0;
		while (true) {
			try {
				final Socket s = new Socket(ip, PORT);
				JClient.getInstance().setSocket(s);
				JClient.getInstance().starCMDMonitor();

				/*
				 * register a RMI Server:
				 * watch out the reconnection here.
				 * so register the rmi server when the RMIInstance is not null
				 */
				if (RMIInstance == null) {
					regRMI();
				}

				setTipInfo("Connected, Wait Symbol From Server.");
				break;
			} catch (UnknownHostException e1) {
				JOptionPane.showMessageDialog(null, "invalid Server IP.");
				break;
			} catch (RemoteException e1 ) {
				JOptionPane.showMessageDialog(null, "rmi register error, but it doesn't matter!");
				break;
			} catch (IOException e1) {
				System.out.printf("%2d th trying: failed to connect server by %s:%d\n", counter, ip, port);
			}

			if (++counter > 30) {
				timeOut = true;
				break;
			}

			// Wait for a second
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// check and display the timeout message
		if (timeOut) {
			JOptionPane.showMessageDialog(null, "fail To Create Socket");
		} else {
			System.out.printf("%2dth trying: successfully connected to the server by %s:%d\n", counter, ip, PORT);
		}
	}

	/**
	 * registry RMI instance
	 * @throws RemoteException
	 * @throws MalformedURLException
	 * @throws UnknownHostException 
	 */
	public void regRMI() throws RemoteException,
		MalformedURLException, UnknownHostException {
		/* get the linux's remote host */
		String host = InetAddress.getLocalHost().getHostAddress();
		if ( OS.equals("LINUX") ) {
			HashMap<String, String> ips = JCmdTools.getNetInterface();
			String remote = ips.get(JCmdTools.HOST_REMOTE_KEY);
			if ( remote != null ) {
				host = remote;
			}
		}
		
		System.setProperty("java.rmi.server.hostname", host);
		String codebase = JCmdTools.getJarHome(this).replaceAll("\\\\", "/");
		System.setProperty("java.rmi.server.codebase", "file://" + codebase + "/rmi-stub.jar");
		System.setProperty("java.security.policy", "file://" + codebase + "security.policy");
		LocateRegistry.createRegistry(JCmdTools.RMI_PORT);

		/* create a RMIServer instance */
		RMIInstance = RMIServer.getInstance();
		Naming.rebind("rmi://" + host +
				":" + JCmdTools.RMI_PORT + "/" +JCmdTools.RMI_OBJ, RMIInstance);
		
		// update the JFrame's title
		final String hostAddr = host;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setTitle(JClientCfg.W_TITLE + " - " + hostAddr);
			}
		});
	}
	
	/** call when use try to exit the program */
	public void close() {
		if ( socket != null ) {
			/*
			 * if the socket is not null
			 * show a confirm dialog and exit the program only when the
			 * user mean it 
			 */
			int _result = JOptionPane.showConfirmDialog(
				null, JClientCfg.EXIT_ONLINE_TEXT,
				"JTeach:",
				JOptionPane.OK_CANCEL_OPTION
			);
			if ( _result == JOptionPane.OK_OPTION ) {
				try {
					if ( reader != null ) reader.close(); 
					if ( socket != null ) socket.close();
					socket = null;
				} catch (IOException e1) {

				}
			}
		}

		System.exit(0);
	}
	
	/**
	 * clear the resource:
	 * close the socket, reader, writer. 
	 */
	public void offLineClear() {
		try {
			if ( reader != null ) reader.close();
			if ( writer != null ) writer.close(); 
			if ( socket != null ) socket.close();
			reader = null;
			writer = null;
			socket = null;
			//Naming.unbind(JCmdTools.RMI_OBJ);
			//RMIInstance = null;
		} catch (Exception e1) {

		}

		setTipInfo("You are now offline.");
	}
	
	/**
	 * add program to systemTray 
	 */
	public static void tray() {
		if ( tray != null ) {
			getInstance().setVisible(false);
			return;
		}

		SystemTray systemTray = SystemTray.getSystemTray();
		tray = new TrayIcon(TRAY_ICON.getImage(), "JTeach - webssky");
		tray.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getInstance().setVisible(true);
			}
		});
		tray.setImageAutoSize(true);

		PopupMenu popupMenu = new PopupMenu();
		MenuItem about = new MenuItem("About JTeach");
		about.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( ! Desktop.isDesktopSupported()) {
					JOptionPane.showMessageDialog(null, "Unsupport function for your System," +
							"You can visit site http://www.webssky.com directly",
							"JTeach: ", JOptionPane.ERROR_MESSAGE);
					return;
				}
				Desktop desktop = Desktop.getDesktop();
				if ( ! desktop.isSupported(Desktop.Action.BROWSE) ) {
					JOptionPane.showMessageDialog(null, "Unsupport function for your System, " +
							"You can visit site http://www.webssky.com directly",
							"JTeach: ", JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					desktop.browse(new URI("http://www.webssky.com"));
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(null, e1);
				}
			}
		});
		popupMenu.add(about);
		
		MenuItem exit = new MenuItem("Exit");
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getInstance().close();
			}
		});
		popupMenu.add(exit);
		tray.setPopupMenu(popupMenu);
		
		try {
			systemTray.add(tray);
			getInstance().setVisible(false);
		} catch (AWTException e) {
			JOptionPane.showMessageDialog(null, "Fail to add to SystemTray",
					"JTeach: ", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/** set tip message */
	public void setTipInfo(final String str) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				tipLabel.setText(str);
			}
		});
	}
	
	/** initialize the Socket */
	public void setSocket(Socket s) throws SocketException {
		if (s == null) {
			throw new NullPointerException();
		}

		socket = s;
		socket.setSoTimeout(3 * 1000);
	}
	
	/** start Server Input Monitor */
	public void starCMDMonitor() {
		threadPool.execute(new CmdMonitor());
	}
	
	/**
	 * wait for the server's cmd
	 * and run the Specified the Thread according to the cmd */
	private class CmdMonitor implements Runnable {
		@Override
		public void run() {
			final DataInputStream in = getReader();
			if ( in == null ) {
				return;
			}

			boolean reConnect = false;
			while ( true ) {
				/* stop the thread */
				if ( getTStatus() == T_OVER ) {
					break;
				}

				/* wait the thread when any JCTask is start */
				if ( getTStatus() == T_STOP ) {
					synchronized (Lock) {
						try {
							Lock.wait();
						} catch (InterruptedException e) {
							continue;
						}
					}
				}
				
				try {
					// check and break the socket if
					// it were closed or cleared
					if (socket == null) {
						break;
					}

					/* Message symbol */
					socket.setSoTimeout(0);
					char symbol = in.readChar();
					if (symbol != JCmdTools.SEND_CMD_SYMBOL) {
						continue;
					}

					int _cmd = in.readInt();
					setTipInfo("Command From Server, Code:" + _cmd);

					/*
					 * Screen Broadcast
					 * wait the JClient's command monitor thread
					 * change the JCTask pointer to a new SBRTask Object
					 * then start the Thread
					 */
					if (_cmd == JCmdTools.SERVER_BROADCAST_START_CMD) {
						setTStatus(T_STOP);
						socket.setSoTimeout(JCmdTools.SO_TIMEOUT);
						JCTask = new SBRTask();
						JCTask.startCTask();
					}

					/*
					 * File Upload
					 * wait the JClient's command monitor thread
					 * change the JCTask pointer to a new UFRTask Object
					 * then start the thread
					 */
					else if (_cmd == JCmdTools.SERVER_UPLOAD_START_CMD) {
						setTStatus(T_STOP);
						JCTask = new UFRTask();
						JCTask.startCTask();
					}

					/*
					 * Screen Monitor
					 * change the JCTask pointer to a new SMSTask Object
					 * then start the thread
					 */
					else if (_cmd == JCmdTools.SERVER_SCREEN_MONITOR_CMD) {
						//String isControl = in.readUTF();
						JCTask = new SMSTask();
						JCTask.startCTask();
					}

					/*
					 * remote command execute
					 * wait the JClient's command monitor thread
					 * change the JCTask pointer to a new RCRTask Object
					 * then start the thread
					 */
					else if (_cmd == JCmdTools.SERVER_RCMD_EXECUTE_CMD) {
						setTStatus(T_STOP);
						JCTask = new RCRTask();
						JCTask.startCTask();
					}

					/*
					 * stop the working JCTask
					 * if the working thread could load data from server
					 * this is unnecessary cause this kind of thread will over in its own thread
					 * so this one is for the thread that send data to the server
					 */
					else if (_cmd == JCmdTools.SERVER_TASK_STOP_CMD) {
						if (JCTask != null) {
							JCTask.stopCTask();
							JCTask = null;
						}
					}

					/*
					 * the server is closed
					 * and this command order the client to exit;
					 */
					else if (_cmd == JCmdTools.SERVER_EXIT_CMD) {
						System.exit(0);
					}
				} catch (IOException e) {
					e.printStackTrace();
					offLineClear();
					reConnect = true;
					break;
				}
			}

			if (reConnect) {
				System.out.println("client is now offline cus of error, try to reconnecting ... ");
				connect();
			}
		}
	}
	
	/**
	 * Notify the waiting thread
	 * call when the JCTask is overed 
	 */
	public void notifyCmdMonitor() {
		setTStatus(T_RUN);
		synchronized ( Lock ) {
			Lock.notify();
		}
	}
	
	public void resetJCTask() {
		JCTask = null;
	}
	
	/**
	 * get the Socket 
	 */
	public Socket getSocket() {
		return socket;
	}
	
	/**
	 * return the socket DataInputStream 
	 */
	public DataInputStream getReader() {
		if ( socket == null ) return null;
		try {
			reader = new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			//System.out.println("Fail To Get InputStream From Socket");
			JOptionPane.showMessageDialog(null,
					"Fail To Get InputStream From Socket",
					"JTeach:", JOptionPane.ERROR_MESSAGE);
		}
		return reader;
	}
	
	/**
	 * return the Socket DataOutputStream
	 * @throws IOException 
	 */
	public DataOutputStream getWriter() throws IOException {
		if ( socket == null ) {
			return null;
		}

		writer = new DataOutputStream(socket.getOutputStream());
		return writer;
	}
	
	public synchronized int getTStatus() {
		return T_STATUS;
	}
	public synchronized void setTStatus(int s) {
		T_STATUS = s;
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		SwingUtilities.invokeLater(() -> {
			JClient.getInstance().setVisible(true);
		});
	}

}
