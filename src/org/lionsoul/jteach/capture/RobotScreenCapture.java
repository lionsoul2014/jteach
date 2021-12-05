package org.lionsoul.jteach.capture;

import org.lionsoul.jteach.config.TaskConfig;

import java.awt.*;
import java.awt.image.BufferedImage;

public class RobotScreenCapture extends ScreenCapture {

    private final Robot robot;

    public RobotScreenCapture(Rectangle rect, TaskConfig config) throws AWTException {
        super(rect, config);
        robot = new Robot();
    }

    @Override
    public int getDriver() {
        return ScreenCapture.ROBOT_DRIVER;
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
