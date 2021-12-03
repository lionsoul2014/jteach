package org.lionsoul.jteach.server.task;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;

import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.ScreenMessage;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.server.JServer;


/**
 * Broadcast Sender Task.
 * @author chenxin - chenxin619315@gmail.com
 */
public class SBTask extends JSTaskBase {
	
	public static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
	public static final Log log = Log.getLogger(SBTask.class);

	private final Robot robot;

	public SBTask(JServer server) throws AWTException {
		super(server);
		robot = new Robot();
	}

	@Override
	public void addClient(JBean bean) {
		try {
			boolean res = bean.offer(Packet.COMMAND_BROADCAST_START);
			if ( !res ) {
				server.lnPrintln(log.getDebug("failed to add new client %s", bean.getHost()));
			} else {
				beanList.add(bean);
				server.lnPrintln(log.getDebug("add a new client %s", bean.getHost()));
			}
		} catch (IllegalAccessException e) {
			server.lnPrintln(bean.getClosedError());
		}
	}

	@Override
	public boolean _before() {
		if (beanList.size() == 0) {
			server.println("empty client list");
			return false;
		}

		// send broadcast start cmd to all the beans;
		final Iterator<JBean> it = beanList.iterator();
		while ( it.hasNext() ) {
			final JBean bean = it.next();
			try {
				bean.offer(Packet.COMMAND_BROADCAST_START);
			} catch (IllegalAccessException e) {
				bean.reportClosedError();
				it.remove();
			}
		}

		return true;
	}

	@Override
	public void _run() {
		// BufferedImage B_IMG = null;
		while ( getStatus() == T_RUN ) {
			//load img
			final BufferedImage img = robot.createScreenCapture(
				new Rectangle(SCREEN_SIZE.width, SCREEN_SIZE.height)
			);

			/*
			 * we need to check the image
			 * if over 98% of the picture is the same
			 * and there is no necessary to send the picture
			 */
			// if ( B_IMG == null ) {
			// 	B_IMG = img;
			// } else if (JTeachIcon.ImageEquals(B_IMG, img) ) {
			// 	continue;
			// }


			/* encode the screen message */
			final Packet p;
			try {
				p = new ScreenMessage(MouseInfo.getPointerInfo().getLocation(), img).encode();
			} catch (IOException e) {
				server.println(log.getError("failed to decode screen image"));
				continue;
			}

			// remember the current img as the last
			// image the for the next round
			// B_IMG = img;


			/* send the image data to the clients */
			synchronized (beanList) {
				final Iterator<JBean> it = beanList.iterator();
				while (it.hasNext()) {
					final JBean bean = it.next();
					try {
						// append the Message
						// to the current bean
						bean.put(p);
					} catch (IllegalAccessException e) {
						server.println(bean.getClosedError());
						it.remove();
					}
				}
			}

			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				server.println(log.getWarn("sleep interrupted due to %s", e.getClass().getName()));
			}
		}
	}

}