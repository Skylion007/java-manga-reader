package org.skylion.mangareader.mangaengine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.skylion.mangareader.util.Logger;
import org.skylion.mangareader.util.MangaUtil;
import org.skylion.mangareader.util.StringUtil;

/**
 * An unofficial API for MangaHere.com. This is API is not affiliated in any way with
 * MangaHere. The developers of this program are in now way responsible for any content provided on
 * the website. Moreover, since the API is unofficial it may be subject to breakage.
 * 
 * @author Skylion (Aaron Gokaslan)
 */
public class MangaHereAPI implements MangaEngine{


	private String currentURL;//Saves the current URL for future looksUps

	private final static String MANGA_HERE_URL = "http://www.mangahere.com/manga/";
	private final String[][] mangaList;

	private String[] pageURLs;
	private String[] chapterURLs;
	private String[] chapterNames;
	
	/**
	 * Constructor
	 * @throws IOException if cannot connect to website
	 */
	public MangaHereAPI() throws IOException {
		// TODO Auto-generated constructor stub
		this("http://www.mangahere.com/manga/mirai_nikki/v00/c000/");//Default Manga
	}

	/**
	 * Constructor
	 * @param URL The URL of the page you wish to start at.
	 * @throws IOException if cannot contact website.
	 */
	public MangaHereAPI(String URL) throws IOException {
		mangaList = initalizeMangaList();
		currentURL = URL;
		refreshLists();
	}

	/**
	 * @return the currentURL
	 */
	@Override
	public String getCurrentURL() {
		return currentURL;
	}

	/**
	 * @param currentURL the currentURL to set
	 */
	@Override
	public void setCurrentURL(String currentURL) {
		this.currentURL = currentURL;
	}

	/**
	 * @return The name of the manga parsed from the current URL
	 */
	@Override
	public String getMangaName(){
		return getMangaName(currentURL);
	}

	/**
	 * @param url The URL you want to find the mange name of
	 * @return The name of the manga parsed from the URL
	 */
	public String getMangaName(String url){
		String manga = url.substring(url.lastIndexOf("/manga/")+7);
		while(manga.indexOf('/')>-1){
			manga = manga.substring(0,manga.indexOf('/'));
		}
		return manga;
	}
	
	/**
	 * Returns a StretchIconHQ of the page found on the specified URL.
	 * @param url the url you want to grab the page from
	 * @return the image as a StretchIconHQ
	 * @throws IOException if URL is invalid
	 */
	@Override
	public BufferedImage loadImg(String url) throws IOException{
		if(url==null || url.equals("")){
			System.out.println(url);
			throw new IOException("INVALID PARAMETER");
		}
		//Detects whether it is loading a new Manga 
		boolean hasMangaChanged = !getMangaName(currentURL).equals(getMangaName(url));
		boolean hasChapterChanged = getCurrentChapNum()!= getChapNum(url);
		BufferedImage image = getImage(url);
		currentURL = url;
		if(hasMangaChanged || hasChapterChanged){
			refreshLists(); //Refreshes Chapter & Page URLs
		}
		return image;
	}


	@Override
	public BufferedImage getImage(String URL) throws IOException{
		if(URL == null || URL.equals("")){ return null;}
		Document doc = Jsoup.connect(URL).timeout(5*1000).get();
		Element e = doc.getElementById("image");
		String imgUrl = e.absUrl("src");
		return ImageIO.read(new URL(imgUrl));
	}
	
	/**
	 * Fetches the URL for the next Page as defined by links within the current page.
	 */
	@Override
	public String getNextPage(){
		String[] pages = this.getPageList();
		int index = StringUtil.indexOf(pages, currentURL);
		if(index + 1 == pages.length){
			return getNextChapter();
		}
		else{
			return pages[index + 1];
		}
	}

	/**
	 * Gets the next chapter
	 * @return
	 */
	public String getNextChapter(){
		try {
			return getNextChapter(Jsoup.connect(currentURL).get());
		} catch (IOException e) {
			Logger.log(e);
			return null;
		}
	}
	
