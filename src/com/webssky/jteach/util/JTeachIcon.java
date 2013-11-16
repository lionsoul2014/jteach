package com.webssky.jteach.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import javax.swing.ImageIcon;


/**
 * quick util. <br />
 * @author  chenxin - chenxin619315@gmail.com <br />
 * {@link http://www.webssky.com}
 */
public class JTeachIcon {
	
	/**
	 * create an icon image 
	 */
	public static ImageIcon Create(String filename) {
		ImageIcon icon = new ImageIcon(JTeachIcon.class.getResource("/res/images/"+filename));
		return icon;
	}
	
	/**
	 * resize the BufferedImage 
	 */
	public static BufferedImage resize(BufferedImage source, int dst_w, int dst_h) { 
		int type = source.getType(); 
		BufferedImage _dst = null; 
		double sx = (double) dst_w / source.getWidth(); 
		double sy = (double) dst_h / source.getHeight(); 

		if( sx > sy ) { 
			sx = sy; 
			dst_w = (int)(sx * source.getWidth()); 
		} else { 
			sy = sx; 
			dst_h = (int)(sy * source.getHeight()); 
		} 
		
		if (type == BufferedImage.TYPE_CUSTOM) { //handmade not a picture
			ColorModel cm = source.getColorModel(); 
			WritableRaster raster = cm.createCompatibleWritableRaster(dst_w, dst_h); 
			boolean alphaPremultiplied = cm.isAlphaPremultiplied(); 
			_dst = new BufferedImage(cm, raster, alphaPremultiplied, null); 
		} else 
			_dst = new BufferedImage(dst_w, dst_h, type); 
			Graphics2D g = _dst.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY );
			//g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); 
		       
			g.drawRenderedImage(source, AffineTransform.getScaleInstance(sx, sy)); 
			g.dispose();
		    return _dst; 
		}  
}
