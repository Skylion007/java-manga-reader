package org.skylion.mangareader;

import java.awt.Color;
import java.awt.Font;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.skylion.mangareader.gui.MainGUI;
import org.skylion.mangareader.util.SplashWindow;



public class Main {
	public static void main(String[] args) {
		try {
			new URL("jar:file://dummy.jar!/").openConnection().setDefaultUseCaches(false);//Disable caching
		}
		catch(Exception ex){
			//Will never happen
		}
		Color neptune = new Color(18,55,63);
		Font police = new Font("Tahoma", Font.BOLD, 12);
		Color teal = new Color(122,216,247);
		try{
			//Loads the Nimbus look and feel
			//Retrieval method is convulted, but stable.
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					UIManager.getLookAndFeelDefaults().put("Button.background", neptune);
					UIManager.getLookAndFeelDefaults().put("Button.font", police);
					UIManager.getLookAndFeelDefaults().put("Button.textForeground", teal);
					break;
				}
			}
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		SplashWindow.splash("Janga is now loading...", police.deriveFont(32f), teal, Color.BLACK);
		MainGUI gui = new MainGUI();
		SplashWindow.disposeSplash();
		gui.setVisible(true);
		gui.pack();//Sizes the Frame
		gui.setExtendedState(gui.getExtendedState()|JFrame.MAXIMIZED_BOTH );//Makes it full screen
	}

}

