package org.skylion.mangareader.mangaengine;

import java.awt.image.BufferedImage;
import java.util.List;


/**
 * An interface used for Manga Scrapers
 * @author Skylion
 *
 */
public interface MangaEngine {

	public String getCurrentURL();
	public void setCurrentURL(String url);
	public BufferedImage loadImg(String url)throws Exception;
	public BufferedImage getImage(String url) throws Exception;
	
	
	public String getNextPage();
	public String getPreviousPage();

	public boolean isValidPage(String url);
	
	public List<String> getMangaList();
	
	public String getMangaName();
	
	public String[] getChapterList();
	public String[] getPageList();
	public String[] getChapterNames();
	
	public String getMangaURL(String mangaName);
	
	public int getCurrentPageNum();
	public int getCurrentChapNum();

}
