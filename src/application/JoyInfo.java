package application;

import java.awt.Point;
import java.awt.Rectangle;

import com.github.strikerx3.jxinput.XInputAxes;
import com.github.strikerx3.jxinput.XInputButtons;
import com.github.strikerx3.jxinput.XInputComponents;
import com.github.strikerx3.jxinput.XInputDevice14;
import com.github.strikerx3.jxinput.enums.XInputButton;
import com.github.strikerx3.jxinput.listener.SimpleXInputDeviceListener;
import com.github.strikerx3.jxinput.listener.XInputDeviceListener;

import gui.util.CanvasUtils;
import gui.util.ImageUtils;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import util.Misc;

public class JoyInfo {

	private XInputDevice14 joystick;
	private Group root;
	private Stage mainStage;
	private Scene mainScene;
	private Canvas drawCanvas;
	private Canvas mainCanvas;
	private GraphicsContext drawGC;
	private Image joyImage;
	private int bps;
	private int bps2;
	private int maxBps;
	private int joyID;
	private long bpsCTime;
	private boolean resized;
	private boolean hideInfos;
	private boolean close;
	
	public JoyInfo(Stage stage, XInputDevice14 joystick, int joyID) {
		this.joystick = joystick;
		this.joyID = joyID;
		loadJoystickEvents();
		close = false;
		hideInfos = false;
		bps = 0;
		bps2 = 0;
		maxBps = 0;
		bpsCTime = System.currentTimeMillis();
		mainStage = stage;
		root = new Group();
		joyImage = ImageUtils.removeBgColor(new Image("joystick.png"), (Color)Paint.valueOf("#FF00FF")); 
		mainCanvas = new Canvas(joyImage.getWidth(),joyImage.getHeight() - 55);
		mainCanvas.getGraphicsContext2D().setEffect(new BoxBlur(1, 1, 0));
		drawCanvas = new Canvas(joyImage.getWidth(),joyImage.getHeight() - 55);
		drawCanvas.getGraphicsContext2D().setEffect(new BoxBlur(1, 1, 0));
		root.getChildren().add(mainCanvas);
		drawGC = drawCanvas.getGraphicsContext2D();
		mainScene = new Scene(root);
		mainStage.setTitle("Joystick Tester");
		mainStage.setScene(mainScene);
		mainStage.setResizable(false);
		mainStage.setScene(mainScene);
		mainScene.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.MULTIPLY)
				hideInfos = !hideInfos; 
			if (event.getCode() == KeyCode.SPACE)
				Main.reallign();
			if (event.getCode() == KeyCode.ESCAPE)
				Main.close();
			if (event.getCode() == KeyCode.SUBTRACT || event.getCode() == KeyCode.MINUS)
				Main.decreaseZoom();
			if (event.getCode() == KeyCode.EQUALS || event.getCode() == KeyCode.ADD)
				Main.increaseZoom();
				
		});
		mainStage.setScene(mainScene);
		setTitle();
		mainStage.show();
		XInputDeviceListener listener = new SimpleXInputDeviceListener() {
			@Override
			public void connected() {
				setTitle();
			}

			@Override
			public void disconnected() {
				setTitle();
			}

			@Override
			public void buttonChanged(final XInputButton button, final boolean pressed) {
				bps2++;
			}
		};
		joystick.addListener(listener);

		mainCanvas.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				int x = (int)(event.getX() / Main.getZoom());
				int y = (int)(event.getY() / Main.getZoom());
				if (new Rectangle(15, 275, 100, 20).contains(new Point(x, y)))
					joystick.setVibration(65535, 0);
				if (new Rectangle((int)drawCanvas.getWidth() - 115, 275, 100, 20).contains(new Point(x, y)))
					joystick.setVibration(0, 65535);
			}
		});
    
		mainLoop();
	}
	
	protected void setTitle()
		{ mainStage.setTitle("Joypad tester (XInput " + this.joyID + ") (" + (joystick.isConnected() ? "Conected" : "Disconnected") + ")"); }

	public void setResized (boolean value) {
		resized = value;
		setWindowsPos(0, 0);
		mainLoop();
	}
	
	private void loadJoystickEvents() {
	}

	private void drawText(String text, Color color, float x, float y) {
		double w = drawGC.getLineWidth();
		Paint c = drawGC.getStroke();
		drawGC.setLineWidth(1);
		drawGC.setStroke(color);
		drawGC.strokeText(text, x, y);
		drawGC.setLineWidth(w);
		drawGC.setStroke(c);
	}

	private void mainLoop() {
		int incy = hideInfos ? 25 : 0;
		drawGC.setFill(Color.BLACK);
		drawGC.fillRect(0, 0, (int)drawCanvas.getWidth(), (int)drawCanvas.getHeight());
		drawGC.setFill(Paint.valueOf("#B97A57"));
		if (hideInfos)
			drawGC.fillRect(0, 0, (int)drawCanvas.getWidth(), incy);
		drawGC.fillRect(0, 270, (int)drawCanvas.getWidth(), (int)drawCanvas.getHeight() - 270);
		drawGC.setLineWidth(3);
		joystick.poll();
		XInputComponents components = joystick.getComponents();
		XInputButtons buttons = components.getButtons();
		XInputAxes axes = components.getAxes();
		// DPADS
		drawGC.setFill(buttons.up ? Color.GREEN : Color.BLACK);
		drawPolygon(drawGC, 76, incy + 78, 108, incy + 78, 108, incy + 106, 92, incy + 127, 76, incy + 106);
		drawGC.setFill(buttons.right ? Color.GREEN : Color.BLACK);
		drawPolygon(drawGC, 140, incy + 112, 112, incy + 112, 92, incy + 127, 112, incy + 142, 140, incy + 142);
		drawGC.setFill(buttons.down ? Color.GREEN : Color.BLACK);
		drawPolygon(drawGC, 76, incy + 174, 76, incy + 146, 92, incy + 127, 108, incy + 146, 108, incy + 174);
		drawGC.setFill(buttons.left ? Color.GREEN : Color.BLACK);
		drawPolygon(drawGC, 44, incy + 110, 70, incy + 110, 92, incy + 127, 70, incy + 142, 44, incy + 142);
		// BACK and SELECT
		drawGC.setFill(buttons.back ? Color.GREEN : Color.BLACK);
		drawGC.fillRect(157, incy + 56, 19, 33);
		drawGC.setFill(buttons.start ? Color.GREEN : Color.BLACK);
		drawGC.fillRect(376, incy + 56, 19, 33);
		// PS (If available)
		if (XInputDevice14.isGuideButtonSupported()) {
			drawGC.setFill(buttons.guide ? Color.GREEN : Color.BLACK);
			drawGC.fillRect(255, incy + 194, 41, 41);
		}
		// L/R SHOULDERS
		drawGC.setFill(buttons.lShoulder ? Color.GREEN : Color.BLACK);
		drawGC.fillRect(64, incy + 26, 63, 17);
		drawGC.setFill(buttons.rShoulder ? Color.GREEN : Color.BLACK);
		drawGC.fillRect(421, incy + 26, 63, 17);
		// L/R TRIGGERS
		drawGC.setFill(axes.lt > 0 ? Color.GREEN : Color.BLACK);
		drawGC.fillRect(66, incy + 6, 61 * axes.lt, 17);
		drawGC.setFill(axes.rt > 0 ? Color.GREEN : Color.BLACK);
		drawGC.fillRect(423, incy + 6, 61 * axes.rt, 17);
		// X, CIRCLE, SQUARE and TRIANGLE
		drawGC.setFill(buttons.a ? Color.GREEN : Color.BLACK);
		drawGC.fillRect(438, incy + 148, 41, 41);
		drawGC.setFill(buttons.b ? Color.GREEN : Color.BLACK);
		drawGC.fillRect(479, incy + 107, 41, 41);
		drawGC.setFill(buttons.x ? Color.GREEN : Color.BLACK);
		drawGC.fillRect(397, incy + 107, 41, 41);
		drawGC.setFill(buttons.y ? Color.GREEN : Color.BLACK);
		drawGC.fillRect(438, incy + 66, 41, 41);

		drawGC.drawImage(joyImage, 0, 0, 550, 275, 0, incy, 550, 275);
		// X/Y AXES
		drawGC.drawImage(buttons.lThumb ? ImageUtils.tintImage(joyImage, Color.GREEN, 0.5f) : joyImage, 66, 291, 71, 72, 141 + axes.lx * 28, incy + 170 - axes.ly * 28, 72, 72);
		drawGC.drawImage(buttons.rThumb ? ImageUtils.tintImage(joyImage, Color.GREEN, 0.5f) : joyImage, 66, 291, 71, 72, 339 + axes.rx * 28, incy + 170 - axes.ry * 28, 72, 72);
		// X/Y AXES and L/R TRIGGERS VALUES
		if (!hideInfos) {
			drawText(String.format("X: %.3f\nY: %.3f", axes.lx, axes.ly), Color.BLACK, 155, 290);
			drawText(String.format("X: %.3f\nY: %.3f", axes.rx, axes.ry), Color.BLACK, 353, 290);
			drawText(String.format("Value: %.3f", axes.lt), Color.BLACK, 135, 20);
			drawText(String.format("Value: %.3f", axes.rt), Color.BLACK, 350, 20);
			// Test motors
			drawGC.setFill(Color.DARKOLIVEGREEN);
			drawGC.fillRect(15, 275, 100, 20);
			drawGC.setStroke(Color.BLACK);
			drawGC.strokeRect(15, 275, 100, 20);
			drawText("Test left motor", Color.BLACK, 27, 289);
			drawGC.setFill(Color.DARKOLIVEGREEN);
			drawGC.fillRect(drawCanvas.getWidth() - 115, 275, 100, 20);
			drawGC.setStroke(Color.BLACK);
			drawGC.strokeRect(drawCanvas.getWidth() - 115, 275, 100, 20);
			drawText("Test right motor", Color.BLACK, (int)drawCanvas.getWidth() - 110, 289);
		}

		if (!resized) {
			resized = true;
			mainCanvas.setWidth(drawCanvas.getWidth() * Main.getZoom());
			mainCanvas.setHeight(drawCanvas.getHeight() * Main.getZoom());
			mainStage.setWidth(mainCanvas.getWidth() + 14);
			mainStage.setHeight(mainCanvas.getHeight() + 36);
		}
		
		CanvasUtils.copyCanvas(drawCanvas, mainCanvas);
		
		if (System.currentTimeMillis() >= bpsCTime) {
			bps = bps2;
			if (bps > maxBps)
				maxBps = bps;
			bps2 = 0;
			bpsCTime = System.currentTimeMillis() + 1000;
		}
		
	 	Misc.sleep(1);
	 	
	 	if (!close)
	 		Misc.runLater(() -> mainLoop());
	 	else
	 		mainStage.close();
	}
	
	private void drawPolygon(GraphicsContext gc, double ... points) {
		double[] x = new double[points.length / 2];
		double[] y = new double[points.length / 2];
		for (int n = 0, i = 0; n < points.length; n += 2, i++) {
			x[i] = points[n];
			y[i] = points[n + 1];
		}		
		gc.fillPolygon(x, y, x.length);
	}

	public void setWindowsPos(int x, int y) {
		mainStage.setX(x);
		mainStage.setY(y);
	}
	
	public Stage getMainStage()
		{ return mainStage; }

	public void close() {
		close = true;
	}
	
}