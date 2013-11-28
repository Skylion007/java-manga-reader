package org.skylion.mangareader.mangaengine;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.skylion.mangareader.util.StretchIconHQ;

public class Prefetcher implements MangaEngine{

	private MangaEngine mangaEngine;
	private StretchIconHQ[] pages;
	private String[] pageURLs; 
	private String mangaName;

	private JProgressBar progressBar;//The Progress Monitor
	private JFrame parent; //The Parent Component to display the loading bar in
	private Task task;//The Swing Worker that handles repaint
	
	public Prefetcher(JFrame component, MangaEngine mangaEngine){
		this.mangaEngine = mangaEngine;
		parent = component;
		mangaName = mangaEngine.getMangaName();
		pageURLs = mangaEngine.getPageList();
		prefetch();
	}

	public void prefetch (){
		mangaName = mangaEngine.getMangaName();
		pageURLs = mangaEngine.getPageList();
		progressBar = new JProgressBar(0, mangaEngine.getPageList().length);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		pages = new StretchIconHQ[pageURLs.length];
		task = new Task();
		task.addPropertyChangeListener(new PropertyChangeListener(){
		    /**
		     * Invoked when task's progress property changes.
		     */
		    @Override
			public void propertyChange(PropertyChangeEvent evt) {
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
	 * Returns whether or not the image could be and is in the database.
	 * Makes assumptions to speed up process.
	 * @param URL The URL you want to check
	 * @return True if it is in the database, false otherwise.
	 */
	private boolean isFetched(String URL){
		if(mangaEngine.getCurrentPageNum()>=pageURLs.length){
			System.out.println(pageURLs.length +"#"+mangaEngine.getCurrentPageNum());
			return false;
		}
		System.out.println(pageURLs[mangaEngine.getCurrentPageNum()] + "#" + URL);
		return (pageURLs[mangaEngine.getCurrentPageNum()].equals(URL) || 
				isCached(URL));
	}

	/**
	 * Checks solely whether or not the URL is in the database.
	 * @param URL The URL you want to check
	 * @return True if in database false otherwise.
	 */
	private boolean isCached(String URL){
		System.out.println("Cached");
		for(int i = 0; i<pageURLs.length; i++){
			if(pageURLs[i].equals(URL) && pages[i]!=null){
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param URL
	 * @return
	 */
	private StretchIconHQ fetch(String URL){
		int page = mangaEngine.getCurrentPageNum();
		if(page>=pageURLs.length){
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
		if(isFetched(url)){
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
	public String getNextPage() {
		if(progressBar!=null && mangaEngine.getCurrentPageNum()+1>=progressBar.getValue()){
			Toolkit.getDefaultToolkit().beep();
		}
		return mangaEngine.getNextPage();
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
		// TODO Auto-generated method stub
		return mangaEngine.getCurrentChapNum();
	}
	
	/**
	 * ProgressMonitor Swing Worker. Needed to ensure successful reload of ProgressMonitor
	 * @author Skylion
	 */
	class Task extends SwingWorker<Void, Void> {
		
		public Task(){
			parent.getContentPane().add(progressBar, BorderLayout.SOUTH);
			parent.revalidate();
			parent.repaint();
		}
		/*
		 * Main task. Executed in background thread.
		 */
		@Override
		public Void doInBackground() {
			try{
				//Resets page counter to first page by fetching pages in reverse order
				for(int i = pageURLs.length-1; i>=0; i--){
					pages[i] = mangaEngine.loadImg(pageURLs[i]);
					progressBar.setValue(pageURLs.length-1-i);
					progressBar.setString("Loading Page:" + (pageURLs.length-1-i) + " of " + (pageURLs.length-1));
				}

			}
			catch(Exception ex){
				ex.printStackTrace();
				done();//Cleans Up 
				return null;
			}
			return null;		
		}

		/*
		 * Executed in event dispatching thread
		 */
		@Override
		public void done() {
			parent.getContentPane().remove(progressBar);
			parent.revalidate();
			parent.repaint();
			progressBar = null;
		}
	}
}