	private String getNextChapter(Document doc){
		Elements exs = doc.getElementsByClass("reader_tip").first().children();
		Element ex = exs.get(exs.size()-2);
		String extract = ex.html();//Element is not properly closed. Manually parsing required
		if(!extract.contains("href=\"")){//First Chapter: Special Case
			ex = exs.get(exs.size()-1);
			extract = ex.html();
		}
		//Manually extracts the HREF
		extract = extract.substring(extract.indexOf("href=\""));
		extract = extract.substring(extract.indexOf('"')+1);
		extract = extract.substring(0,extract.indexOf('"'));
		//TODO Update parsing with Regex
		return extract;
	}
	
	/**
	 * Fetches the previous page as specified by links on the current page
	 * If at page 0 chapter 1, goes to Chapter 2.
	 * If at page 0 of chapter #>2 goes to previous chapter page 1.
	 */
	@Override
	public String getPreviousPage(){
		Document doc;
		try {
			doc = Jsoup.connect(currentURL).get();
			Element e = doc.getElementsByClass("prew_page").first();
			String backPage = e.absUrl("href");
			//Special Case: Beginning of Chapter
			if(backPage == null || backPage.equals("") || backPage.equals("javascript:void(0)")){
				//System.out.println("Previous Chapter");
				backPage = getPreviousChapter(doc);
			}
			return backPage;
		} catch(IOException e){
			Logger.log(e);
		}
		return null;
	}

	/**
	 * Fetches the previous chapter
	 * @return The URL for the previous chapter
	 */
	public String getPreviousChapter(){
		try {
			return getPreviousChapter(Jsoup.connect(currentURL).get());
		} catch (IOException e) {
			Logger.log(e);
			return null;
		}
	}
	
	/**
	 * Overloaded method to conserve resources
	 * @param doc
	 * @return
	 */
	private String getPreviousChapter(Document doc){
		Elements exs = doc.getElementsByClass("reader_tip").first().children();
		Element ex = exs.get(exs.size()-1);
		String extract = ex.html();//Element not closed properly. Manual parsing required.
		extract = extract.substring(extract.indexOf("href=\""));
		extract = extract.substring(extract.indexOf('"')+1);
		extract = extract.substring(0,extract.indexOf('"'));
		return extract;

	}
	
	/**
	 * Generates a list of all available manga on the site
	 * @return the list as a List<String>
	 */
	@Override
	public List<String> getMangaList(){	
		List<String> names = new ArrayList<String>(mangaList[0].length);
		names.addAll(Arrays.asList(mangaList[0]));
		return names;
	}

	/**
	 * Generates the URL from the name of the manga by comparing it with local databases.
	 * @param mangaName The name of the Manga you want to search for.
	 */
	@Override
	public String getMangaURL(String mangaName){
		String mangaURL = "";
		mangaName = StringUtil.removeTrailingWhiteSpaces(mangaName);
		try {
			boolean found = false;
			for(int i = 0;!found && i<mangaList[0].length; i++){
				String name = mangaList[0][i];
				if(mangaName.equalsIgnoreCase(name)){
					mangaURL = mangaList[1][i]; 
					found = true;
				}
			}
			if(!found){
				mangaURL = searchForManga(mangaName);
			}
			mangaURL = getFirstChapter(mangaURL);
		} catch (IOException e) {
			Logger.log(e);
			mangaName = StringUtil.removeTrailingWhiteSpaces(mangaName);
			mangaURL = mangaNameToURL(mangaName);
			mangaURL = MANGA_HERE_URL + mangaURL;
		}
		return mangaURL;
	}

	/**
	 * Searches for the manga name and returns the URL.
	 * @param searchTerm The name of the Manga
	 * @return The URL 
	 * @throws IOException if something goes wrong
	 */
	private String searchForManga(String searchTerm) throws IOException{
		String url = "http://www.mangahere.com/search.php";
		String encoded = URLEncoder.encode(searchTerm, "UTF-8");
		url += "?name=" + encoded;
		Document doc = Jsoup.connect(url).timeout(5000).get();
		Elements results = doc.getElementsByClass("result_search").first().children();
		results.remove(results.last());//Removes useless link from footer
		for(Element e: results){
			String text = e.children().last().text();
			text = text.substring(text.indexOf(':')+1);
			String[] names = text.split(";");
			for(String s: names){
				if(s.substring(1).equalsIgnoreCase(searchTerm)){
					return e.select("a").first().absUrl("href");
				}
			}
		}
		return "";
	}

