package org.skylion.mangareader.mangaengine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.skylion.mangareader.util.StretchIconHQ;;

/**
 * An unofficial API for MangaHere.com. This is API is not affiliated in any way with
 * MangaHere. The developers of this program are in now way responsible for any content provided on
 * the website. Moreover, since the API is unofficial it may be subject to breakage.
 * @author Skylion (Aaron Gokaslan)
 */
public class MangaHereAPI implements MangaEngine{


	private String currentURL;//Saves the current URL for future looksUps

	private final static String MANGA_HERE_URL = "http://www.mangahere.com/manga/";
	private String[][] mangaList;

	private String[] pageURLs;
	private String[] chapterURLs;

	/**
	 * Constructor
	 * @throws Exception if cannot connect to website
	 */
	public MangaHereAPI() throws Exception {
		// TODO Auto-generated constructor stub
		this("http://www.mangahere.com/manga/mirai_nikki/v00/c000/");//Default Manga
	}

	/**
	 * Constructor
	 * @param URL The URL of the page you wish to start at.
	 * @throws Exception if cannot contact website.
	 */
	public MangaHereAPI(String URL) throws Exception {
		mangaList = initalizeMangaList();
		currentURL = URL;
		refreshLists();
	}

	/**
	 * @return the currentURL
	 */
	public String getCurrentURL() {
		return currentURL;
	}

	/**
	 * @param currentURL the currentURL to set
	 */
	public void setCurrentURL(String currentURL) {
		this.currentURL = currentURL;
	}

	/**
	 * @return The name of the manga parsed from the current URL
	 */
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
	 * @throws Exception if URL is invalid
	 */
	public StretchIconHQ loadImg(String url) throws Exception{
		if(url==null || url.equals("")){
			System.out.println(url);
			throw new Exception("INVALID PARAMETER");
		}
		//Detects whether it is loading a new Manga 
		boolean hasMangaChanged = !getMangaName(currentURL).equals(getMangaName(url));
		boolean hasChapterChanged = getCurrentChapNum()!= getChapNum(url);
		Document doc = Jsoup.connect(url).get();
		Element e = doc.getElementById("image");
		String imgUrl = e.absUrl("src");
		BufferedImage image = ImageIO.read(new URL(imgUrl));
		currentURL = url;
		if(hasMangaChanged || hasChapterChanged){
			refreshLists(); //Refreshes Chapter & Page URLs
		}
		return new StretchIconHQ(image,url);
	}


