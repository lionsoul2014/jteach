package org.lionsoul.jteach.client;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.lionsoul.jteach.client.task.JCTaskBase;
import org.lionsoul.jteach.client.task.RCRTask;
import org.lionsoul.jteach.client.task.SBRTask;
import org.lionsoul.jteach.client.task.SMSTask;
import org.lionsoul.jteach.client.task.UFRTask;
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.rmi.RMIServer;
import org.lionsoul.jteach.util.JClientCfg;
import org.lionsoul.jteach.util.JCmdTools;


/**
 * JTeach Client .
 * @author chenxin - chenxin619315@gmail.com
 */
public class JClient extends JFrame implements Runnable {
	
	private static final long serialVersionUID = 1L;
	public static final int T_OVER = -1;
	public static final int T_STOP = 0;
	public static final int T_RUN = 1;
	public static Object Lock = new Object();
	public static final String OS = System.getProperty("os.name").toUpperCase();
	public static final Log log = Log.getLogger(JClient.class);

	public static int PORT = 55535;
	private static RMIServer RMIInstance = null;

	/** GUI Component area */
	private JLabel tipLabel = null;
	private JTextField serverTextField = null;
	private JTextField portTextField = null;
	private JButton connectButton = null;

	private int T_STATUS = T_RUN;
	private volatile JBean bean = null;
	private volatile JCTaskBase JCTask = null;

