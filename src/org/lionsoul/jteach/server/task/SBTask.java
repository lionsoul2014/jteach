package org.lionsoul.jteach.server.task;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;

import org.lionsoul.jteach.capture.Factory;
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.PacketConfig;
import org.lionsoul.jteach.msg.ScreenMessage;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.server.JServer;
import org.lionsoul.jteach.capture.CaptureException;
import org.lionsoul.jteach.capture.ScreenCapture;


/**
 * Broadcast Sender Task.
 * @author chenxin - chenxin619315@gmail.com
 */
public class SBTask extends JSTaskBase {

	public static final Log log = Log.getLogger(SBTask.class);

	private final ScreenCapture capture;
	private final PacketConfig config;

	public SBTask(JServer server) throws CaptureException {
		super(server);
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.capture = Factory.create(server.config.captureDriver,
				new Rectangle(0, 0, screenSize.width, screenSize.height), server.config);
		this.config = new PacketConfig(true, server.config.compressLevel);
		log.debug("initialized with driver: %s, rect: %s", capture.getDriverName(), capture.getRect());
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
			/* check and the client list */
			if (beanList.size() == 0) {
				server.println(log.getInfo("task overed due to empty client list"));
				break;
			}

			// grab screen capture
			long start = System.currentTimeMillis();
			final BufferedImage img;
			try {
				img = capture.capture();
			} catch (CaptureException e) {
				server.println(log.getError("failed to capture screen due to %s", e.getClass().getName()));
				continue;
			}
			log.debug("end grab, cost: %dms", System.currentTimeMillis() - start);

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
			start = System.currentTimeMillis();
			final Packet p;
			try {
				p = new ScreenMessage(capture.getDriver(), MouseInfo.getPointerInfo().getLocation(), img).encode(config);
			} catch (IOException e) {
				server.println(log.getError("failed to decode screen image"));
				continue;
			}

			log.debug("end encode, cost: %dms", System.currentTimeMillis() - start);

			// remember the current img as the last
			// image the for the next round
			// B_IMG = img;

			/* send the image data to the clients */
			start = System.currentTimeMillis();
			synchronized (beanList) {
				final Iterator<JBean> it = beanList.iterator();
				while (it.hasNext()) {
					final JBean bean = it.next();
					try {
						// append the Message
						// to the current bean
						log.debug("put to bean %s with pool size=%d", bean.getHost(), bean.sendPoolSize());
						bean.put(p);
					} catch (IllegalAccessException e) {
						server.println(bean.getClosedError());
						it.remove();
					}
				}
			}

			log.debug("end send, cost: %dms", System.currentTimeMillis() - start);

			// try {
			// 	Thread.sleep(16);
			// } catch (InterruptedException e) {
			// 	server.println(log.getWarn("sleep interrupted due to %s", e.getClass().getName()));
			// }
		}
	}

}