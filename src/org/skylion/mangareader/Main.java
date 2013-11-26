package org.skylion.mangareader;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.skylion.mangareader.gui.MainGUI;


public class Main {
	public static void main(String[] args) {
		try{
			//Loads the Nimbus look and feel
			//Retrieval method is convulted, but stable.
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		 MainGUI gui = new MainGUI();
         gui.setVisible(true);
         gui.pack();//Sizes the Frame
         gui.setExtendedState(gui.getExtendedState()|JFrame.MAXIMIZED_BOTH );//Makes it full screen
	}

}

