package application;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import util.Misc;

public class JoyInfo {

	final static int maxInputs = 42;
	final static int inputsSpacing = 36;

	XInputDevice14 joystick;
	Group root;
	Stage stageMain;
	Stage stageInputs;
	Scene sceneMain;
	Canvas canvasDraw;
	Canvas canvasMain;
	Canvas canvasInputs;
	GraphicsContext gcDraw;
	GraphicsContext gcInputs;
	Image joyImage;
	int bps;
	int bps2;
	int maxBps;
	int joyID;
	long bpsCTime;
	boolean resized;
	boolean hideInfos;
	boolean close;
	boolean inputTranspBg;
	long lastButtonPressedCTime;
	long lastAnyPressedCTime;
	
	static List<XInputButton> buttons = new ArrayList<>(Arrays.asList(
			XInputButton.DPAD_LEFT, XInputButton.DPAD_UP,
			XInputButton.DPAD_RIGHT, XInputButton.DPAD_DOWN,
			null, null, null, null, null, null, null, null,
			XInputButton.A, XInputButton.B, XInputButton.X, XInputButton.Y,
			XInputButton.LEFT_SHOULDER, XInputButton.RIGHT_SHOULDER, null, null,
			XInputButton.LEFT_THUMBSTICK, XInputButton.RIGHT_THUMBSTICK,
			XInputButton.BACK, XInputButton.START, XInputButton.GUIDE_BUTTON));
	
	Map<Integer, Float> axesLastVal;
	List<DebugInput> pressedButtons;
	public List<Integer> currentHoldButtons;
	
	@SuppressWarnings("serial")
	static Map<Integer, Integer> povBins = new HashMap<>() {{
		put(0, 101);
		put(1, 102);
		put(2, 104);
		put(3, 108);
		put(4, 201);
		put(5, 202);
		put(6, 204);
		put(7, 208);
		put(8, 301);
		put(9, 302);
		put(10, 304);
		put(11, 308);
	}};
	
