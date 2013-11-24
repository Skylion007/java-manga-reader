package org.skylion.mangareader;

import javax.swing.JFrame;
import javax.swing.UIManager;

import org.skylion.mangareader.gui.MainGUI;


public class Main {
	public static void main(String[] args) {
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		 MainGUI gui = new MainGUI();
         gui.setVisible(true);
         gui.pack();//Sizes the Frame
         gui.setExtendedState(gui.getExtendedState()|JFrame.MAXIMIZED_BOTH );//Makes it full screen
	}
}

