package org.skylion.mangareader.mangaengine;

import java.util.List;

import org.skylion.mangareader.util.StretchIconHQ;

public class Prefetcher implements MangaEngine{

	private MangaEngine mangaEngine;
	private StretchIconHQ[] pages;
	private String[] pageURLs; 
	private String mangaName;
	
	public Prefetcher(MangaEngine mangaEngine){
		this.mangaEngine = mangaEngine;
		prefetch();
	}
	
	public void prefetch (){
		try{
			mangaName = mangaEngine.getMangaName();
			pageURLs = mangaEngine.getPageList();
			pages = new StretchIconHQ[pageURLs.length];
			//Resets page counter to first page by fetching pages in reverse order
			for(int i = pageURLs.length-1; i>=0; i--){
				pages[i] = mangaEngine.loadImg(pageURLs[i]);
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
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
		for(String s: pageURLs){
			if(s.equals(URL)){
				return true;
			}
		}
		return false;
	}
	
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
		System.out.println(pageURLs[mangaEngine.getCurrentPageNum()]);
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

	
}