	JoyInfo(Stage stage, XInputDevice14 joystick, int joyID) {
		this.joystick = joystick;
		this.joyID = joyID;
		axesLastVal = new HashMap<>();
		loadJoystickEvents();
		for (int n = 0; n < 6; n++)
			axesLastVal.put(n, 0f);
		pressedButtons = new ArrayList<>();
		currentHoldButtons = new ArrayList<>();
		close = false;
		hideInfos = false;
		inputTranspBg = false;
		stageInputs = null;
		canvasInputs = null;
		gcInputs = null;
		lastButtonPressedCTime = 0;
		lastAnyPressedCTime = 0;
		bps = 0;
		bps2 = 0;
		maxBps = 0;
		bpsCTime = System.currentTimeMillis();
		stageMain = stage;
		root = new Group();
		joyImage = ImageUtils.removeBgColor(new Image("file:.\\images\\joystick.png"), (Color)Paint.valueOf("#FF00FF")); 
		canvasMain = new Canvas(joyImage.getWidth(),joyImage.getHeight() - 55);
		canvasMain.getGraphicsContext2D().setEffect(new BoxBlur(1, 1, 0));
		canvasDraw = new Canvas(joyImage.getWidth(),joyImage.getHeight() - 55);
		canvasDraw.getGraphicsContext2D().setEffect(new BoxBlur(1, 1, 0));
		root.getChildren().add(canvasMain);
		gcDraw = canvasDraw.getGraphicsContext2D();
		sceneMain = new Scene(root);
		stageMain.setTitle("Joystick Tester");
		stageMain.setScene(sceneMain);
		stageMain.setResizable(false);
		stageMain.setOnCloseRequest(e -> close());
		sceneMain.setOnKeyPressed(event -> {
			System.out.println(event.getCode());
			if (event.getCode() == KeyCode.MULTIPLY)
				hideInfos = !hideInfos; 
			else if (event.getCode() == KeyCode.SPACE)
				Main.reallign();
			else if (event.getCode() == KeyCode.ESCAPE)
				Main.close();
			else if (event.getCode() == KeyCode.SUBTRACT || event.getCode() == KeyCode.MINUS)
				Main.decreaseZoom();
			else if (event.getCode() == KeyCode.EQUALS || event.getCode() == KeyCode.ADD)
				Main.increaseZoom();
			else if (event.getCode() == KeyCode.DIVIDE)
				toogleDebugInputs();
		});
		stageMain.setScene(sceneMain);
		setTitle();
		stageMain.show();

		canvasMain.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				int x = (int)(event.getX() / Main.getZoom());
				int y = (int)(event.getY() / Main.getZoom());
				if (new Rectangle(15, 275, 100, 20).contains(new Point(x, y)))
					joystick.setVibration(65535, 0);
				if (new Rectangle((int)canvasDraw.getWidth() - 115, 275, 100, 20).contains(new Point(x, y)))
					joystick.setVibration(0, 65535);
			}
		});
    
		mainLoop();
	}
	
	void toogleDebugInputs() {
		if (stageInputs == null) {
			stageInputs = new Stage();
			canvasInputs = new Canvas(inputsSpacing * maxInputs, inputsSpacing + 10);
			gcInputs = canvasInputs.getGraphicsContext2D();
			Scene scene = new Scene(new Group(canvasInputs));
			if (inputTranspBg) {
				stageInputs.initStyle(StageStyle.TRANSPARENT);
				scene.setFill(Color.TRANSPARENT);
			}
			scene.setOnKeyPressed(event -> {
				if (event.getCode() == KeyCode.DIVIDE)
					toogleDebugInputs();
				else if (event.getCode() == KeyCode.SUBTRACT || event.getCode() == KeyCode.MINUS) {
					inputTranspBg = !inputTranspBg;
					toogleDebugInputs();
					toogleDebugInputs();
				}
			});
			stageInputs.setTitle("Debug Inputs");
			stageInputs.setScene(scene);
			stageInputs.setResizable(false);
			stageInputs.show();
			currentHoldButtons.clear();
			pressedButtons.clear();
			lastAnyPressedCTime = 0;
			inputsLoop();
		}
		else {
			stageInputs.close();
			stageInputs = null;
			canvasInputs = null;
			gcInputs = null;
		}
	}

	protected void setTitle()
		{ stageMain.setTitle("Joypad tester (XInput " + this.joyID + ") (" + (joystick.isConnected() ? "Conected" : "Disconnected") + ")"); }

	void setResized (boolean value) {
		resized = value;
		setWindowsPos(0, 0);
		mainLoop();
	}
	
	void loadJoystickEvents() {
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
				if (stageInputs != null) {
					if (pressed)
						onPressButton(buttons.indexOf(button));
					else
						onReleasedButton(buttons.indexOf(button));
				}
			}

		};
		joystick.addListener(listener);
	}
	
	int checkDiagonal(int povId) {
		if (povBins.containsKey(povId))
			povId = povBins.get(povId);
		if (povId > 100) {
			int start = (int)(povId / 100) * 100, idSum = 0;
			for (int n = 1; n <= 8; n++)
				if (currentHoldButtons.contains(start + n))
					idSum += n;
			if (idSum > 0)
				return start + idSum;
		}
		return povId;
	}
	
	boolean isValidDirection(int dir) {
		dir = dir % 100;
		return (dir == 1 || dir == 2 || dir == 3 || dir == 4 || dir == 6 || dir == 8 || dir == 9 || dir == 12);
	}

	void onPressButton(int buttonId) {
		System.out.println(buttonId);
		if (lastAnyPressedCTime != 0 && System.currentTimeMillis() - lastAnyPressedCTime >= 500)
			addPressedButtons(0);
		if (povBins.containsKey(buttonId))
			buttonId = povBins.get(buttonId);
		currentHoldButtons.add(buttonId);
		int diagonal = checkDiagonal(buttonId);
		if (buttonId > 100 && !isValidDirection(diagonal)) {
			currentHoldButtons.remove(Integer.valueOf(buttonId));
			return;
		}
		boolean atSameTime = !pressedButtons.isEmpty() &&  
													pressedButtons.get(pressedButtons.size() - 1).getFirstButtonId() < 100 &&
													buttonId < 100 && System.currentTimeMillis() - lastButtonPressedCTime < 33;
		if (!atSameTime)
			lastButtonPressedCTime = System.currentTimeMillis();
		addPressedButtons(diagonal != buttonId ? diagonal : buttonId, atSameTime);
		lastAnyPressedCTime = System.currentTimeMillis();
	}
	
	void addPressedButtons(int buttonId, boolean atSameTime) {
		DebugInput debugInput = new DebugInput(buttonId);
		if (!atSameTime && pressedButtons.size() * inputsSpacing  >= canvasInputs.getWidth())
			pressedButtons.remove(0);
		if (atSameTime)
			pressedButtons.get(pressedButtons.size() - 1).addButtonId(buttonId);
		else
			pressedButtons.add(debugInput);
	}
	
	void addPressedButtons(int buttonId)
		{ addPressedButtons(buttonId, false); }

	void onReleasedButton(int buttonId) {
		if (povBins.containsKey(buttonId))
			buttonId = povBins.get(buttonId);
		currentHoldButtons.remove(Integer.valueOf(buttonId));
		int diagonal = checkDiagonal(buttonId);
		if (diagonal != buttonId)
			addPressedButtons(diagonal);
	}

	void drawText(String text, Color color, float x, float y) {
		double w = gcDraw.getLineWidth();
		Paint c = gcDraw.getStroke();
		gcDraw.setLineWidth(1);
		gcDraw.setStroke(color);
		gcDraw.strokeText(text, x, y);
		gcDraw.setLineWidth(w);
		gcDraw.setStroke(c);
	}

	void mainLoop() {
		int incy = hideInfos ? 25 : 0;
		gcDraw.setFill(Color.BLACK);
		gcDraw.fillRect(0, 0, (int)canvasDraw.getWidth(), (int)canvasDraw.getHeight());
		gcDraw.setFill(Paint.valueOf("#B97A57"));
		if (hideInfos)
			gcDraw.fillRect(0, 0, (int)canvasDraw.getWidth(), incy);
		gcDraw.fillRect(0, 270, (int)canvasDraw.getWidth(), (int)canvasDraw.getHeight() - 270);
		gcDraw.setLineWidth(3);
		joystick.poll();
		XInputComponents components = joystick.getComponents();
		XInputButtons buttons = components.getButtons();
		XInputAxes axes = components.getAxes();
		// DPADS
		gcDraw.setFill(buttons.up ? Color.GREEN : Color.BLACK);
		drawPolygon(gcDraw, 76, incy + 78, 108, incy + 78, 108, incy + 106, 92, incy + 127, 76, incy + 106);
		gcDraw.setFill(buttons.right ? Color.GREEN : Color.BLACK);
		drawPolygon(gcDraw, 140, incy + 112, 112, incy + 112, 92, incy + 127, 112, incy + 142, 140, incy + 142);
		gcDraw.setFill(buttons.down ? Color.GREEN : Color.BLACK);
		drawPolygon(gcDraw, 76, incy + 174, 76, incy + 146, 92, incy + 127, 108, incy + 146, 108, incy + 174);
		gcDraw.setFill(buttons.left ? Color.GREEN : Color.BLACK);
		drawPolygon(gcDraw, 44, incy + 110, 70, incy + 110, 92, incy + 127, 70, incy + 142, 44, incy + 142);
		// BACK and SELECT
		gcDraw.setFill(buttons.back ? Color.GREEN : Color.BLACK);
		gcDraw.fillRect(157, incy + 56, 19, 33);
		gcDraw.setFill(buttons.start ? Color.GREEN : Color.BLACK);
		gcDraw.fillRect(376, incy + 56, 19, 33);
		// PS (If available)
		if (XInputDevice14.isGuideButtonSupported()) {
			gcDraw.setFill(buttons.guide ? Color.GREEN : Color.BLACK);
			gcDraw.fillRect(255, incy + 194, 41, 41);
		}
		// L/R SHOULDERS
		gcDraw.setFill(buttons.lShoulder ? Color.GREEN : Color.BLACK);
		gcDraw.fillRect(64, incy + 26, 63, 17);
		gcDraw.setFill(buttons.rShoulder ? Color.GREEN : Color.BLACK);
		gcDraw.fillRect(421, incy + 26, 63, 17);
		// L/R TRIGGERS
		updateAxesLastValue(axes);
		gcDraw.setFill(axes.lt > 0 ? Color.GREEN : Color.BLACK);
		gcDraw.fillRect(66, incy + 6, 61 * axes.lt, 17);
		gcDraw.setFill(axes.rt > 0 ? Color.GREEN : Color.BLACK);
		gcDraw.fillRect(423, incy + 6, 61 * axes.rt, 17);
		// X, CIRCLE, SQUARE and TRIANGLE
		gcDraw.setFill(buttons.a ? Color.GREEN : Color.BLACK);
		gcDraw.fillRect(438, incy + 148, 41, 41);
		gcDraw.setFill(buttons.b ? Color.GREEN : Color.BLACK);
		gcDraw.fillRect(479, incy + 107, 41, 41);
		gcDraw.setFill(buttons.x ? Color.GREEN : Color.BLACK);
		gcDraw.fillRect(397, incy + 107, 41, 41);
		gcDraw.setFill(buttons.y ? Color.GREEN : Color.BLACK);
		gcDraw.fillRect(438, incy + 66, 41, 41);

		gcDraw.drawImage(joyImage, 0, 0, 550, 275, 0, incy, 550, 275);
		// L3/R3
		gcDraw.drawImage(buttons.lThumb ? ImageUtils.tintImage(joyImage, Color.GREEN, 0.5f) : joyImage, 66, 291, 71, 72, 141 + axes.lx * 28, incy + 170 - axes.ly * 28, 72, 72);
		gcDraw.drawImage(buttons.rThumb ? ImageUtils.tintImage(joyImage, Color.GREEN, 0.5f) : joyImage, 66, 291, 71, 72, 339 + axes.rx * 28, incy + 170 - axes.ry * 28, 72, 72);
		// X/Y AXES and L/R TRIGGERS VALUES
		if (!hideInfos) {
			drawText(String.format("X: %.3f\nY: %.3f", axes.lx, axes.ly), Color.BLACK, 155, 290);
			drawText(String.format("X: %.3f\nY: %.3f", axes.rx, axes.ry), Color.BLACK, 353, 290);
			drawText(String.format("Value: %.3f", axes.lt), Color.BLACK, 135, 20);
			drawText(String.format("Value: %.3f", axes.rt), Color.BLACK, 350, 20);
			// Test motors
			gcDraw.setFill(Color.DARKOLIVEGREEN);
			gcDraw.fillRect(15, 275, 100, 20);
			gcDraw.setStroke(Color.BLACK);
			gcDraw.strokeRect(15, 275, 100, 20);
			drawText("Test left motor", Color.BLACK, 27, 289);
			gcDraw.setFill(Color.DARKOLIVEGREEN);
			gcDraw.fillRect(canvasDraw.getWidth() - 115, 275, 100, 20);
			gcDraw.setStroke(Color.BLACK);
			gcDraw.strokeRect(canvasDraw.getWidth() - 115, 275, 100, 20);
			drawText("Test right motor", Color.BLACK, (int)canvasDraw.getWidth() - 110, 289);
		}

		if (!resized) {
			resized = true;
			canvasMain.setWidth(canvasDraw.getWidth() * Main.getZoom());
			canvasMain.setHeight(canvasDraw.getHeight() * Main.getZoom());
			stageMain.setWidth(canvasMain.getWidth() + 14);
			stageMain.setHeight(canvasMain.getHeight() + 36);
		}
		
		CanvasUtils.copyCanvas(canvasDraw, canvasMain);
		
		if (System.currentTimeMillis() >= bpsCTime) {
			bps = bps2;
			if (bps > maxBps)
				maxBps = bps;
			bps2 = 0;
			bpsCTime = System.currentTimeMillis() + 1000;
		}
		
	 	Misc.sleep(1);
	 	
	 	if (!close) {
	 		inputsLoop();
	 		Misc.runLater(() -> mainLoop());
	 	}
	 	else
	 		Main.close();
	}
	
	private void updateAxesLastValue(XInputAxes axes) {
		float[][] aXes = {
				{axes.lx, axes.ly, axes.lx, axes.ly, axes.rx, axes.ry, axes.rx, axes.ry, axes.lt, axes.rt},
				{-1f, 1f, 1f, -1f, -1f, 1f, 1f, -1f, 1f, 1f},
				{4, 5, 6, 7, 8, 9, 10, 11, 18, 19}
		};
		for (int n = 0; n < aXes[0].length; n++) {
			int buttonId = (int)aXes[2][n];
			float currentValue = aXes[0][n];
			float previewValue = axesLastVal.containsKey(buttonId) ? axesLastVal.get(buttonId) : 0f;
			axesLastVal.put(buttonId, currentValue);
			if (aXes[1][n] > 0) {
				if (previewValue < 0.67f && currentValue >= 0.67f)
					onPressButton(buttonId);
				else if (previewValue >= 0.67f && currentValue < 0.67f)
					onReleasedButton(buttonId);
			}
			else {
				if (previewValue >= -0.67f && currentValue < -0.67f)
					onPressButton(buttonId);
				else if (previewValue < -0.67f && currentValue >= -0.67f)
					onReleasedButton(buttonId);
			}
		}
	}

	void inputsLoop() {
		if (stageInputs != null) {
			gcInputs.setFill(inputTranspBg ? Color.rgb(0,  0,  0, 0) : Color.BLACK);
			if (inputTranspBg)
				gcInputs.clearRect(0, 0, canvasInputs.getWidth(), canvasInputs.getHeight());
			else
				gcInputs.fillRect(0, 0, canvasInputs.getWidth(), canvasInputs.getHeight());
			for (int n = 0; n < pressedButtons.size(); n++) {
				DebugInput debugInput = pressedButtons.get(n);
				if (debugInput.totalButtons() == 1)
					gcInputs.drawImage(debugInput.getImage(), 2 + n * inputsSpacing, (canvasInputs.getHeight() - inputsSpacing) / 2);
				else
					for (int x = 0, y = 0, z = 0, t = debugInput.totalButtons(); z < 4 && z < t; z++) {
						gcInputs.drawImage(debugInput.getImage(z), 2 + n * inputsSpacing + x * inputsSpacing / 2, (canvasInputs.getHeight() - inputsSpacing) / 2 + y * inputsSpacing / 2 + (t < 3 ? inputsSpacing / 4 : 0), debugInput.getImage(z).getWidth() / 2, debugInput.getImage(z).getHeight() / 2);
						if (++x == 2)
							{ x = 0; y++; }
					}
			}
		}
	}

	void drawPolygon(GraphicsContext gc, double ... points) {
		double[] x = new double[points.length / 2];
		double[] y = new double[points.length / 2];
		for (int n = 0, i = 0; n < points.length; n += 2, i++) {
			x[i] = points[n];
			y[i] = points[n + 1];
		}		
		gc.fillPolygon(x, y, x.length);
	}

	void setWindowsPos(int x, int y) {
		stageMain.setX(x);
		stageMain.setY(y);
	}
	
	Stage getMainStage()
		{ return stageMain; }

	void close() {
		if (!close) {
			close = true;
			stageMain.close();
			if (stageInputs != null)
				stageInputs.close();
		}
	}
	
}

class DebugInput {
	
	List<WritableImage> images;
	int firstButtonId;
	
	public DebugInput(int firstButtonId) {
		this.firstButtonId = firstButtonId;
		images = new ArrayList<>(Arrays.asList(Main.buttonsImages.get(firstButtonId)));
	}
	
	public void addButtonId(int buttonId)
		{ images.add(Main.buttonsImages.get(buttonId)); }
	
	public int getFirstButtonId()
		{ return firstButtonId; }
	
	public int totalButtons()
		{ return images.size(); }
	
	public WritableImage getImage()
		{ return getImage(0); }
	
	public WritableImage getImage(int i) {
		if (i >= images.size())
			throw new RuntimeException(i + " - Invalid button image index (Max indexi is " + (images.size() - 1) + ")");
		return images.get(i);
	}

}