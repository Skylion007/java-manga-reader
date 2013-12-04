package org.skylion.mangareader.mangaengine;

import java.awt.BorderLayout;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.skylion.mangareader.util.StretchIconHQ;;

/**
 * A class that wraps a MangaEngine and prefetches from it to improve speed.
 * The class also adds a progress bar to the JFrame to show how the prefetching is going.
 * @author Skylion
 *
 */
public class Prefetcher implements MangaEngine{

	private MangaEngine mangaEngine;//Current MangaEngine
	private StretchIconHQ[] pages;//Current Images
	private String[] pageURLs; //URLs corresponding to current images
	private String mangaName;//CurrentMangaName

	private JProgressBar progressBar;//The Progress Monitor
	private JFrame parent; //The Parent Component to display the loading bar in
	private Task task;//The Swing Worker that handles repaint
	
	/**
	 * Constructor
	 * @param window The JFrame you want to add the progressbar to
	 * @param mangaEngine The Engine you want to prefetch from
	 */
	public Prefetcher(JFrame window, MangaEngine mangaEngine){
		this.mangaEngine = mangaEngine;
		parent = window;
		mangaName = mangaEngine.getMangaName();
		pageURLs = mangaEngine.getPageList();
		progressBar = new JProgressBar(0, mangaEngine.getPageList().length);
		prefetch();
	}

	/**
	 * Performs the prefetching
	 */
	public void prefetch (){
		mangaName = mangaEngine.getMangaName();
		pageURLs = mangaEngine.getPageList();
		pages = new StretchIconHQ[pageURLs.length];
		progressBar.setValue(0);
		progressBar.setMaximum(pageURLs.length);
		progressBar.setStringPainted(true);
		if(task!=null && !task.isDone() && !task.isCancelled()){//Cancels previous task before starting a new one.
			task.cancel(true);
		}
		//System.out.print("PREFETCHING" + mangaEngine.getCurrentURL());
		task = new Task();
		task.addPropertyChangeListener(new PropertyChangeListener(){
		    /**
		     * Invoked when task's progress property changes.
		     */
		    @Override
			public void propertyChange(PropertyChangeEvent evt) {//Updates Progressbar
		        if ("progress" == evt.getPropertyName() ) {
		            int progress = (Integer) evt.getNewValue();
		            parent.repaint();
		            progressBar.setValue(progress);
		            progressBar.setString("Loading Page: " + progress + " of " + progressBar.getMaximum());
		        }
		    }
		});
		task.execute();
	}
	
	/**
	 * Checks solely whether or not the URL is in the database.
	 * @param URL The URL you want to check
	 * @return True if in database false otherwise.
	 */
	private boolean isCached(String URL){
		for(int i = 0; i<pageURLs.length; i++){
			if(pageURLs[i].equals(URL) && pages[i]!=null){
				System.out.println("Cached");
				return true;
			}
		}
		System.out.println("Prefetching because of" + URL);
		return false;
	}

	/**
	 * 
	 * @param URL
	 * @return
	 */
	private StretchIconHQ fetch(String URL){
		if(!isCached(URL) || isNewChapter()){
			try {
				StretchIconHQ icon = loadImg(mangaEngine.getNextPage());
				prefetch();
				return icon;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		else {
			return pages[mangaEngine.getCurrentPageNum()];
		}
	}
	
	public boolean isNewChapter(){
		String nextPage = mangaEngine.getNextPage();
		for(String chapter: mangaEngine.getChapterList()){
			if(chapter.equals(nextPage)){
				return true;
			}
		}
		return false;
	}

	@Override
	public String getCurrentURL() {
		return mangaEngine.getCurrentURL();
	}

	@Override
	public void setCurrentURL(String url){
		mangaEngine.setCurrentURL(url);
	}

	@Override
	public StretchIconHQ loadImg(String url) throws Exception {
		if(isCached(url)){
			mangaEngine.setCurrentURL(url);
			return fetch(url);
		}
		else{
			StretchIconHQ out =  mangaEngine.loadImg(url);
			prefetch();
			return out;
		}
	}
	
	@Override
	public BufferedImage getImage(String url) throws Exception {
		return mangaEngine.getImage(url);
	}

	@Override
	public String getNextPage() {
		//Prevents the User from going to a page that hasn't been fetched yet
		String currentURL = mangaEngine.getCurrentURL();
		String nextPage = mangaEngine.getNextPage();
		if(isCached(nextPage) || task == null || task.isCancelled() || task.isDone()){
			return nextPage;
		}
		else{
			Toolkit.getDefaultToolkit().beep();
			return currentURL;
		}
	}

	@Override
	public String getPreviousPage() {
		return mangaEngine.getPreviousPage();
	}

	@Override
	public boolean isValidPage(String url) {
		return mangaEngine.isValidPage(url);
	}

	@Override
	public List<String> getMangaList() {
		return mangaEngine.getMangaList();
	}

	@Override
	public String getMangaName() {
		return mangaName;
	}

	@Override
	public String[] getChapterList() {
		return mangaEngine.getChapterList();
	}

	@Override
	public String[] getPageList() {
		return pageURLs;
	}

	@Override
	public String getMangaURL(String mangaName) {
		return mangaEngine.getMangaURL(mangaName);
	}

	@Override
	public int getCurrentPageNum() {
		return mangaEngine.getCurrentPageNum();
	}

	@Override
	public int getCurrentChapNum() {
		return mangaEngine.getCurrentChapNum();
	}
	
	@Override
	public String[] getChapterNames(){
		return mangaEngine.getChapterNames();
	}
	
	/**
	 * Where the actual prefetching happens
	 * @author Skylion
	 */
	class Task extends SwingWorker<Void, Void> {
		
		public Task(){
			parent.getContentPane().add(progressBar, BorderLayout.SOUTH);//Adds ProgressBar to bottom
			parent.revalidate();//Refreshes JFRame
			parent.repaint();
		}
		/*
		 * Main task. Executed in background thread.
		 */
		@Override
		public Void doInBackground() {
			for(int i = 0; i<pageURLs.length && !this.isCancelled(); i++){
				try{		
					pages[i] = new StretchIconHQ(mangaEngine.getImage(pageURLs[i]));//Loads image
					progressBar.setValue(i);//Updates progressbar
					progressBar.setString("Loading Page: " + (i+1) + " of " + progressBar.getMaximum());
				}	
				catch(Exception ex){
					ex.printStackTrace();
				}
			}
			return null;		
		}

		/*
		 * Executed in event dispatching thread
		 */
		@Override
		public void done() {
			super.done();//Cleans up
			parent.getContentPane().remove(progressBar);//Removes progressbar
			parent.revalidate();//Refreshes JFrame
			parent.repaint();
		}
		
		
		
	}
}

