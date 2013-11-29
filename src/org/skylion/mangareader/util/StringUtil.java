package org.skylion.mangareader.util;

/**
 * Utility class that houses commonly used methods for String parsing
 * @author Skylion
 *
 */
public class StringUtil {

	private StringUtil(){};//Prevents instantiation
	
	/**
	 * Checks if it contains a number
	 * @param input The String you want to check
	 * @return True if it does, false otherwise
	 */
	public static boolean containsNum(String input){
		for(char c: input.toCharArray()){
			if(Character.isDigit(c)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if the number is parsable into a int or double (contains only num)
	 * @param input
	 * @return
	 */
	public static boolean isNum(String input){
		for(char c: input.toCharArray()){
			if(!Character.isDigit(c) || c!='.'){//Special case for decimals
				return false;
			}
		}
		return true;
	}
	
}
