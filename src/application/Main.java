package application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.strikerx3.jxinput.XInputDevice14;
import com.github.strikerx3.jxinput.exceptions.XInputNotLoadedException;

import gui.util.ImageUtils;
import javafx.application.Application;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import util.FindFile;

public class Main extends Application {
	
	static List<JoyInfo> joyInfos = new ArrayList<>();
	static float zoom = 0.5f;
	static int maxWindowWidth = 0;
	static int maxWindowHeight = 0;
	static boolean sideBySide = false;
	static boolean close = false;
	public static Map<Integer, WritableImage> buttonsImages;

	@Override
	public void start(Stage stage) {
		try {
			buttonsImages = new HashMap<>();
			FindFile.findFile(".\\images\\inputs\\","*").forEach(e ->
				buttonsImages.put(Integer.parseInt(e.getName().replace(".png", "")), ImageUtils.loadWritableImageFromFile(e)));
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
	
	static void setMaxWindowSize() {
		maxWindowWidth = 0;
		maxWindowHeight = 0;
		for (JoyInfo ji : joyInfos) {
			if (ji.getMainStage().getWidth() > maxWindowWidth)
				maxWindowWidth = (int)ji.getMainStage().getWidth();
			if (ji.getMainStage().getHeight() > maxWindowHeight)
				maxWindowHeight = (int)ji.getMainStage().getHeight();
		}
	}
	
	static void reallign() {
		int x = 0, y = 0;
		for (JoyInfo j : joyInfos) {
			j.setWindowsPos(x, y);
			if (sideBySide && (x += maxWindowWidth) + maxWindowWidth > 1920)
				{ x = 0; y += maxWindowHeight; }
		}
		sideBySide = !sideBySide;
	}
	
	static void close() {
		if (!close) {
			close = true;
			for (JoyInfo j : joyInfos)
				j.close();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}

	static double getZoom()
		{ return zoom; }

	static void decreaseZoom() {
		if (zoom > 0.5f) {
			zoom /= 2;
			for (JoyInfo j : joyInfos)
				j.setResized(false);
			setMaxWindowSize();
		}
	}

	static void increaseZoom() {
		if (zoom < 2)
			zoom *= 2;
		for (JoyInfo j : joyInfos)
			j.setResized(false);
		setMaxWindowSize();
	}

}