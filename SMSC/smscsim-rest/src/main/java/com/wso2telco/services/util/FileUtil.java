package com.wso2telco.services.util;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class FileUtil extends Thread {

	private static Properties settings = new Properties();
	private long modifiedTime;
	private int reloadInterval;
	private File file;
	
	/**
	 * 
	 */
	public FileUtil(String filePath) {
		try {
			file = new File(filePath);
			if(!file.exists()) {
//				System.out.println("Settings file not found!. ");
				String defaultPath = new File("./conf/settings.properties").getCanonicalPath();
//				System.out.println("Looking for file in default location..."+new File("./conf").getCanonicalPath());
				file = new File(defaultPath);
//				file = new File("settings.properties");
				if(!file.exists()) {
					System.out.println("settings.properties File not found in default location. Exiting");
					System.exit(0);
				} else {
				  System.out.println("Settings file found at " + file.getAbsolutePath());
				}
			}
			settings.load(new FileReader(file));
			modifiedTime = file.lastModified();
			reloadInterval = Integer.parseInt(settings.getProperty("reload_interval"));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static String getRegex() {
		return settings.getProperty("regex");
	}
	/**
	 * 
	 * @return
	 */
	public static String getPort() {
		return settings.getProperty("port");
	}

	public static String getPropertyValue(String property) {
	  return settings.getProperty(property);
	}

	@Override
	public void run() {
		while(true) {
			try {
				if(modifiedTime != file.lastModified()) {
					modifiedTime = file.lastModified();
					settings.load(new FileReader(file));
					reloadInterval = Integer.parseInt(settings.getProperty("reload_interval"));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				sleep(reloadInterval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new FileUtil("D:/smsmessaging/SourceFilesv7/smscsim-rest/conf").start();
	}
}
