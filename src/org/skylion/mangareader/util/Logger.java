package org.skylion.mangareader.util;

public class Logger {
	
	public static void log(Throwable t) {
		log(t.getMessage());
	}
	
	public static void log(String s) {
		System.out.println(s);
	}
}
