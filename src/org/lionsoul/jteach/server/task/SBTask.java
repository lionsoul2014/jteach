package org.lionsoul.jteach.server.task;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.lionsoul.jteach.capture.Factory;
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.msg.PacketConfig;
import org.lionsoul.jteach.msg.ScreenMessage;
import org.lionsoul.jteach.server.JServer;
import org.lionsoul.jteach.capture.CaptureException;
import org.lionsoul.jteach.capture.ScreenCapture;
import org.lionsoul.jteach.util.ImageUtil;


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
	public void _run() {
		BufferedImage B_IMG = null;
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

			/*
			 * check and filter the duplicate image
			 * if over 98% of the picture is the same
			 * and there is no necessary to send the picture */
			if (server.config.filterDupImg) {
				if ( B_IMG == null ) {
					B_IMG = img;
				} else if (ImageUtil.ImageEquals(B_IMG, img) ) {
					log.debug("ignore duplicate image");
					continue;
				}
			}
			log.debug("end grab, cost: %dms", System.currentTimeMillis() - start);


			/* encode the screen message */
			start = System.currentTimeMillis();
			final Packet p;
			try {
				p = new ScreenMessage(capture.getDriver(),
						MouseInfo.getPointerInfo().getLocation(),
						img, server.config.imgEncodePolicy, server.config.imgFormat,
						server.config.imgCompressionQuality).encode(config);
			} catch (IOException e) {
				server.println(log.getError("failed to decode screen image"));
				continue;
			}

			log.debug("end encode, cost: %dms", System.currentTimeMillis() - start);

			// remember img as the last image the for the next round
			B_IMG = img;

			/* send the image data to the clients */
			start = System.currentTimeMillis();
			final int okCount = send(p);
			log.debug("end send, ok_count: %d, cost: %dms", okCount, System.currentTimeMillis() - start);
		}
	}

}