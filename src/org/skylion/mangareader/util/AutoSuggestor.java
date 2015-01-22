package org.skylion.mangareader.util;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A decoration for JTextFields which allow for a dropdown menu to show possible suggestion based off the values
 * from an arrayList. Matching is non-strict.
 * @author David, Aaron Gokaslan
 */
public class AutoSuggestor {

    private final JTextField textField;
    private final Window container;
    private JPanel suggestionsPanel;
    private JWindow autoSuggestionPopUpWindow;
    private String typedWord;
    private final List<String> dictionary = new ArrayList<>();
    private int tW, tH;
	private int lastFocusableIndex = 0;
    private DocumentListener documentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent de) {
            checkForAndShowSuggestions();
        }

        @Override
        public void removeUpdate(DocumentEvent de) {
            checkForAndShowSuggestions();
        }

        @Override
        public void changedUpdate(DocumentEvent de) {
            checkForAndShowSuggestions();
        }
    };
    private final Color suggestionsTextColor;
    private final Color suggestionFocusedColor;

    public AutoSuggestor(JTextField textField, Window mainWindow, List<String> words, Color popUpBackground, Color textColor, Color suggestionFocusedColor, float opacity) {
        this.textField = textField;
        this.suggestionsTextColor = textColor;
        this.container = mainWindow;
        this.suggestionFocusedColor = suggestionFocusedColor;
        this.textField.getDocument().addDocumentListener(documentListener);
      
        //WorkAround for anomalous behavior on Macs
        //Disables automatic highlighting when focused upon
        this.textField.addFocusListener(new FocusListener(){
        	 
        		public void focusGained(FocusEvent e) {
        			getTextField().getHighlighter().removeAllHighlights();
        		}

        	    @Override
        	    public void focusLost(FocusEvent e) {
        	    	getTextField().getHighlighter().removeAllHighlights();
        	    	if(!(e.getOppositeComponent() instanceof Window || e.getOppositeComponent() instanceof SuggestionLabel) && isPopUpVisible()){
        	    		showPopUp(false);
        	    	}
        	    }

        });
        
        this.textField.addHierarchyListener(new HierarchyListener(){//Hides the PopUp if parent is hidden
        	
        	private Container previousParent = getTextField().getParent();
        	private ComponentListener cl = new ComponentAdapter(){
        		
        		@Override
        		public void componentHidden(ComponentEvent ce){
        			showPopUp(false);
        		}
        		
        	};
        	
			@Override
			public void hierarchyChanged(HierarchyEvent he) {
				if(previousParent != null){
					previousParent.removeComponentListener(cl);
				}
				if(he.getChangedParent() != null){
					he.getChangedParent().addComponentListener(cl);
				}
				previousParent = he.getChangedParent();//Updates the Previous Parent
			}
        	
        });
        
        mainWindow.addComponentListener(new ComponentAdapter(){
            @Override
            public void componentResized(ComponentEvent e) {
            	checkForAndShowSuggestions();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            	checkForAndShowSuggestions();
            }
        });
        
        setDictionary(words);

        typedWord = "";
        tW = 0;
        tH = 0;

        autoSuggestionPopUpWindow = new JWindow(mainWindow);
        autoSuggestionPopUpWindow.setOpacity(opacity);

        suggestionsPanel = new JPanel();
        suggestionsPanel.setLayout(new GridLayout(0, 1));
        suggestionsPanel.setBackground(popUpBackground);        

        addKeyBindingToRequestFocusInPopUpWindow();
    }
    
    

    private void addKeyBindingToRequestFocusInPopUpWindow() {
        textField.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "Down released");
        textField.getActionMap().put("Down released", new AbstractAction() {
            /**
			 * 
			 */
			private static final long serialVersionUID = 4304541929636803547L;

			@Override
            public void actionPerformed(ActionEvent ae) {//focuses the first label on popwindow
                resetLabelFocus();
				for (int i = 0; i < suggestionsPanel.getComponentCount(); i++) {
                    if (suggestionsPanel.getComponent(i) instanceof SuggestionLabel) {
                        ((SuggestionLabel) suggestionsPanel.getComponent(i)).setFocused(true);
                        autoSuggestionPopUpWindow.toFront();
                        autoSuggestionPopUpWindow.requestFocusInWindow();
                        suggestionsPanel.requestFocusInWindow();
                        suggestionsPanel.getComponent(i).requestFocusInWindow();
                        break;
                    }
                }
            }
        });
        suggestionsPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "Down released");
        suggestionsPanel.getActionMap().put("Down released", new AbstractAction() {
            /**
			 * Auto-generated serial long
			 */
			private static final long serialVersionUID = 7355352173460888575L;

            @Override
            public void actionPerformed(ActionEvent ae) {//allows scrolling of labels in pop window (I know very hacky for now :))

                List<SuggestionLabel> sls = getAddedSuggestionLabels();
                int max = sls.size();

                if (max > 1) {//more than 1 suggestion
                    for (int i = 0; i < max; i++) {
                        SuggestionLabel sl = sls.get(i);
                        if (sl.isFocused()) {
                            if (lastFocusableIndex == max - 1) {
                                lastFocusableIndex = 0;
                                sl.setFocused(false);
                                autoSuggestionPopUpWindow.setVisible(false);
                                setFocusToTextField();
                                checkForAndShowSuggestions();//fire method as if document listener change occured and fired it

                            } else {
                                sl.setFocused(false);
                                lastFocusableIndex = i;
                            }
                        } else if (lastFocusableIndex <= i) {
                            if (i < max) {
                                sl.setFocused(true);
                                autoSuggestionPopUpWindow.toFront();
                                autoSuggestionPopUpWindow.requestFocusInWindow();
                                suggestionsPanel.requestFocusInWindow();
                                suggestionsPanel.getComponent(i).requestFocusInWindow();
                                lastFocusableIndex = i;
                                break;
                            }
                        }
                    }
                } else {//only a single suggestion was given
                    autoSuggestionPopUpWindow.setVisible(false);
                    checkForAndShowSuggestions();//fire method as if document listener change occured and fired it
                    setFocusToTextField();
                    
                }
            }
        });
        suggestionsPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), "Up released");
        suggestionsPanel.getActionMap().put("Up released", new AbstractAction() {
            /**
			 * Auto-generated serial long
			 */
			private static final long serialVersionUID = 7355352173460888575L;

            @Override
            public void actionPerformed(ActionEvent ae) {//allows scrolling of labels in pop window (I know very hacky for now :))

                List<SuggestionLabel> sls = getAddedSuggestionLabels();
                int max = sls.size();

                if (max > 1) {//more than 1 suggestion
                    for (int i = max-1; i >= 0; i--) {
                        SuggestionLabel sl = sls.get(i);
                        if (sl.isFocused()) {
                            if (lastFocusableIndex == 0) {
                                sl.setFocused(false);
                                autoSuggestionPopUpWindow.setVisible(false);
                                setFocusToTextField();
                                checkForAndShowSuggestions();//fire method as if document listener change occured and fired it

                            } else {
                                sl.setFocused(false);
                                lastFocusableIndex = i;
                            }
                        } else if (lastFocusableIndex >= i) {
                            if (i >= 0) {
                                sl.setFocused(true);
                                autoSuggestionPopUpWindow.toFront();
                                autoSuggestionPopUpWindow.requestFocusInWindow();
                                suggestionsPanel.requestFocusInWindow();
                                suggestionsPanel.getComponent(i).requestFocusInWindow();
                                lastFocusableIndex = i;
                                break;
                            }
                        }
                    }
                } else {//only a single suggestion was given
                    autoSuggestionPopUpWindow.setVisible(false);
                    checkForAndShowSuggestions();//fire method as if document listener change occured and fired it
                    setFocusToTextField();
                    
                }
            }
        });
                
    }

    private void setFocusToTextField() {
    	container.toFront();
    	container.requestFocusInWindow();
		textField.setFocusable(true);
		textField.setRequestFocusEnabled(true);
		SwingUtilities.invokeLater(new Runnable() {
			void run() {
				textField.requestFocusInWindow();
				textField.requestFocus();
			}
		});	
    }

    public List<SuggestionLabel> getAddedSuggestionLabels() {
        List<SuggestionLabel> sls = new ArrayList<>();
        for (int i = 0; i < suggestionsPanel.getComponentCount(); i++) {
            if (suggestionsPanel.getComponent(i) instanceof SuggestionLabel) {
                SuggestionLabel sl = (SuggestionLabel) suggestionsPanel.getComponent(i);
                sls.add(sl);
            }
        }
        return sls;
    }

    private void checkForAndShowSuggestions() {
        typedWord = getCurrentlyTypedWord();

        suggestionsPanel.removeAll();//remove previos words/jlabels that were added

        //used to calculate size of JWindow as new Jlabels are added
        tW = 0;
        tH = 0;
        
        lastFocusableIndex = 0;//Resets the index

        boolean added = wordTyped(typedWord);

        if (!added) {
            if (autoSuggestionPopUpWindow.isVisible()) {
                autoSuggestionPopUpWindow.setVisible(false);
            }
        } else {
            showPopUpWindow();
            setFocusToTextField();
        }
    }

    protected void addWordToSuggestions(String word) {
    	
    	SuggestionLabel suggestionLabel = new SuggestionLabel(word, suggestionFocusedColor, suggestionsTextColor, this);
    	
        calculatePopUpWindowSize(suggestionLabel);
        
        suggestionsPanel.add(suggestionLabel);
    }

    /**
     * Gets the String currently used. Can be modified to show suggestions for each word.
     * @return
     */
    public String getCurrentlyTypedWord() {
        String text = textField.getText();
        return text;
    }

    private void calculatePopUpWindowSize(JLabel label) {
        //so we can size the JWindow correctly
        if (tW < label.getPreferredSize().width) {
            tW = label.getPreferredSize().width;
        }
        tH += label.getPreferredSize().height;
    }

    private void showPopUpWindow() {
        
    	
    	autoSuggestionPopUpWindow.getContentPane().add(suggestionsPanel);
        autoSuggestionPopUpWindow.setMinimumSize(new Dimension(textField.getWidth() -
        		- textField.getInsets().left - textField.getInsets().right, 30));
        autoSuggestionPopUpWindow.setSize(tW, tH);
        autoSuggestionPopUpWindow.setVisible(true);

        //Calculates the optimal window location
        int windowX = container.getX() + container.getInsets().left + textField.getX() + textField.getMargin().left + 3;
        int windowY = container.getY() + container.getInsets().top - textField.getInsets().bottom + textField.getY() + textField.getHeight();
        				
        autoSuggestionPopUpWindow.setLocation((windowX), windowY);
        autoSuggestionPopUpWindow.setMinimumSize(new Dimension(textField.getWidth() - textField.getInsets().right, 30));
        autoSuggestionPopUpWindow.setSize(textField.getWidth() - textField.getInsets().right, autoSuggestionPopUpWindow.getHeight());
        autoSuggestionPopUpWindow.revalidate();
        autoSuggestionPopUpWindow.repaint();
        
        setFocusToTextField();
    }

    public void setDictionary(List<String> words) {
        dictionary.clear();
        if (words == null) {
            return;//so we can call constructor with null value for dictionary without exception thrown
        }
        //Converts List<String> to remove duplicates
        Set<String> s = new LinkedHashSet<String>(words);
        dictionary.addAll(s);
        Collections.sort(dictionary);//The List works much better when sorted
    }
    
    public List<String> getDictionary(){
    	return dictionary;
    }
    
    public JWindow getAutoSuggestionPopUpWindow() {
        return autoSuggestionPopUpWindow;
    }

    public Window getContainer() {
        return container;
    }

    public JTextField getTextField() {
        return textField;
    }

    public void addToDictionary(String word) {
        dictionary.add(word);
    }

    boolean wordTyped(String typedWord) {

    	if (typedWord.isEmpty()) {
    		return false;
    	}
    	//System.out.println("Typed word: " + typedWord);

    	boolean suggestionAdded = false;



    	for (String word : dictionary) {//get words in the dictionary which we added
    		boolean fullymatches = true;
    		if(typedWord.length()<=word.length()){//Important! Removes harmful exception for words not in list
    			for (int i = 0; i < typedWord.length(); i++) {//each string in the word
    				if (!typedWord.toLowerCase().startsWith(String.valueOf(word.toLowerCase().charAt(i)), i)) {//check for match
    					fullymatches = false;
    					break;
    				}
    			}
    		}
    		else{
    			fullymatches = false;
    		}
    		if (fullymatches) {
    	        if(tH>container.getHeight()-container.getInsets().top-container.getInsets().bottom-
    	        		textField.getY() - textField.getHeight()){
    	        	break;//Prevents the suggestions panel from drawing unused suggestionLabels offscreen
    	        }
    			addWordToSuggestions(word);
    			suggestionAdded = true;
    		}
    	}
    	return suggestionAdded;
    }
    
    public boolean isPopUpVisible(){
    	return this.autoSuggestionPopUpWindow.isVisible();
    }
    
    public void showPopUp(boolean show){
    	this.autoSuggestionPopUpWindow.setVisible(show);
    }
    
    protected void setSelectedSuggestionLabel(SuggestionLabel sl){
    	List<SuggestionLabel> sls = getAddedSuggestionLabels();
    	
    	if(lastFocusableIndex<sls.size() && (sl == null || sls.contains(sl))){
    		sls.get(lastFocusableIndex).setFocused(false);//Resets the index
    	}
    	
    	if(!sls.contains(sl)){
    		return;
    	}
    	
    	sl.setFocused(true);
    	lastFocusableIndex = sls.indexOf(sl);
    }
 
    private void resetLabelFocus(){
    	getAddedSuggestionLabels().get(lastFocusableIndex).setFocused(false);
    	lastFocusableIndex = 0;
    }
}

