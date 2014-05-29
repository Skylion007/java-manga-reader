package org.skylion.mangareader.mangaengine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import org.skylion.mangareader.util.Logger;
import org.skylion.mangareader.util.StringUtil;



/**
 * Unofficial API hook for MangaEden.com
 * This website actually has an API so should be reliable.
 * @author Skylion
 *
 */
public class MangaEdenAPI implements MangaEngine{

	private final static String[][] mangaList = initializeMangaList();//This takes a while to populate;
	private String[] chapterURLs;
	private String[] pageURLs;

	private String currentManga;
	private int currentChapter;
	private int currentPage;

	private final static String MANGA_EDEN_URL = "https://www.mangaeden.com/api/";

	public MangaEdenAPI() throws IOException {
		currentManga = this.getMangaURL("Mirai Nikki");
		chapterURLs = this.initializeChapterList();
		currentChapter = 0;
		pageURLs = this.initializePageList();
		currentPage = 0;
	}

	@Override
	public String getCurrentURL() {
		return pageURLs[currentPage];
	}

	@Override
	public void setCurrentURL(String url) {
		if(url.contains("www.mangaeden.com/api/manga/")){
			this.currentManga = url;
			this.currentChapter = 0;
			refreshLists();
		} else if(url.contains("www.mangaeden.com/api/chapter")){
			int chapterIndex = StringUtil.indexOf(chapterURLs, url);
			if(chapterIndex!=-1){
				this.currentChapter = chapterIndex;
				this.currentPage = 0;
				refreshLists();
			}
		} else if(url.contains("cdn.mangaeden.com/mangasimg/")){
			int pageIndex = StringUtil.indexOf(pageURLs, url);
			if(pageIndex!=-1) {
				this.currentPage = pageIndex;
			} else {
				String[] pages;
				try {
					pages = this.getPageURLs(chapterURLs[this.currentChapter+1]);
					if(StringUtil.indexOf(pages, url)!=-1){
						this.currentChapter++;
						this.pageURLs = pages;
						this.currentPage = 0;
					}
					refreshLists();
				} catch (IOException e) {
					Logger.log(e);
				}	
			}
		}
	}

	@Override
	public BufferedImage loadImg(String url) throws IOException {
		if(url.contains("manga")){
			setCurrentURL(url);
			refreshLists();
			url = this.pageURLs[0];
		}
		BufferedImage image = (getImage(url));
		setCurrentURL(url);
		return image;
	}

	@Override
	public BufferedImage getImage(String url) throws IOException {
		if(url.contains("chapter")){
			url = this.getPageURLs(url)[0];
		}
		return ImageIO.read(new URL(url));
	}

	@Override
	public String getNextPage() {
		if(currentPage+1<this.pageURLs.length-1){
			return this.pageURLs[currentPage+1];
		} else if(currentChapter+1 < chapterURLs.length){
			try {
				return this.getPageURLs(this.chapterURLs[currentChapter+1])[0];
			} catch (IOException e) {
				Logger.log(e);
				return this.currentManga;
			}
		}
		else {
			return this.pageURLs[this.currentPage];
		}
	}

	@Override
	public String getPreviousPage() {
		if(currentPage>0){
			return this.pageURLs[currentPage-1];//Goes back page
		} else if(currentChapter>0){
			return this.chapterURLs[currentChapter-1];//Else goes back a chapter
		} else {
			return this.pageURLs[this.currentPage];//Else does nothing
		}
	}

	@Override
	public boolean isValidPage(String url) {
		return url.contains("api") && url.contains("mangaeden.com");
	}

	@Override
	public List<String> getMangaList() {
		List<String> out = new ArrayList<String>(mangaList.length);
		for(int i = 0; i<mangaList.length; i++){
			out.add(mangaList[i][0]);
		}
		return out;
	}