	/**
	 * Fetches the URL for the next Page as defined by links within the current page.
	 */
	public String getNextPage(){
		Document doc;
		try {
			doc = Jsoup.connect(currentURL).get();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		Element e = doc.getElementById("viewer");
		e = e.child(0);//gets the child which contains the URL
		String nextURL = e.absUrl("href");
		//Special Case: End of Chapter
		if(e== null || nextURL == null || nextURL.equals("") || nextURL.equals("javascript:void(0)")){
			//System.out.println("Next Chapter");
			Elements exs = doc.getElementsByClass("reader_tip").first().children();
			Element ex = exs.get(exs.size()-2);
			String extract = ex.html();//Element is not properly closed. Manually parsing required
			if(extract.indexOf("href=\"")==-1){//First Chapter: Special Case
				ex = exs.get(exs.size()-1);
				extract = ex.html();
			}
			//Manually extracts the HREF
			extract = extract.substring(extract.indexOf("href=\""));
			extract = extract.substring(extract.indexOf('"')+1);
			extract = extract.substring(0,extract.indexOf('"'));
			//TODO Update parsing with Regex
			nextURL = extract;
		}
		return nextURL;
	}

	/**
	 * Fetches the previous page as specified by links on the current page
	 * If at page 0 chapter 1, goes to Chapter 2.
	 * If at page 0 of chapter #>2 goes to previous chapter page 1.
	 */
	public String getPreviousPage(){
		Document doc;
		try{
			doc = Jsoup.connect(currentURL).get();
			Element e = doc.getElementsByClass("prew_page").first();
			String backPage = e.absUrl("href");
			//Special Case: Beginning of Chapter
			if(e== null || backPage == null || backPage.equals("") || backPage.equals("javascript:void(0)")){
				//System.out.println("Previous Chapter");
				Elements exs = doc.getElementsByClass("reader_tip").first().children();
				Element ex = exs.get(exs.size()-1);
				String extract = ex.html();//Element not closed properly. Manual parsing required.
				extract = extract.substring(extract.indexOf("href=\""));
				extract = extract.substring(extract.indexOf('"')+1);
				extract = extract.substring(0,extract.indexOf('"'));
				backPage = extract;
			}
			return backPage;
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Generates a list of all available manga on the site
	 * @return the list as a List<String>
	 */
	public List<String> getMangaList(){	
		Document doc;
		try {
			doc = Jsoup.connect("http://www.mangahere.com/mangalist/").get();
			Elements items = doc.getElementsByClass("manga_info");
			List<String> names = new ArrayList<String>();
			for(Element item: items){
				names.add(item.text());
			}
			return names;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Gets Text from URL
	 * @param input
	 */
	public String getMangaURL(String input){
		String mangaURL;
		input = mangaNameToURL(input);
		mangaURL = MANGA_HERE_URL + input;
		//System.out.println("check");
		try {
			//System.out.println(mangaURL);
			for(String[] manga: mangaList){
				String name = manga[0];
				if(input.equalsIgnoreCase(name)){
					mangaURL = manga[1];  
					break;
				}
			}
			mangaURL = getFirstChapter(mangaURL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mangaURL;
	}



	/**
	 * Gets first the chapter from the manga Base URL
	 * @param mangaURL
	 * @return
	 * @throws IOException
	 */
	private String getFirstChapter(String mangaURL) throws IOException{
		Document doc = Jsoup.connect(mangaURL).get();
		Elements e = doc.getElementsByClass("color_0077");
		Element x = e.get(e.size()/2);//In case it can't find anything, it makes a guess.
		for(int i = e.size()-1; i>0; i--){
			Element y = e.get(i);
			if(y.text().toLowerCase().contains(getMangaName().replace("_", " ")) && containsNum(y.text())){
				x = y;
				break;
			}
		}
		return x.absUrl("href");
	}

	/**
	 * Returns the current page number
	 */
	public int getCurrentPageNum(){
		if(currentURL.charAt(currentURL.length()-1)=='/'){
			return 1;
		}
		else{
			String page = currentURL.substring(currentURL.lastIndexOf('/')+1,currentURL.lastIndexOf('.'));
			return Integer.parseInt(page);
		}
	}
	
	/**
	 * Returns the current chapter number
	 */
	public int getCurrentChapNum(){
		String url = currentURL.replace(getBaseURL(), "");
		url = url.substring(1,url.indexOf('/'));
		return Integer.parseInt(url);
	}
	
	private int getChapNum(String url){
		if(!containsNum(url) || url.lastIndexOf('c')==-1){
			return -1;
		}
		url = url.substring(url.lastIndexOf('c'));
		url = url.substring(1, url.indexOf('/'));
		return Integer.parseInt(url);
	}
	
	/**
	 * @return an array of PageURLs
	 */
	public String[] getPageList(){
		return pageURLs;
	}

	/**
	 * @return an array of Chapter Urls
	 */
	public String[] getChapterList(){
		return chapterURLs;
	}

	/**
	 * Generates a list of Chapter names
	 * @return the chapter name.
	 */
	public String[] getChapterNames(){
		String mangaURL = MANGA_HERE_URL + getMangaName().replace(' ', '_')+'/';
		Document doc;
		List<String> chaptersList = new ArrayList<String>();
		try {
			doc = Jsoup.connect(mangaURL).get();
			Elements chapters = doc.getElementsByClass("color_0077");
			for(Element item: chapters){
				//Checks if the link contains the manga name and a number. 
				if(item.text().toLowerCase().contains(getMangaName().replace("_", " ")) && containsNum(item.text())){
					chaptersList.add(item.text());
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		String[] out = new String[chaptersList.size()];
		//Reverses the order so that first chapter is first.
		for(int i=chaptersList.size()-1, x = 0; i>0 && x<out.length; i--, x++){
			out[x] = chaptersList.get(i);
		}
		return out;
	}

	/**
	 * Determines whether a webpage is valid on the site.
	 * @param url The URL you want
	 * @return true if it is valid. False, otherwise.
	 */
	public boolean isValidPage(String url){
		Document doc;
		try {
			doc = Jsoup.connect(url).get();
			return !(doc.hasClass("error_404") || doc.hasClass("mangaread_error") 
					|| doc.text().contains(" is not available yet. We will update")
					|| this.isMangaLicensed(url));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Determines whether a Manga is licensed and is not shown on MangaHere
	 * @param mangaURL The URL you are checking, should be base URL.
	 * @return True if liscensed, false otherwise
	 * @throws Exception if cannot complete request
	 */
	public boolean isMangaLicensed(String mangaURL) throws IOException{
		Document doc;
		doc = Jsoup.connect(mangaURL).get();
		Element e =  doc.getElementsByClass("detail_list").first();
		return e.text().contains("has been licensed, it is not available in MangaHere.");
	}

	/**
	 * Gets a list of all Manga and the corresponding URL
	 * @return A 2D array containing the name and URL of each Manga on the site
	 */
	private String[][] initalizeMangaList() throws Exception{
		String[][] out;
		Document doc = Jsoup.connect("http://www.mangahere.com/mangalist/").get();
		Elements items = doc.getElementsByClass("manga_info");
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
		//System.out.println(getMangaName());
		String mangaURL = getBaseURL();
		Document doc;
		List<String> chaptersList = new ArrayList<String>();
		try {
			doc = Jsoup.connect(mangaURL).get();
			Element chapterList = doc.getElementsByClass("detail_list").first();
			Elements chapters = chapterList.getElementsByClass("color_0077");
			for(Element item: chapters){
				if(item.text().toLowerCase().contains(getMangaName().replace("_", " ")) && containsNum(item.text())){
					String url = item.absUrl("href");
					if(url!=null && !url.equals("")){
						chaptersList.add(url);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] out = new String[chaptersList.size()];
		for(int i=chaptersList.size()-1; i>=0 ; i--){
			out[out.length-1-i] = chaptersList.get(i);
		}
		return out;
	}

	/**
	 * Fetches a list of URLs.
	 * @return A String[] consisting of the URLs for the pages.
	 */
	private String[] initalizePageList(){
		List<String> pages = new ArrayList<String>();
		try{
			Document doc = Jsoup.connect(currentURL).get();
			Element list = doc.getElementsByClass("wid60").first();
			for(Element item: list.children()){
				pages.add(item.attr("value"));
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
		String[] out = new String[pages.size()];
		pages.toArray(out);
		return out;
	}

	/**
	 * Checks whether a String contains Numbers
	 * @param text
	 * @return
	 */
	private boolean containsNum(String text){
		for(char c: text.toCharArray()){
			if(Character.isDigit(c))
				return true;
		}
		return false;
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
		for(int i = 0; i<name.length(); i++){
			char x = name.charAt(i);
			if(!Character.isLetter(x) && x != '_' && !Character.isDigit(x)){
				name = name.replace(""+x,"");
			}
		}
		return name;
	}

	/**
	 * Infers the base URL of the manga (mangaURL) from the currentURL
	 * @return The inferred base URL
	 */
	private String getBaseURL(){
		return MANGA_HERE_URL + getMangaName().toLowerCase().replace(' ', '_')+'/';
	}
	
	/**
	 * Reload pageURLs and chapterURLs when the manga changes
	 */
	private void refreshLists(){
		chapterURLs = this.intializeChapterList();
		pageURLs = this.initalizePageList();
	}

}
