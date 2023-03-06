package com.cubaix.kaiDJ.xml;

import java.util.*;

public class XTag extends XObject {
	
	public String tagName;
	Vector names, values;
	
	public boolean parseOk = true;

	/**
	 * @param aStr
	 */
	public XTag(String aStr) {
		text = aStr;
		kind = TAG;

		names = new Vector();
		values = new Vector();

		parse();

		// System.err.println("XTag name=" + tagName + " Names = " + names +
		// " Values = " + values);
	}

    public XObject getClone(){
    	return new XTag(text);
    }

    /**
	 * 
	 */
	void parse() {
		parseOk = true;
		StringTokenizer aTokenizer = new StringTokenizer(text, " \t\n\r<>=\"\'", true);
		int aState = -1;
		String aTok = null, aTmpName = null, aName = null, aValue = null;

		while (aState >= -1 && aTokenizer.hasMoreTokens()) {
			switch (aState) {
			case 2: // Attribute = ''
				aTok = aTokenizer.nextToken();
				if ("\'".equals(aTok)) {
					// End
					names.add(aName);
					values.add(aValue);
					aName = null;
					aValue = null;
					aState = 0;
					continue;
				}
				aValue += aTok;
				break;
			case 1: // Attribute = ""
				aTok = aTokenizer.nextToken();
				if ("\"".equals(aTok)) {
					// End
					names.add(aName);
					values.add(aValue);
					aName = null;
					aValue = null;
					aState = 0;
					continue;
				}
				aValue += aTok;
				break;
			case 0:
			default:
				aTok = aTokenizer.nextToken();
				if (">".equals(aTok)) {
					aState = -2;
					if(aTokenizer.hasMoreElements()){
						// Error
						System.err.println("Warning, bad tag attribute : " + text);
						parseOk = false;
					}
					continue;
				}
				if ("<".equals(aTok)) {
					if(aState != -1){
						// Error
						System.err.println("Warning, bad tag attribute : " + text);
						parseOk = false;
					}
					//Begin of tag
					aState = 0;
					continue;
				}
				if (" ".equals(aTok) || "\t".equals(aTok) || "\r".equals(aTok) || "\n".equals(aTok)) {
					//No interest
					continue;
				}
				if ("=".equals(aTok)) {
					if(aTmpName == null){
						// Error
						System.err.println("Warning, bad tag attribute : " + text);
						parseOk = false;
					}
					aName = aTmpName;
					aValue = "";
					aTmpName = null;
					continue;
				}
				if ("\"".equals(aTok)) {
					if (aName == null) {
						// Error
						System.err.println("Warning, bad tag attribute : " + text);
						parseOk = false;
					}
					aState = 1;
					continue;
				}
				if ("\'".equals(aTok)) {
					if (aName == null) {
						// Error
						parseOk = false;
						System.err.println("Error, bad tag attribute : " + text);
					}
					aState = 2;
					continue;
				}
				if (aName != null && aName.length() > 0) {
					names.add(aName);
					values.add(aTok);
					aName = null;
					aValue = null;
					continue;
				}
				if (tagName == null) {
					tagName = aTok;
					if (tagName.startsWith("!")) {
						// Not a parsable tag !
						parseOk = false;
						return;
					}
					continue;
				}
				if(aTmpName != null){
					// Error
					System.err.println("Warning, bad tag attribute : " + text);
					parseOk = false;
				}
				aTmpName = aTok;
				break;
			}
		}
		//EM 29/05/2006
		// also take aAttrName into account : a possible attribute without a value
		if(aState >= 0 || aName != null || aValue != null || aTmpName != null){
			parseOk = false;
		}
	}

	/**
	 * @param aName
	 * @return
	 */
	public String getValue(String aName) {
		for (int i = 0; i < names.size(); i++) {
			if (aName.equalsIgnoreCase((String) names.elementAt(i))) {
				if (i > values.size()) {
					return null;
				}
				return (String) values.elementAt(i);
			}
		}
		return null;
	}
}