	/**
	 * Gets first the chapter from the manga Base URL
	 * @param mangaURL
	 * @return
	 * @throws IOException
	 */
	private String getFirstChapter(String mangaURL) throws IOException{
		Document doc = Jsoup.connect(mangaURL).get();
		Element e = doc.getElementsByClass("detail_list").last();
		Element item = e.select("a").last();
		return item.absUrl("href");
	}

	/**
	 * Returns the current page number
	 */
	@Override
	public int getCurrentPageNum(){
		if(currentURL.charAt(currentURL.length()-1)=='/'){
			return 1;
		} else {
			String page = currentURL.substring(currentURL.lastIndexOf('/')+1,currentURL.lastIndexOf('.'));
			return Integer.parseInt(page);
		}
	}
	
	/**
	 * Returns the current chapter number
	 */
	@Override
	public int getCurrentChapNum(){
		return (int)getChapNum(currentURL);
	}
	
	/**
	 * Calculates the chapter
	 * @param url
	 * @return
	 */
	private double getChapNum(String url){
		if(!StringUtil.containsNum(url) || url.lastIndexOf('c')==-1){
			return -1;
		}
		url = url.substring(url.lastIndexOf('c'));
		url = url.substring(1, url.indexOf('/'));
		return Double.parseDouble(url);//Rounds to an int. Needed for v2 uploads and such.
	}
	
	/**
	 * @return an array of PageURLs
	 */
	@Override
	public String[] getPageList(){
		return pageURLs;
	}

	/**
	 * @return an array of Chapter Urls
	 */
	@Override
	public String[] getChapterList(){
		return chapterURLs;
	}

	/**
	 * Generates a list of Chapter names
	 * @return the chapter name.
	 */
	@Override
	public String[] getChapterNames(){
		return chapterNames;
	}

	/**
	 * Determines whether a webpage is valid on the site.
	 * @param url The URL you want
	 * @return true if it is valid. False, otherwise.
	 */
	@Override
	public boolean isValidPage(String url){
		Document doc;
		try {
			doc = Jsoup.connect(url).get();
			return !(doc.hasClass("error_404") || doc.hasClass("mangaread_error") 
					|| doc.text().contains(" is not available yet. We will update")
					|| this.isMangaLicensed(url));

		} catch (IOException e) {
			Logger.log(e);
			return false;
		}
	}

	/**
	 * Determines whether a Manga is licensed and is not shown on MangaHere
	 * @param mangaURL The URL you are checking, should be base URL.
	 * @return True if liscensed, false otherwise
	 * @throws IOException if cannot complete request
	 */
	public boolean isMangaLicensed(String mangaURL) throws IOException{
		Document doc;
		doc = Jsoup.connect(mangaURL).get();
		Element e =  doc.getElementsByClass("detail_list").first();
		if(e==null){
			return false;
		}
		return e.text().contains("has been licensed, it is not available in MangaHere.");
	}

	/**
	 * Gets a list of all Manga and the corresponding URL
	 * @return A 2D array containing the name and URL of each Manga on the site
	 */
	private String[][] initalizeMangaList() throws IOException{
		String[][] out;
		Document doc = Jsoup.connect("http://www.mangahere.com/mangalist/").timeout(10*1000).maxBodySize(0).get();
		Elements items = doc.getElementsByClass("manga_info");
		items = MangaUtil.removeLicensedManga(items);
		out = new String[2][items.size()];
		for(int i = 0; i<items.size(); i++){
			Element item = items.get(i);
			out[0][i] = item.text();
			out[1][i] = item.absUrl("href");
		}
		return out;
	}

