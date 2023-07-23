package core;

import java.awt.AWTException;
import java.awt.event.InputEvent;
import java.util.List;

import core.dto.Operation;


public class Robot {
    private static java.awt.Robot robot;
    static {
        try {
            robot = new java.awt.Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void processOperation(Operation operation) {
        switch (operation.getName()) {
            case "move":
                move(operation.getArgs());
                break;
            case "click":
                click(operation.getArgs());
                break;
            default:
                System.out.println("Unsupported operation: " + operation.getName());
                break;
        }
    }

    private static void move(List<String> args) {
        int x = Integer.parseInt(args.get(0));
        int y = Integer.parseInt(args.get(1));
        robot.mouseMove(x, y);
        sleep(10);
    }

    private static void click(List<String> args) {
        int buttons = 0;
        switch (args.get(0)) {
            case "left":
                buttons = InputEvent.BUTTON1_DOWN_MASK;
                break;
            default:
                System.out.println("Unsupported mouse button: " + args.get(0));
                break;
        }

        robot.mousePress(buttons);
        sleep(10);
        robot.mouseRelease(buttons);
        sleep(10);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
