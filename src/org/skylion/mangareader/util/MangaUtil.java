package org.skylion.mangareader.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A class that contains utility methods for the MangaEngine.
 * @author Skylion
 */
public class MangaUtil {

	private MangaUtil(){};
	
	/**
	 * Retrieves a list of licensed Manga from Anime News Network. 
	 * @return A list of Manga licensed in English.
	 * @throws IOException If it cannot complete the request.
	 */
	public static List<String> getLicensedManga() throws IOException{
		StringBuilder sb = 
				new StringBuilder("http://www.animenewsnetwork.com/encyclopedia/anime-list.php");
		sb.append("?licensed=1");
		sb.append("&sort=title");
		sb.append("&showG=1");
		Document doc = Jsoup.connect(sb.toString()).maxBodySize(0).get();
		Elements list = doc.getElementsByClass("HOVERLINE");
		List<String> blackList = new ArrayList<String>(list.size());
		for(Element e: list){
			String title = e.text();
			if(title.startsWith("(The)")){
				title = title.replace("(The)", "The");
			}
			if(title.contains("(")){
				title = title.substring(0, title.lastIndexOf('(')).trim();
			}
			blackList.add(title);
		}
		return blackList;
	}
	
	/**
	 * Removes licensed manga from a list of JSoup Elements
	 * @param items The JSoup Elements you want to cleanse.
	 * @return The original Elements without licensed Manga.
	 */
	public static Elements removeLicensedManga(Elements items){
		try{
			List<String> blackList = MangaUtil.getLicensedManga();
			for(int i = 0; i<items.size(); i++){
				Element item = items.get(i);
				if(blackList.contains(item.text().trim())){
					items.remove(item);
					i--;//Removes licensed Manga (deadEnd links)
				}
			}
			return items;
		}
		catch(Exception ex){
			Logger.log(ex);
			return null;
		}
	}
}