	@Override
	public String getMangaName() {
		try {
			return getNameFromChapter(currentManga);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String[] getChapterList() {
		return this.chapterURLs;
	}

	@Override
	public String[] getPageList() {
		return this.pageURLs;
	}

	@Override
	public String[] getChapterNames() {
		try {
			String raw = StringUtil.urlToText(currentManga);
			raw = raw.substring(raw.indexOf("\"chapters\": [")+13);//Removes unnecessary information
			raw = raw.substring(0, raw.indexOf("],\n  \""));//End of Chapter Section
			String[] chapters = raw.split("\\],");//Parses into chapters
			String[] out = new String[chapters.length];
			for(int i = 0; i<chapters.length; i++){
				String tmp = chapters[i].substring(0, chapters[i].indexOf(','));
				out[chapters.length-1-i] = tmp.substring(tmp.lastIndexOf(' '));
			}
			StringUtil.formatChapterNames(out);
			return out;
		} catch (IOException e) {
			return null;
		}

	}

	@Override
	public String getMangaURL(String mangaName) {
		mangaName = StringUtil.removeTrailingWhiteSpaces(mangaName);
		for(int i = 0; i<mangaList.length; i++){
			if(mangaList[i][0].equalsIgnoreCase(mangaName)){
				System.out.println("Manga#: " + i);
				return mangaList[i][1];
			}
		}
		return null;
	}

	@Override
	public int getCurrentPageNum() {
		return this.currentPage+1;
	}

	@Override
	public int getCurrentChapNum() {
		return this.currentChapter;
	}

	/**
	 * Generates the MangaList
	 * Called once during first instationation of the class. The list is very resource 
	 * intensive to construct. Thus, it is only called once.
	 * @return The initialized manga list.
	 */
	private static String[][] initializeMangaList(){
		String raw = multiPageFetch(25);
		String[] manga = raw.split("\\},");
		String[][] mangaProps = new String[manga.length][7];
		for(int i = 0; i<manga.length; i++){
			mangaProps[i]=manga[i].split(",\n");
		}
		String[][] out = new String[mangaProps.length][2];
		for(int i = 0; i<mangaProps.length; i++){
			out[i][0] = extractValue(mangaProps[i][mangaProps[i].length-1]);//Always last value
			if(mangaProps[i].length<7){//Performs manual search if a value is missing or out of order.
				for(int x = 0; x<mangaProps[i].length; x++){
					if(mangaProps[i][x].contains("\"i\"")){
						out[i][1] = extractValue(mangaProps[i][x]);
					}
				}
			}
			else{
				out[i][1] = extractValue(mangaProps[i][2]);
			}
			out[i][1] = generateMangaURL(out[i][1]);//Generates the URL from the ID
		}
		return out;
	}

	private static String multiPageFetch(int pages){
		//Uses an executor service pool for concurrency
		ExecutorService pool = Executors.newFixedThreadPool(pages);
		//Stores the Future (Data that will be returned in the future)
		Set<Future<String>> set = new LinkedHashSet<Future<String>>(pages);
		for(int i = 0; i<= pages; i++){ //Iterates through the list
			Callable<String> callable = new pageFetcher(i);//Creates Callable
			Future<String> future = pool.submit(callable);//Begins to run Callable
			set.add(future);//Adds the response that will be returned to a set.
		}
		List<String> strings = new ArrayList<String>(set.size());
		for(Future<String> future: set){
			try {
				strings.add(future.get());//Gets the returned data from the future.
			} catch (ExecutionException e) {//Thrown if the MP3DataFetcher encountered an error.
				Throwable ex = e.getCause();
				ex.printStackTrace();
			} catch (InterruptedException e){//Will probably never be called, but just in case...
				Thread.currentThread().interrupt();//Interrupts the thread since something went wrong.
			}
		}
		return StringUtil.concatCollection(strings);//Sequences the stream.
	}
	
	private static class pageFetcher implements Callable<String>{
		private static final String BASE_URL = "https://www.mangaeden.com/api/list/0/";
		
		private int page;
		
		public pageFetcher(int page){
			this.page = page;
		}
		
		@Override
		public String call() throws IOException{
			String url = BASE_URL + "?p=" + page;
			String raw = StringUtil.urlToText(url);
			raw = raw.substring(raw.indexOf("  \"manga\": [")+"  \"manga\": [".length()+1, 
					raw.lastIndexOf("],"));//Cleans up
			raw = raw.substring(1);
			return raw;
		}
		
	}
	
	private String[] initializeChapterList() throws IOException{
		return getChapterURLs(this.currentManga);
	}

	private String[] getChapterURLs(String mangaURL) throws IOException{
		String raw = StringUtil.urlToText(mangaURL);
		raw = raw.substring(raw.indexOf("\"chapters\": [")+13);//Removes unnecessary information
		raw = raw.substring(0, raw.indexOf("],\n  \""));//End of Chapter Section
		String[] chapters = raw.split("\\],");//Parses
		String[] out = new String[chapters.length];
		for(int i = 0; i<chapters.length; i++){
			chapters[i] = chapters[i].substring(0, chapters[i].lastIndexOf('"'));
			chapters[i] = chapters[i].substring(chapters[i].lastIndexOf('"')+1);
			chapters[i] = generateChapterURL(chapters[i]);
			out[chapters.length-1-i] = chapters[i];//Reverses order
		}
		return out;
	}

	private String[] initializePageList() throws IOException{
		return getPageURLs(this.chapterURLs[this.currentChapter]);
	}

	private String[] getPageURLs(String chapterURL) throws IOException{
		String raw = StringUtil.urlToText(chapterURL);
		return parsePageList(raw);
	}

	private String[] parsePageList(String raw){
		raw = raw.substring(raw.indexOf("\"images\": [") + 15);
		String[] pages = raw.split("\\]");
		String[] out = new String[pages.length];
		for(int i = 0; i<pages.length-2; i++){
			pages[i] = pages[i].split(",")[2];
			pages[i] = pages[i].substring(pages[i].lastIndexOf(' ')+1);
			if(pages[i].indexOf('"')!=-1){
				pages[i] = this.generateImageURL(StringUtil.stripQuotes(pages[i]));
			}
			else{
				pages[i] = null;
			}
			out[pages.length-1-i] = pages[i];//Reverse Array to proper order
		}
		pages = out;
		out = null;//Trash collection
		pages = StringUtil.cleanArray(pages);
		return pages;
	}

	/**
	 * Generates the Image from the currentURL
	 * @param mangaID The manga ID you want to generate the URL
	 * @return The generated URL
	 */
	private static String generateMangaURL(String mangaID){
		return MANGA_EDEN_URL + "manga/" + mangaID + "/"; 
	}

	private String generateChapterURL(String chapterID){
		return MANGA_EDEN_URL + "chapter/" + chapterID + "/";
	}

	private String generateImageURL(String imageID){
		String out = "http://cdn.mangaeden.com/mangasimg/" + imageID;
		System.out.println(out.replace("\n", "\\n"));
		return out;
	}

	private static String extractValue(String input){
		input = input.replace("{","");
		input = input.replace("}", "");
		input = input.replace("\n", "");
		if(!input.contains(":")){
			System.out.println("ERROR" + input);
		}
		input = input.substring(input.indexOf(':')+2);
		input = StringUtil.stripQuotes(input);
		return input;
	}

	public String getNameFromChapter(String mangaURL) throws IOException{
		String raw = StringUtil.urlToText(mangaURL);
		raw = raw.substring(raw.indexOf("\"title\": ")+ 10);
		raw = raw.substring(0,raw.indexOf('"'));
		return raw;
	}

	private void refreshLists(){
		int attempts = 0;
		while(attempts<3){
			try {
				currentPage = 0;
				this.chapterURLs = this.initializeChapterList();
				this.pageURLs = this.initializePageList();
				break;
			} catch (IOException e) {
				if(attempts == 2){
					e.printStackTrace();
				}
				attempts++;
			}
		}
		System.out.println(Arrays.toString(this.pageURLs));
	}


}