	private JClient() {
		this.setTitle(JClientCfg.W_TITLE);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close();
			}
			public void windowIconified(WindowEvent e) {
			}
		});

		this.setSize(JClientCfg.W_SIZE);
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		initGUI();
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
		
		// server ip
		JLabel serverLable = new JLabel(JClientCfg.SERVER_LABLE_TEXT);
		serverLable.setBounds(15, 55, 80, 24);
		serverLable.setFont(JClientCfg.LABLE_FONT);
		serverLable.setForeground(JClientCfg.LABEL_FRONT_COLOR);
		c.add(serverLable);
		
		serverTextField = new JTextField();
		serverTextField.setBounds(100, 55, 200, 24);
		serverTextField.addActionListener(e -> getConnectionJButton().doClick());
		c.add(serverTextField);
		
		// server port
		JLabel portLabel = new JLabel(JClientCfg.PORT_LABLE_TEXT);
		portLabel.setBounds(15, 90, 80, 24);
		portLabel.setFont(JClientCfg.LABLE_FONT);
		portLabel.setForeground(JClientCfg.LABEL_FRONT_COLOR);
		c.add(portLabel);
		
		portTextField = new JTextField(PORT+"");
		portTextField.setBounds(100, 90, 200, 24);
		portTextField.addActionListener(e -> getConnectionJButton().doClick());
		c.add(portTextField);
		
		connectButton = new JButton(JClientCfg.CONNECT_BUTTON_TEXT);
		connectButton.setBounds(30, 125, 100, 24);
		c.add(connectButton);
		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JBean.threadPool.execute(new Thread(() -> connect()));
			}
		});
		
		JButton aboutButton = new JButton(JClientCfg.ABOUT_BUTTON_TEXT);
		aboutButton.setBounds(200, 125, 100, 24);
		c.add(aboutButton);
		aboutButton.addActionListener(e -> JOptionPane.showMessageDialog(null,
				JClientCfg.ABOUT_INFO, "JTeach:", JOptionPane.INFORMATION_MESSAGE));
	}
	
	public JButton getConnectionJButton() {
		return connectButton;
	}

	/* check and reconnect to the server */
	public boolean connect() {
		if (bean != null && bean.isClosed() == false) {
			return true;
		}

		String ip = serverTextField.getText().trim();
		if (ip.equals("")) {
			JOptionPane.showMessageDialog(null, "Ask The Boss For Server IP First.");
			return false;
		}

		try {
			InetAddress.getByName(ip);
		} catch (UnknownHostException e2) {
			JOptionPane.showMessageDialog(null, "Invalid Server IP.");
			return false;
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
				bean = new JBean(s);
				bean.start();
				startCMDMonitor();

				/* register a RMI Server:
				 * watch out the reconnection here.
				 * so register the rmi server when the RMIInstance is not null */
				if (RMIInstance == null) {
					regRMI();
				}

				log.debug("client connected to server %s:%d", ip, PORT);
				setTipInfo("Connected, Wait Symbol From Server.");
				break;
			} catch (UnknownHostException e1) {
				JOptionPane.showMessageDialog(null, "invalid Server IP.");
				break;
			} catch (RemoteException e1 ) {
				JOptionPane.showMessageDialog(null, "rmi register error, but it doesn't matter!");
				break;
			} catch (IOException e1) {
				log.debug("%dth try: failed to connect to server by %s:%d", counter, ip, PORT);
			}

			if (++counter > 600) {
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
			return false;
		} else {
			log.debug("%dth try: successfully connected to the server by %s:%d", counter, ip, PORT);
		}

		return true;
	}

	/**
	 * registry RMI instance
	 * @throws RemoteException
	 * @throws MalformedURLException
	 * @throws UnknownHostException 
	 */
	private void regRMI() throws RemoteException, MalformedURLException, UnknownHostException {
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
		SwingUtilities.invokeLater(() -> setTitle(JClientCfg.W_TITLE + " - " + hostAddr));
	}
	
	/** call when use try to exit the program */
	public void close() {
		if (bean != null && bean.isClosed() == false ) {
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
				bean.clear();
			}
		}

		System.exit(0);
	}
	
	/** set tip message */
	public void setTipInfo(final String str) {
		SwingUtilities.invokeLater(() -> tipLabel.setText(str));
	}
	
	/** start Server Input Monitor */
	public void startCMDMonitor() {
		JBean.threadPool.execute(this);
	}
	
	/**
	 * wait for the server's cmd
	 * and run the Specified the Thread according to the cmd */
	@Override
	public void run() {
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
				/* Message symbol */
				// bean.getSocket().setSoTimeout(0);
				log.debug("waiting for data packet from server ... ");
				final Packet p = bean.take();
				if (p.symbol != JCmdTools.SYMBOL_SEND_CMD) {
					continue;
				}

				setTipInfo("Command From Server, Code:" + p.cmd);

				/*
				 * Screen Broadcast
				 * wait the JClient's command monitor thread
				 * change the JCTask pointer to a new SBRTask Object
				 * then start the Thread
				 */
				if (p.isCommand(JCmdTools.COMMAND_BROADCAST_START)) {
					setTStatus(T_STOP);
					JCTask = new SBRTask(this);
					log.debug("try to start task %s by command %s", JCTask.getClass().getName(), p.cmd);
					JCTask.start();
					log.debug("task %s stopped", JCTask.getClass().getName());
				}

				/*
				 * File Upload
				 * wait the JClient's command monitor thread
				 * change the JCTask pointer to a new UFRTask Object
				 * then start the thread
				 */
				else if (p.isCommand(JCmdTools.COMMAND_UPLOAD_START)) {
					setTStatus(T_STOP);
					JCTask = new UFRTask(this);
					log.debug("try to start task %s by command %s", JCTask.getClass().getName(), p.cmd);
					JCTask.start();
					log.debug("task %s stopped", JCTask.getClass().getName());
				}

				/*
				 * Screen Monitor
				 * change the JCTask pointer to a new SMSTask Object
				 * then start the thread
				 */
				else if (p.isCommand(JCmdTools.COMMAND_SCREEN_MONITOR)) {
					JCTask = new SMSTask(this);
					log.debug("try to start task %s by command %s", JCTask.getClass().getName(), p.cmd);
					JCTask.start();
					log.debug("task %s stopped", JCTask.getClass().getName());
				}

				/*
				 * remote command execute
				 * wait the JClient's command monitor thread
				 * change the JCTask pointer to a new RCRTask Object
				 * then start the thread
				 */
				else if (p.isCommand(JCmdTools.COMMAND_RCMD_ALL_EXECUTION,
						JCmdTools.COMMAND_RCMD_SINGLE_EXECUTION)) {
					setTStatus(T_STOP);
					JCTask = new RCRTask(this);
					log.debug("try to start task %s by command %s", JCTask.getClass().getName(), p.cmd);
					JCTask.start(p.isCommand(JCmdTools.COMMAND_RCMD_ALL_EXECUTION) ? "all" : "single");
					log.debug("task %s stopped", JCTask.getClass().getName());
				}

				/*
				 * stop the working JCTask
				 * if the working thread could load data from server
				 * this is unnecessary cause this kind of thread will over in its own thread
				 * so this one is for the thread that send data to the server
				 */
				else if (p.isCommand(JCmdTools.COMMAND_TASK_STOP)) {
					if (JCTask != null) {
						JCTask.stop();
						log.debug("task %s stopped by command %s", JCTask.getClass().getName(), p.cmd);
						JCTask = null;
					}
				}

				/*
				 * the server is closed
				 * and this command order the client to exit;
				 */
				else if (p.isCommand(JCmdTools.COMMAND_EXIT)) {
					System.exit(0);
				}
			} catch (IllegalAccessException e) {
				reConnect = true;
				bean.reportClosedError();
				break;
			} catch (InterruptedException e) {
				log.warn("bean.take were interrupted");
			}
		}

		if (reConnect) {
			log.debug("client is now offline cus of error, reconnecting ... ");
			connect();
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
	public JBean getBean() {
		return bean;
	}

	public synchronized int getTStatus() {
		return T_STATUS;
	}
	public synchronized void setTStatus(int s) {
		T_STATUS = s;
	}

	public static void main(String[] args) {
		final JClient client = new JClient();
		SwingUtilities.invokeLater(() -> {
			client.setVisible(true);
		});
	}

}
