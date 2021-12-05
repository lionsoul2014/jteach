package org.lionsoul.jteach.capture;

import java.awt.*;
import java.awt.image.BufferedImage;

public class RobotScreenCapture extends ScreenCapture {

    private final Robot robot;

    public RobotScreenCapture(Rectangle rect) throws AWTException {
        super(rect);
        robot = new Robot();
    }

    @Override
    public String getDriverName() {
        return "robot";
    }

    @Override
    public BufferedImage capture() {
        return robot.createScreenCapture(rect);
    }
}