	/**
	 * Generates a list of all the chapters from the base URL
	 * @return An array of current chapter URLs
	 */
	private String[] intializeChapterList(){
		try{
			String linkPage = StringUtil.urlToText(currentURL);
			String url = "http://www.mangahere.com/get_chapters" + parseSeriesID(linkPage) 
					+ ".js?v=306";
			String page = StringUtil.urlToText(url);
			page = page.substring(page.indexOf("Array(")+6);
			page = page.substring(0,page.indexOf(");"));
			page = page.substring(1,page.length()-1);
			String[] array = page.split("\\],\n  \\[");
			String name = parseSeriesName(linkPage);
			for(int i = 0; i<array.length; i++){
				String s = array[i];
				s = StringUtil.stripQuotes(s);
				s = s.substring(s.indexOf("http"));
				s = s.replace("\"+series_name+\"", name);
				array[i] = s;
			}
			return array;
		} catch(IOException e) {
			Logger.log(e);
		}
		return null;
	}	
	
	/**
	 * Retrieves the names of the chapter
	 * @return The current chapter name.
	 */
	private String[] initializeChapterNames(){
		try{
			String linkPage = StringUtil.urlToText(currentURL);
			String url = "http://www.mangahere.com/get_chapters"+ parseSeriesID(linkPage) 
					+ ".js?v=306";
			String page = StringUtil.urlToText(url);
			page = page.substring(page.indexOf("Array(")+6);
			page = page.substring(0,page.indexOf(");"));
			page = page.substring(1,page.length()-1);
			String[] array = page.split("\\],\n  \\[");
			for(int i = 0; i<array.length; i++){
				String s = array[i];
				s = StringUtil.stripQuotes(s);
				if(s.indexOf(':')!=-1){
					s = s.substring(0, s.indexOf(':'));
				}
				if(s.indexOf('"')!=-1){
					s = s.substring(0, s.indexOf('"'));
				}
				s = s.substring(s.lastIndexOf(' ') + 1);				
				s = StringUtil.formatChapterNames(s);
				array[i] = s;
			}
			return array;
		}
		catch(IOException e){
			Logger.log(e);
		}
		return null;
	}
	
	/**
	 * Fetches a list of URLs.
	 * @return A String[] consisting of the URLs for the pages.
	 */
	private String[] initalizePageList(){
		List<String> pages = new ArrayList<String>();
		try {
			Document doc = Jsoup.connect(currentURL).get();
			Element list = doc.getElementsByClass("wid60").first();
			for(Element item: list.children()){
				pages.add(item.attr("value"));
			}
		} catch(IOException e){
			Logger.log(e);
		}
		String[] out = new String[pages.size()];
		pages.toArray(out);
		return out;
	}

	private String parseSeriesName(String page){
		page = page.substring(page.indexOf("series_name = \"")+15);
		page = page.substring(0, page.indexOf('"'));
		return page;
	}

	private String parseSeriesID(String page){
		page = page.substring(page.indexOf("series_id")+10);
		page = page.substring(0, page.indexOf('&'));
		return page;
	}

	/**
	 * Converts a Manga Title into the website's naming conventions
	 * @param The name of the Manga you want to convert
	 * @return The returned name of the Manga
	 */
	private String mangaNameToURL(String name){
		name = name.toLowerCase();
		while(name.charAt(name.length()-1)==' '){//Removes trailing whitespaces
			name = name.substring(0,name.length()-1);
		}
		name = name.replace('@', 'a');//Special Case
		name = name.replace("& ", "");//Special Case
		name = name.replace(' ', '_');
		name = name.replace(",", "");
		for(int i = 0; i<name.length(); i++){
			char x = name.charAt(i);
			if(!Character.isLetter(x) && x != '_' && !Character.isDigit(x)){
				if(i-1>=0 && i+1>=name.length() && name.charAt(i-1) == '_' && name.charAt(i+1) == '_') {
					name = name.replace(""+x, "");
				} else {
					name = name.replace(x,'_');
				}
			}
			
		}
		return name;
	}
	
	/**
	 * Reload pageURLs and chapterURLs when the manga changes
	 */
	private void refreshLists(){
		chapterURLs = this.intializeChapterList();
		pageURLs = this.initalizePageList();
		chapterNames = this.initializeChapterNames();
	}
}
