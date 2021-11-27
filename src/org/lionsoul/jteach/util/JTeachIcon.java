package org.lionsoul.jteach.util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import javax.swing.ImageIcon;


/**
 * quick util.
 * @author  chenxin - chenxin619315@gmail.com
 */
public class JTeachIcon {
	
	/** create an icon image */
	public static ImageIcon Create(String filename) {
		ImageIcon icon = new ImageIcon(JTeachIcon.class.getResource("/res/images/"+filename));
		return icon;
	}
	
	/** resize the BufferedImage */
	public static BufferedImage resize(BufferedImage srcImg, int dst_w, int dst_h) {
		int type = srcImg.getType();

		double sx = (double) dst_w / srcImg.getWidth();
		double sy = (double) dst_h / srcImg.getHeight();

		if (sx > sy) {
			sx = sy;
			dst_w = (int) Math.ceil(sx * srcImg.getWidth());
		} else {
			sy = sx;
			dst_h = (int) Math.ceil(sy * srcImg.getHeight());
		}

		BufferedImage _dst;
		if (type == BufferedImage.TYPE_CUSTOM) { //handmade not a picture
			ColorModel cm = srcImg.getColorModel();
			WritableRaster raster = cm.createCompatibleWritableRaster(dst_w, dst_h);
			boolean alphaPremultiplied = cm.isAlphaPremultiplied();
			_dst = new BufferedImage(cm, raster, alphaPremultiplied, null);
		} else {
			_dst = new BufferedImage(dst_w, dst_h, type);
		}

		Graphics2D g = _dst.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY );
		//g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

		g.drawRenderedImage(srcImg, AffineTransform.getScaleInstance(sx, sy));
		g.dispose();
		return _dst;
	}

	public static BufferedImage resize_2(BufferedImage srcImg, int dst_w, int dst_h) {
		Image bImg = srcImg.getScaledInstance(dst_w, dst_h, Image.SCALE_SMOOTH);
		BufferedImage _dst = new BufferedImage(dst_w, dst_h, srcImg.getType());

		Graphics2D g = _dst.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		g.drawImage(bImg, 0, 0, null);
		g.dispose();
		return _dst;
	}

	public static boolean ImageEquals(BufferedImage image1, BufferedImage image2) {
		int w1 = image1.getWidth();
		int h1 = image1.getHeight();
		int w2 = image2.getWidth();
		int h2 = image2.getHeight();
		if (w1 != w2 || h1 != h2) {
			return false;
		}

		for (int i = 0; i < w1; i += 4) {
			for (int j = 0; j < h1; j += 4) {
				if (image1.getRGB(i, j) != image2.getRGB(i, j)) {
					return false;
				}
			}
		}

		return true;
	}
	
}
