package lk.dialog.smsc.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class DbPropertyReader {

	private static String propFileName = "dbConfig.properties";
	
	private static Properties properties = new Properties();
	static {
	  String strCanonical = "./conf/" + propFileName;
		try {
		  String defaultPath = new File(strCanonical).getCanonicalPath();
			File configFile = new File(defaultPath);configFile.getAbsoluteFile();
			FileInputStream input = new FileInputStream(configFile);
			properties.load(input);
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Database config file (dbConfig.properties) not found. Exiting...");
			System.exit(2);
		}
	}

	public static String getPropertyValue(String property) {
		return properties.getProperty(property);
	}

}
