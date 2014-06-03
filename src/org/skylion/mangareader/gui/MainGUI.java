package org.skylion.mangareader.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.skylion.mangareader.mangaengine.MangaEdenAPI;
import org.skylion.mangareader.mangaengine.MangaEngine;
import org.skylion.mangareader.mangaengine.MangaHereAPI;
import org.skylion.mangareader.mangaengine.MangaPandaAPI;
import org.skylion.mangareader.mangaengine.MangaReaderAPI;
import org.skylion.mangareader.mangaengine.Prefetcher;
import org.skylion.mangareader.util.AutoSuggestor;
import org.skylion.mangareader.util.HashMapComboBoxModel;
import org.skylion.mangareader.util.ImagePanel;
import org.skylion.mangareader.util.Logger;
import org.skylion.mangareader.util.StringUtil;




//TODO Re-compose code to remove unnecessary JFrame extension
/**
 * 
 * A GUI designed for Janga
 * @author Skylion
 *
 */
public class MainGUI extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6351329658980316434L;

	private Container pane;//Place holder

	/**
	 * User Interface Buttons
	 */
	private JButton next;
	private JButton previous;
	
	private JPanel toolbar;
	private JComboBox<String> chapterSel;
	private JComboBox<String> pageSel;
	private JComboBox<String> engineSel;
	private JButton toggleFullScreen;
	//private JToggleButton toolbarLock;
	
	/**
	 * User commandline
	 */
	private JTextField mangaSelect;//MangaSelect
	private AutoSuggestor autoSelect;//My Autosuggestion Decorator
	private ImagePanel page; //That page currently displayed
	private boolean fullScreen = false;//Is in Fullscreen?
	
	private static final String instructions = "<html><center>" +
			"<p><font size =\"32\">Just type the name of the manga you wish to read in the search bar above."+
			" Use the dropdown menus to navigate between different manga sources, chapters," +
			" and pages.<font size =\"32\"></p>" + "</html></center>";
	
	private JPanel pageUI;
	
	/**
	 * Used to create and store Global Keystroke
	 */
	private HashMap<KeyStroke, Action> actionMap = new HashMap<KeyStroke, Action>();
	
	/**
	 * Used to store different Manga Engines
	 */
	private LinkedHashMap<String, MangaEngine> mangaEngineMap = new LinkedHashMap<String, MangaEngine>();
	
	/**
	 * The Engine used to fetch content from the Manga Website. In this case MangaHere
	 */
	private MangaEngine mangaEngine;

	public MainGUI(){
		super();
		initGUI();
	}

	private void initGUI(){
		this.setTitle("Janga Manga Reader");
		this.setBackground(Color.BLACK);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//Exits the program on close
		
		pane = getContentPane();
		pane.setLayout(new BorderLayout());
		pane.setBackground(Color.BLACK);

		previous = new JButton("Previous Page");
		previous.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				loadPage(mangaEngine.getPreviousPage());
			}
		});
						   
		next = new JButton("Next Page");
		next.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt){
				loadPage(mangaEngine.getNextPage());
			}
		});

		mangaSelect = new JTextField("Type your manga into here");
		mangaSelect.setEditable(true);
		mangaSelect.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				disableNavigation(false);
				loadPage(mangaEngine.getMangaURL(mangaSelect.getText()));
				refreshLists();
			}
		});
		
		//Loads MangaEngines in a separate Thread to reduce start up time
		engineSel = new JComboBox<String>(
				new HashMapComboBoxModel<String, MangaEngine>(mangaEngineMap));
		new Thread(new Runnable(){
			private boolean engineLoadingFailed = false;
			
			@Override
			public void run(){
				
				try {
					mangaEngineMap.put("MangaHere", new MangaHereAPI());
				} catch (IOException ex) {
					catcher(ex);
				}
								
				try {
					mangaEngineMap.put("MangaReader", new MangaReaderAPI());
				} catch(Exception ex){
					catcher(ex);
				}

				try {
					mangaEngineMap.put("MangaPanda", new MangaPandaAPI());
				} catch(Exception ex){
					catcher(ex);
				}

				try {
					mangaEngineMap.put("MangaEden", new MangaEdenAPI());
				} catch(IOException ex){
					catcher(ex);
				}
				
				if(engineLoadingFailed){
					SwingUtilities.invokeLater(new Runnable(){
						@Override
						public void run(){
							JOptionPane.showMessageDialog(MainGUI.this, "ERROR", 
								"Some MangaEngine could not be loaded", JOptionPane.ERROR_MESSAGE);
						}
					});
				}
			}
						
			private void catcher(Exception e){
				Toolkit.getDefaultToolkit().beep();
				Logger.log(e);
				engineLoadingFailed = true;
			}
		}).start();
		
		//Loads the first Manga Engine that loads succesfully
		while(mangaEngine == null){
			try {
				if(mangaEngineMap.keySet().isEmpty()){
					Thread.sleep(100);
				} else {
					String first = mangaEngineMap.keySet().toArray()[0].toString();
					engineSel.setSelectedItem(first);
					mangaEngine = new Prefetcher(this, mangaEngineMap.get(first));
				}
			} catch (InterruptedException e) {
				System.exit(ABORT);
			}
		}

		
		//Wraps the TextField with my custom autosuggestion box
		autoSelect = new AutoSuggestor(mangaSelect, this, mangaEngine.getMangaList(), 
				Color.WHITE.brighter(), Color.BLUE, Color.MAGENTA.darker(), 0.75f);
		
		//Sets up the page
		page = new ImagePanel();
		page.setPreferredSize(getEffectiveScreenSize());
		page.setBackground(Color.BLACK);
		page.setForeground(next.getForeground());
		page.setFont(next.getFont().deriveFont(72f));
		page.setDoubleBuffered(true);
		//User Welcome Screen!
		page.setText("<html><center><p> Welcome to Janga!</p> " +
				"<h1>A Java Manga Reading Application</h1>" + instructions + 
				"<p>Enjoy!</p></center>");
	
		////////////////////////////////////////
		//Constructs components in the toolbar//
		////////////////////////////////////////
		chapterSel = new JComboBox<String>(StringUtil.formatChapterNames(
				mangaEngine.getChapterNames()));
		chapterSel.setToolTipText("Chapter Navigation");
		chapterSel.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (evt.getModifiers() != 0 && evt.getSource() instanceof JComboBox
						 && ((JComboBox<?>)evt.getSource()).isPopupVisible()){
					int index = chapterSel.getSelectedIndex();
					try {
						loadPage(mangaEngine.loadImg(mangaEngine.getChapterList()[index]));
						refreshLists();
						autoSelect.setDictionary(mangaEngine.getMangaList());
						updateStatus();
					} catch (IOException e) {
						Logger.log(e);
					}
				}
			}
		});		
		
		pageSel = generateComboBox("Pg: ", mangaEngine.getPageList().length);
		pageSel.setToolTipText("Page Navigation");
		pageSel.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				//Checks if it wasn't programmatically fired.
				if (evt.getModifiers() != 0 && evt.getSource() instanceof JComboBox
						 && ((JComboBox<?>)evt.getSource()).isPopupVisible()){
					int index = pageSel.getSelectedIndex();
					try {
						loadPage(mangaEngine.loadImg(mangaEngine.getPageList()[index]));
						updateStatus();
					} catch (IOException e) {
						Logger.log(e);
					}
				}
			}
		});
		
		//Initializes mangaEngineSel
		engineSel.setToolTipText("Manga Source Selection");
		engineSel.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (evt.getModifiers() != 0) {//Checks if it wasn't programmatically fired.
					String engineName = (String)engineSel.getSelectedItem();
					try {
						if(mangaEngine instanceof Closeable){
							((Closeable) mangaEngine).close();
						}
						mangaEngine = new Prefetcher(MainGUI.this, mangaEngineMap.get(engineName));
						loadPage((BufferedImage)null);
						page.setText(instructions + "</center></html>");
						disableNavigation(true);
						refreshLists();
						autoSelect.setDictionary(mangaEngine.getMangaList());//Resets the list
					} catch (IOException e) {
						Logger.log(e);
					}
				}
			}
		});
		
		toggleFullScreen = new JButton("F");
		toggleFullScreen.setToolTipText("Toggles FullScreen (CTRL-F)");
		toggleFullScreen.setForeground(Color.WHITE);
		toggleFullScreen.setBackground(Color.MAGENTA.darker().darker());
		toggleFullScreen.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				setFullScreen(!isFullScreen());
			}
		});
		
		/*
  		toolbarLock = new JToggleButton("T");
  		toolbarLock.setToolTipText("Locks toolbar");
  		toolbarLock.setFont(next.getFont());
  		toolbarLock.setForeground(Color.WHITE);
  		toolbarLock.setBackground(Color.LIGHT_GRAY);
  		toggleFullScreen.addActionListener(new java.awt.event.ActionListener() {
  			public void actionPerformed(java.awt.event.ActionEvent evt) {
  				
  			}
  		});
		*/
		
		//Instantionates and sets up asthetics for toolbar
		toolbar = new JPanel() {
			/**
			 * Default Generated Serial UID
			 */
			private static final long serialVersionUID = -2560322159063958032L;

			@Override
			protected void paintComponent(Graphics graphics) { // Adds a gradient to the JPanel
				super.paintComponent(graphics);
				Graphics2D g2d = (Graphics2D) graphics;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);

				GradientPaint gp = new GradientPaint(0, 0, 
						getBackground().brighter(), 0, getHeight(),
						getBackground().darker());

				g2d.setPaint(gp);
				g2d.fillRect(0, 0, getWidth(), getHeight());
			}            
		};
		toolbar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		toolbar.setBackground(Color.DARK_GRAY.brighter());
		//toolbar.setBackground(next.getBackground().brighter());
		
		//Adds Items to toolbar
		toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
		toolbar.add(mangaSelect);
		toolbar.add(engineSel);
		toolbar.add(chapterSel);
		toolbar.add(pageSel);
		toolbar.add(toggleFullScreen);
		//End of Toolbar setup
		
		next.setPreferredSize(previous.getPreferredSize());////Makes the buttons the same size	
		
		//Sets up the User Interface
		
		pageUI = new JPanel(new BorderLayout(0,0));
		pageUI.setBackground(Color.BLACK);
		pageUI.add(next, BorderLayout.EAST);
		pageUI.add(previous, BorderLayout.WEST);
		pageUI.add(page, BorderLayout.CENTER);
				
		pane.add(toolbar, BorderLayout.NORTH);
		pane.add(pageUI, BorderLayout.CENTER);
		
		//toolbar.setBackground(toolbar.getParent().getBackground());
		
		/*
		// Experimental auto-hide functionality
  		final JToggleButton lockToolbar = new JToggleButton("V");
  		lockToolbar.setSelected(true);
  		lockToolbar.setForeground(Color.WHITE);
  		lockToolbar.setBackground(Color.GREEN.darker());
  		lockToolbar.setFont(next.getFont());
  		toolbar.add(lockToolbar);
  		pane.addMouseMotionListener(new MouseAdapter() {
  		    public void mouseMoved (MouseEvent me) {
  		        boolean was = toolbar.isVisible();
  		    	if (!was && toolbar.getBounds().contains(me.getPoint())) {//If mouseOver
  					toolbar.setVisible(true);
  		        } else if(!lockToolbar.isSelected()){
  		            toolbar.setVisible(false);
  		        }
  		    	if(was != toolbar.isVisible()){
  		    		revalidate();
  		    	}
  		    }
  		});
		*/
		
		disableNavigation(true);
		initKeyboard();
	}
			
	private void initKeyboard(){		
		Action nextPageAction = new AbstractAction(){
			/**
			 * AutoGenerated UID
			 */
			private static final long serialVersionUID = -3381019543157339629L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if(pageSel.isEnabled()){
					next.doClick();
				}
			}
		};

		Action previousPageAction = new AbstractAction(){
			/**
			 * AutoGenerated UID
			 */
			private static final long serialVersionUID = 1148536792558547221L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if(pageSel.isEnabled()){
					previous.doClick();
				}
			}
		};
		
		Action toggleTaskbarAction = new AbstractAction(){
			/**
			 * AutoGenerated UID
			 */
			private static final long serialVersionUID = 1148566792558547221L;

			@Override
			public void actionPerformed(ActionEvent e) {
				toolbar.setVisible(!toolbar.isVisible());
			}
		};
		
		Action toggleFullScreenAction = new AbstractAction(){
			/**
			 * AutoGenerated UID
			 */
			private static final long serialVersionUID = 1148566792585247221L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setFullScreen(!isFullScreen());
			}
		};

		Action escapeFullScreenAction = new AbstractAction(){
			/**
			 * AutoGenerated UID
			 */
			private static final long serialVersionUID = 1148563792585247221L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if(isFullScreen()){
					setFullScreen(false);
				}
			}
		};
		
		//Loads Bindings into JFrame
		this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "NEXT");
		this.getRootPane().getActionMap().put("NEXT", nextPageAction);

		this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "PREV");
		this.getRootPane().getActionMap().put("PREV", previousPageAction);
		
		//Loads Keyboard Commands
		actionMap.put(KeyStroke.getKeyStroke("ctrl released UP"), nextPageAction);
		actionMap.put(KeyStroke.getKeyStroke("ctrl released DOWN"), previousPageAction);
		actionMap.put(KeyStroke.getKeyStroke("released PAGE_UP"), nextPageAction);
		actionMap.put(KeyStroke.getKeyStroke("released PAGE_DOWN"), previousPageAction);
		actionMap.put(KeyStroke.getKeyStroke("control T"), toggleTaskbarAction);
		actionMap.put(KeyStroke.getKeyStroke("control F"), toggleFullScreenAction);
		actionMap.put(KeyStroke.getKeyStroke("pressed ESCAPE"), escapeFullScreenAction);
		
		//Overrides the KeyboardFocusManager to allow Global key commands.
		KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		kfm.addKeyEventDispatcher( new KeyEventDispatcher() {
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
				//System.out.println(keyStroke);
				if ( actionMap.containsKey(keyStroke) ) {
					final Action a = actionMap.get(keyStroke);
					final ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), null );
					SwingUtilities.invokeLater( new Runnable() {
						@Override
						public void run() {
							a.actionPerformed(ae);
						}
					} ); 
					return true;
				}
				return false;
			}
		});
		
	}
	
	/**
	 * Toggles full screen mode. Requires a lot of references to the JFrame.
	 */
	public void setFullScreen(boolean fullScreen){
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice dev = env.getDefaultScreenDevice();//Gets the main screen
		if(!fullScreen){//Checks if a full screen application isn't open
			this.dispose();//Restarts the JFrame
			this.setVisible(false);
			this.setResizable(true);//Re-enables resize-ability.
			this.setUndecorated(false);//Adds title bar back
			this.setVisible(true);//Shows restarted JFrame
			this.removeMouseListener(macWorkAround);
			this.pack();
			this.setExtendedState(this.getExtendedState()|JFrame.MAXIMIZED_BOTH);//Returns to maximized state
			this.fullScreen = false;
		}
		else{
			this.dispose();//Restarts the JFrame
			this.setResizable(false);//Disables resizing else causes bugs
			this.setUndecorated(true);//removes title bar
			this.setVisible(true);//Makes it visible again
			this.revalidate();
			this.setSize(Toolkit.getDefaultToolkit().getScreenSize());
			try{
				dev.setFullScreenWindow(this);//Makes it full screen
				if(System.getProperty("os.name").contains("Mac OS X")){
					this.setVisible(false);
					this.setVisible(true);
					this.addMouseListener(macWorkAround);
				}
				this.repaint();
				this.revalidate();
			}
			catch(Exception e){
				dev.setFullScreenWindow(null);//Fall back behavior
			}
			this.requestFocus();
			this.fullScreen = true;
		}
	}
	
	private final MouseAdapter macWorkAround = new MouseAdapter(){
		@Override
		public void mouseClicked(MouseEvent e){
			MainGUI.this.setVisible(false);
			MainGUI.this.setVisible(true);
		}
	};
	
	public boolean isFullScreen(){
		return fullScreen;
	}
	
	/*
	// Alternate fullscreen
  	@Override
  	public void setExtendedState(int state){
  		if(state == JFrame.MAXIMIZED_BOTH){
  			this.dispose();
  			this.setUndecorated(true);
  			this.setVisible(true);
  		}
  		else{
  			this.dispose();
  			this.setUndecorated(false);
  			this.setVisible(true);
  		}
  		super.setExtendedState(state);
  	}
	*/
	
	/**
	 * Loads the page of the Manga specified by the URL
	 * @param newURL
	 */
	private void loadPage(String URL){
		int attempts = 0;
		while(attempts < 3)
		try {
			if(mangaEngine.getCurrentURL().equals(URL)){
				return;//No need to waste time reloading a page.
			}
			loadPage(mangaEngine.loadImg(URL));
			page.setText(null);
			updateStatus();
			return;
		} catch (IOException e) {
			Logger.log(e);
			if(e instanceof SocketTimeoutException) {
				// Do nothing allow reattempt.
				// This doesn't do nothing...
			}
			page.loadImage(null);
			page.setText("<html><p><center>An error has occured :(</p>" +
					"<h1>Sorry, the currently requested title or page number could not be found. " +
					"Please try a different page, chapter, or manga source. " +
					"If you encountered this error while searching for a manga title, " +
					"the manga you currently have requested is most likely licensed; " +
					"hence, not available in your country and/or region. " +
					"We apologize for this inconvenience.</h1> " +
					"<h1>Thank you for your cooperation!</h1></center></html>");
			Toolkit.getDefaultToolkit().beep();
			return;
		}
	}

	private void loadPage(BufferedImage image){
		page.loadImage(image);
	}

	/**
	 * Gets the effective screen size: the screen size without toolbar
	 * @return
	 */
	private Dimension getEffectiveScreenSize(){
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
		int taskBarSize = scnMax.bottom;
		return new Dimension(screenSize.width, screenSize.height-taskBarSize);
	}

	/**
	 * Spawns a combo box
	 * @param prefix the prefix you want to have before the combobox
	 * @param size The size of the ComboBox
	 * @return The ComboBox
	 */
	private JComboBox<String> generateComboBox(String prefix, int size){
		String[] out = new String[size];
		for(int i = 0; i<size; i++){
			out[i] = (prefix + (i+1));
		}
		return new JComboBox<String>(out);
	}
	
	private void disableNavigation(boolean disabled){
		boolean enabled = !disabled;
		pageSel.setEnabled(enabled);
		chapterSel.setEnabled(enabled);
		next.setVisible(enabled);
		previous.setVisible(enabled);
	}
	
	/**
	 * Updates the ComboBoxes for chapters and pages
	 */
	private void refreshLists(){
		//Work around to forcefully refresh
		chapterSel.setModel(new DefaultComboBoxModel<String>(mangaEngine.getChapterNames()));
		String[] pages = new String[mangaEngine.getPageList().length];
		for(int i = 0; i<mangaEngine.getPageList().length; i++){
			pages[i] = ("Pg: " + (i+1));
		}
		System.out.print("UPDATED ");
		pageSel.setModel(new DefaultComboBoxModel<String>(pages));
	}
	
	private void updateStatus(){
		String chapter = "Ch: " + mangaEngine.getCurrentChapNum();
		String currentPage = "Pg: " + mangaEngine.getCurrentPageNum();
		chapterSel.getModel().setSelectedItem(chapter);//Work around to forcefully change combobox
		pageSel.getModel().setSelectedItem(currentPage);
	}

}