class SuggestionLabel extends JLabel {

    /**
	 * Auto-generated serial long
	 */
	private static final long serialVersionUID = 1L;

	
	private boolean focused = false;
    private final JWindow autoSuggestionsPopUpWindow;
    private final JTextField textField;
    private final AutoSuggestor autoSuggestor;
    private Color suggestionsTextColor, suggestionBorderColor;

    public SuggestionLabel(String string, final Color borderColor, Color suggestionsTextColor, AutoSuggestor autoSuggestor) {
        super(string);

        this.suggestionsTextColor = suggestionsTextColor;
        this.autoSuggestor = autoSuggestor;
        this.textField = autoSuggestor.getTextField();
        this.suggestionBorderColor = borderColor;
        this.autoSuggestionsPopUpWindow = autoSuggestor.getAutoSuggestionPopUpWindow();

        initComponent();
    }

    private void initComponent() {
        setFocusable(true);
        setForeground(suggestionsTextColor);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                replaceWithSuggestedText();
                autoSuggestionsPopUpWindow.setVisible(false);
                fireTextFieldActionEvents();
            }
            
            @Override
            public void mouseEntered(MouseEvent me) {
            	autoSuggestor.setSelectedSuggestionLabel(SuggestionLabel.this);//Highlights selected label
            }
        });

        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "Enter released");
        getActionMap().put("Enter released", new AbstractAction() {
            /**
        	 * Auto-generated serial long
        	 */
			private static final long serialVersionUID = 2010022788442417401L;

			@Override
            public void actionPerformed(ActionEvent ae) {
                replaceWithSuggestedText();
                autoSuggestionsPopUpWindow.setVisible(false);
                fireTextFieldActionEvents();
            }
        });
    }

    public void setFocused(boolean focused) {
        if (focused) {
            setBorder(new LineBorder(suggestionBorderColor));
        } else {
            setBorder(null);
        }
        repaint();
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }

    private void replaceWithSuggestedText() {
        String suggestedWord = getText();
        String text = textField.getText();
        String typedWord = autoSuggestor.getCurrentlyTypedWord();
        if(text.indexOf(typedWord)==-1){
        	return;
        }
    
        String t = text.substring(0, text.lastIndexOf(typedWord));
        String tmp = t + text.substring(text.lastIndexOf(typedWord)).replace(typedWord, suggestedWord);
        textField.setText(tmp + " ");
    }
    
    private void fireTextFieldActionEvents(){
    	int uniqueId = (int) System.currentTimeMillis();
        String commandName = "Word Replaced";
        for(ActionListener tmp: textField.getActionListeners()){//Manually fires action events.
        	tmp.actionPerformed(new ActionEvent(textField, uniqueId, commandName));
        }
    }
    
}
