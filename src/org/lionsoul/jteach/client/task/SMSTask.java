package org.lionsoul.jteach.client.task;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.lionsoul.jteach.capture.ScreenCapture;
import org.lionsoul.jteach.client.JClient;
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.ScreenMessage;
import org.lionsoul.jteach.util.ImageUtil;


/**
 * Image send thread for Screen monitor.
 * 
 * @author chenxin - chenxin619315@gmail.com
 */
public class SMSTask extends JCTaskBase {
	
	private static final Log log = Log.getLogger(SMSTask.class);

	private Robot robot = null;
	public  final Rectangle screenSize;

	public SMSTask(JClient client) {
		super(client);
		try {
			robot = new Robot();
		} catch (AWTException e) {
			JOptionPane.showMessageDialog(null, "Fail to create Robot Object",
					"JTeach:", JOptionPane.ERROR_MESSAGE);
		}

		final Dimension winSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.screenSize = new Rectangle(winSize.width, winSize.height);
	}

	@Override
	public boolean _before(String...args) {
		return true;
	}

	@Override
	public void _run() {
		BufferedImage B_IMG = null;
		while ( getStatus() == T_RUN ) {
			try {
				/* get the screen image */
				final BufferedImage img = robot.createScreenCapture(screenSize);

				if ( B_IMG == null ) {
					B_IMG = img;
				} else if (ImageUtil.ImageEquals(B_IMG, img) ) {
					continue;
				}

				/* encode the screen image */
				final Packet p;
				try {
					p = new ScreenMessage(ScreenCapture.ROBOT_DRIVER,
							MouseInfo.getPointerInfo().getLocation(),
							img, ScreenCapture.IMAGEIO_POLICY, ScreenCapture.DEFAULT_FORMAT,
							ScreenCapture.DEFAULT_COMPRESSION_QUALITY).encode();
				} catch (IOException e) {
					log.error("failed to decode screen image");
					continue;
				}

				// reset the backup image
				B_IMG = img;

				/* send the image byte data to server */
				bean.send(p);
			} catch (IOException e) {
				log.error("task is overed due to %s", e.getClass().getName());
				bean.clear();
				break;
			} catch (IllegalAccessException e) {
				bean.reportClosedError();
				break;
			}
		}
	}

}
