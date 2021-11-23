package com.webssky.jteach.client.task;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
//import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.webssky.jteach.client.JClient;
import com.webssky.jteach.util.JCmdTools;
import com.webssky.jteach.util.JTeachIcon;


/**
 * Task for Broadcast Receive when server started the
 * 		Broadcast Send Thread <br />
 * 
 * @author chenxin <br />
 */
public class SBRTask extends JFrame implements JCTaskInterface {

	private static final long serialVersionUID = 1L;
	/**
	 * Lang package 
	 */
	public static final String title = "JTeach - Remote Window";
	public static final String EMTPY_INFO = "Loading Image Resource From Server";
	public static final Font IFONT = new Font("Arial", Font.BOLD, 18);
	public static Image MOUSE_CURSOR = JTeachIcon.Create("m_pen.png").getImage();
	public static float BIT = 1;
	public static Dimension IMG_SIZE = null;
	public static Point IMG_POS = null;
	public static Point MOUSE_POS = null;
	
	
	private int TStatus = T_RUN;
	private volatile BufferedImage B_IMG = null;
	private ImageJPanel imgJPanel = null;
	
	public SBRTask() {
		this.setTitle(title);
		this.setUndecorated(true);
		this.setAlwaysOnTop(true);
		//this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setSize(JClient.SCREEN_SIZE);
		this.setResizable(false);
		setLayout(new BorderLayout());
		imgJPanel = new ImageJPanel();
		getContentPane().add(imgJPanel, BorderLayout.CENTER);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				/*
				 * stop the JCTask and dispose the window 
				 */
				//stopCTask();
				//_dispose();
			}
		});
	}
	
	/**
	 * Remote BufferedImage show JPanel. <br />
	 * 		paint the BufferedImage Load from the socket. <br />
	 * 
	 * @author chenxin
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
			if ( B_IMG == null ) {
				g.setColor(Color.WHITE);
				g.setFont(IFONT);
				FontMetrics m = getFontMetrics(IFONT);
				g.drawString(EMTPY_INFO, (getWidth() - m.stringWidth(EMTPY_INFO))/2, getHeight()/2);
				return;
			}
			
			if ( IMG_SIZE == null ) {
				BIT = Math.max((float)B_IMG.getWidth()/JClient.SCREEN_SIZE.width,
						(float)B_IMG.getHeight()/JClient.SCREEN_SIZE.height);
				/*
				 * the server'screen size is larger then the client's 
				 */
				/*
				int h = (int) Math.ceil((B_IMG.getHeight() / BIT));
				if ( h > getHeight() ) h = (int) (B_IMG.getHeight() / BIT);
				int w = (int) Math.ceil(B_IMG.getWidth() / BIT);
				if ( w > getWidth() ) w = (int) (B_IMG.getWidth() / BIT); 
				IMG_SIZE = new Dimension(w, h);
				
				int x = 0, y = 0;
				if ( IMG_SIZE.width < getWidth() ) x = (getWidth() - IMG_SIZE.width) / 2;
				if ( IMG_SIZE.height < getHeight() ) y = ( getHeight() - IMG_SIZE.height ) / 2; 
				IMG_POS = new Point(x, y);*/
			}
			
			/*
			 * Draw the image from server
			 * start from point IMG_POS with size IMG_SIZE 
			 */
//			g.drawImage(B_IMG, IMG_POS.x, IMG_POS.y,
//					IMG_SIZE.width, IMG_SIZE.height, null);
			// g.drawImage(B_IMG, 0, 0, null);
			final BufferedImage img = JTeachIcon.resize_2(B_IMG, getWidth(), getHeight());
			g.drawImage(img, 0, 0, null);

			/*Draw the Mouse*/
			g.drawImage(MOUSE_CURSOR, (int) (MOUSE_POS.x / BIT),
					(int) ( MOUSE_POS.y / BIT), null);
		}
	}
	
	private void repaintImageJPanel() {
		SwingUtilities.invokeLater(() -> imgJPanel.repaint());
	}
	
	/**
	 * dispose the JFrame 
	 */
	public void _dispose() {
		this.setVisible(false);
		//dispose();
	}

	@Override
	public void startCTask(String...args) {
		JClient.getInstance().setTipInfo("Broadcast Thread Is Working");
		JClient.threadPool.execute(this);
		SwingUtilities.invokeLater(() -> {
			setVisible(true);
			requestFocus();
		});
	}

	@Override
	public void stopCTask() {
		setTSTATUS(T_STOP);
		//_dispose();
	}

	@Override
	public void run() {
		DataInputStream reader = JClient.getInstance().getReader();
		// JCWriter writer = new JCWriter();
		while ( getTSTATUS() == T_RUN ) {
			try {
				// writer.send(JCmdTools.SEND_HBT_SYMBOL);
				char symbol = reader.readChar();
				/*
				 * Check the symbol type
				 * case SEND_CMD_SYMBOL, then stop the current thread
				 * case SEND_IMG_SYMBOL, then receive the image data from server
				 * 		get the image size then get the image 
				 */
				if ( symbol == JCmdTools.SEND_CMD_SYMBOL ) {
					int cmd = reader.readInt();
					if ( cmd == JCmdTools.SERVER_TASK_STOP_CMD ) {
						break;
					}
				} else if (symbol != JCmdTools.SEND_DATA_SYMBOL) {
					continue;
				}
				
				/*load the mouse location information */
				MOUSE_POS = new Point(reader.readInt(), reader.readInt());
				
				/* the size of the BufferedImage */
				int imgsize = reader.readInt();
				
				/*
				 * the BufferedImage byte data
				 * read the byte data into the buffer
				 * cause cannot read all the data by once when the image is large 
				 */
				byte buffer[] = new byte[imgsize];
				int length = 0;
				while ( length < imgsize ) {
					int readsize = reader.read(buffer, length, imgsize - length);
					if ( readsize > 0 ) length += readsize;
					else break;
				}
				
				/*turn the byte data to a BufferedImage */
				ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
				//ZipInputStream zis = new ZipInputStream(bis);
				//zis.getNextEntry();
				B_IMG = ImageIO.read(bis);
				
				/*repaint the ImageJPanel */
				repaintImageJPanel();
			} catch (IOException e) {
				JClient.getInstance().offLineClear();
				break;
			}
		}
		
		//dispose the JFrame
		_dispose();
		JClient.getInstance().resetJCTask();
		JClient.getInstance().notifyCmdMonitor();
		JClient.getInstance().setTipInfo("Broadcast Thread Is Overed!");
	}
	
	public synchronized void setTSTATUS(int s) {
		TStatus = s;
	}
	
	public synchronized int getTSTATUS() {
		return TStatus;
	}

}
