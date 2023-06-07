package com.cubaix.kai;

import org.eclipse.swt.graphics.Color;

public class ScreenerSettingsFilesTools {
	
	/**
	 * Convert a String extracted from a screener settings file to a Color Object.
	 * Ex : "10,0,100" => RGB(10,0,100)
	 * @param RgbString
	 * @return a Color Object matching RGB saved in the string or white if an error as been caught.
	 */
	public static Color stringToColor(String RgbString) {
		String[] values = RgbString.split(",");
		
		try {
			int red = Integer.valueOf(values[0]);
			int green = Integer.valueOf(values[1]);
			int blue = Integer.valueOf(values[2]);
			return new Color(red, green, blue);
			
		} catch (Exception e) {
			System.out.println("Corrupted string : " + e);
			return new Color(0, 0, 0);
		}
	}
	
	/**
	 * Convert a Color Object into a formated String to save RGB values in settings file.
	 * Ex : RGB(10,0,100) => "10,0,100"
	 * @param color
	 * @return string
	 */
	public static String colorToSettingsString(Color color) {
		return color.getRed() + "," + color.getGreen() + ","+ color.getBlue();
	}
	
}
