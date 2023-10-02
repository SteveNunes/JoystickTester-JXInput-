package application;

import java.util.ArrayList;
import java.util.List;

import com.github.strikerx3.jxinput.XInputDevice14;
import com.github.strikerx3.jxinput.exceptions.XInputNotLoadedException;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
	
	private static List<JoyInfo> joyInfos = new ArrayList<>();
	private static float zoom = 0.5f;
	private static int maxWindowWidth = 0;
	private static int maxWindowHeight = 0;
	private static boolean sideBySide = false;

	@Override
	public void start(Stage stage) {
		try {
			XInputDevice14 devices[] = XInputDevice14.getAllDevices();
			for (int n = 0; n < devices.length && n == 0; n++)
				joyInfos.add(new JoyInfo(new Stage(), devices[n], n));
			setMaxWindowSize();
		}
		catch (XInputNotLoadedException e) {
			e.printStackTrace();
			stage.close();
		}
	}
	
	private static void setMaxWindowSize() {
		maxWindowWidth = 0;
		maxWindowHeight = 0;
		for (JoyInfo ji : joyInfos) {
			if (ji.getMainStage().getWidth() > maxWindowWidth)
				maxWindowWidth = (int)ji.getMainStage().getWidth();
			if (ji.getMainStage().getHeight() > maxWindowHeight)
				maxWindowHeight = (int)ji.getMainStage().getHeight();
		}
	}
	
	public static void reallign() {
		int x = 0, y = 0;
		for (JoyInfo j : joyInfos) {
			j.setWindowsPos(x, y);
			if (sideBySide && (x += maxWindowWidth) + maxWindowWidth > 1920)
				{ x = 0; y += maxWindowHeight; }
		}
		sideBySide = !sideBySide;
	}
	
	public static void close() {
		for (JoyInfo j : joyInfos)
			j.close();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

	public static double getZoom()
		{ return zoom; }

	public static void decreaseZoom() {
		if (zoom > 0.25f) {
			zoom /= 2;
			for (JoyInfo j : joyInfos)
				j.setResized(false);
			setMaxWindowSize();
		}
	}

	public static void increaseZoom() {
		zoom *= 2;
		for (JoyInfo j : joyInfos)
			j.setResized(false);
		setMaxWindowSize();
	}

}