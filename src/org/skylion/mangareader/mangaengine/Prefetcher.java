package org.skylion.mangareader.mangaengine;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import org.skylion.mangareader.util.Logger;

/**
 * A class that wraps a MangaEngine and prefetches from it to improve speed.
 * The class also adds a progress bar to the JFrame to show how the prefetching is going.
 * @author Skylion
 *
 */
public class Prefetcher implements MangaEngine, Closeable{

	private MangaEngine mangaEngine;//Current MangaEngine
	private BufferedImage[] pages;//Current Images
	private String[] pageURLs; //URLs corresponding to current images
	private String mangaName;//CurrentMangaName

	private JProgressBar progressBar;//The Progress Monitor
	private Container parent; //The Parent Component to display the loading bar in
	private Task task;//The Swing Worker that handles repaint
	
	/**
	 * Constructor
	 * @param window The JFrame you want to add the progressbar to
	 * @param mangaEngine The Engine you want to prefetch from
	 */
	public Prefetcher(Container window, MangaEngine mangaEngine){
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
	public void prefetch(){
		mangaName = mangaEngine.getMangaName();
		pageURLs = mangaEngine.getPageList();
		pages = new BufferedImage[pageURLs.length];
		progressBar.setValue(0);
		progressBar.setMaximum(pageURLs.length);
		progressBar.setStringPainted(true);
		if(task!=null && !task.isDone() && !task.isCancelled()){//Cancels previous task before starting a new one.
			task.cancel(true);
		}
		task = new Task();
		task.addPropertyChangeListener(new PropertyChangeListener(){
		    /**
		     * Invoked when a task's progress property changes.
		     */
		    @Override
			public void propertyChange(PropertyChangeEvent evt) {//Updates Progressbar
		        if ("progress".equals(evt.getPropertyName()) ) {
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
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if url is in database.
	 * @param URL The URL You want to check
	 * @return True if the URL is found, false otherwise.
	 */
	private boolean isFetched(String URL){
		for(int i = 0; i<pageURLs.length; i++){
			if(pageURLs[i].equals(URL)){
				return true;
			}
		}
		System.out.println("Prefetching because of " + URL);
		return false;
	}

	/**
	 * Fetches the data
	 * @param URL
	 * @return
	 */
	private BufferedImage fetch(String URL){
		if(!isCached(URL) || mangaEngine.getCurrentPageNum()>pages.length){
			try {
				BufferedImage icon = mangaEngine.loadImg(URL);
				if(!isFetched(URL)){
					prefetch();
				}
				if(icon==null){//Sometimes the chapter ends or starts on a blank page.
					icon = mangaEngine.loadImg(mangaEngine.getNextPage());
				}
				return icon;
			} catch (IOException e) {			
				Logger.log(e);
				return null;
			}
		}
		else {
			return pages[mangaEngine.getCurrentPageNum()-1];
		}
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
	public BufferedImage loadImg(String url) throws IOException {
		if(url == null) {
			return null;
		} if(isCached(url)) {
			mangaEngine.setCurrentURL(url);
			return fetch(url);
		} else {
			BufferedImage out = mangaEngine.loadImg(url);
			if(!isFetched(url)) {
				prefetch();
			}
			return out;
		}
	}
	
	@Override
	public BufferedImage getImage(String url) throws IOException {
		return mangaEngine.getImage(url);
	}

	@Override
	public String getNextPage() {
		String currentURL = mangaEngine.getCurrentURL();
		String nextPage = mangaEngine.getNextPage();
		if(isCached(nextPage) || task == null || task.isCancelled() || task.isDone()){
			return nextPage;
		}
		else{
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
	
	/**
	 * Returns the MangaEngine that the class wraps
	 * @return The wrapped MangaEngine
	 */
	public MangaEngine getMangaEngine(){
		return mangaEngine;
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
	public String[] getChapterNames() {
		return mangaEngine.getChapterNames();
	}

	@Override
	public void close(){
		if(task!=null && !task.isDone() && !task.isCancelled()){//Cancels previous task before starting a new one.
			task.cancel(true);
		}
	}
	
	/**
	 * Where the actual prefetching happens
	 * @author Skylion
	 */
	class Task extends SwingWorker<Void, Void> {
		
		public Task(){
			parent.add(progressBar, BorderLayout.SOUTH);//Adds ProgressBar to bottom
			parent.revalidate();//Refreshes JFRame
			parent.repaint();
		}
		/**
		 * Main task. Executed in background thread.
		 */
		@Override
		public Void doInBackground() {
			for(int i = 0; i<pageURLs.length && !this.isCancelled(); i++){
				int attemptNum = 0;
				while(attemptNum <=3){//Retries three times to load the image.
					try {		
						pages[i] = mangaEngine.getImage(pageURLs[i]);//Loads image
						progressBar.setValue(i);//Updates progressbar
						progressBar.setString("Loading Page: " + (i+1) + " of " + progressBar.getMaximum());
						break;
					} catch(IOException e){
						Logger.log(e);
						attemptNum++;
					}
				}
			}
			return null;		
		}

		/**
		 * Executed in event dispatching thread
		 */
		@Override
		public void done() {
			super.done(); //Cleans up
			parent.remove(progressBar); //Removes progressbar
			parent.revalidate(); //Refreshes JFrame
			parent.repaint();
		}

	}
}

