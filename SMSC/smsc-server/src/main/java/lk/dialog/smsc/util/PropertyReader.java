package lk.dialog.smsc.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class PropertyReader {

	private static String propFileName = "sim_config.properties";
	
	private static Properties properties = new Properties();
	static {
		try {
		  String defaultPath = new File("./conf/" + propFileName).getCanonicalPath();
		  
			File configFile = new File(defaultPath);configFile.getAbsoluteFile();
			FileInputStream input = new FileInputStream(configFile);
			properties.load(input);
			input.close();
		} catch (Exception e) {
		  e.printStackTrace();
      System.out.println("Application config file (sim_config.properties) not found. Exiting...");
      System.exit(3);
		}
	}

	public static String getPropertyValue(String property) {
		return properties.getProperty(property);
	}

}
