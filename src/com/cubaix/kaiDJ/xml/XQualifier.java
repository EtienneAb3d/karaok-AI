package com.cubaix.kaiDJ.xml;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

public class XQualifier {
	//Default values
	static public char nbsp = (char)160;
	static public String nbspStr = ""+nbsp;
	static public String PUNCTS = " ,;:.!?()[]{}\"\'*/+-=|\\�\n\r\t" 
		+ nbsp;
	static public String NL_AFTER_CHAR_RE = "[\n\r]";
	static public String NL_AFTER_TAG_RE = "(/(p|tr|h[0-9]|il|title|head)|br)";
	static public String INLINE_TAG_RE = "/?(b|i|u|a|img|font|span)";
	static public String SPECIAL_EMPTY_RE = "(br|hr)";
	static public boolean USE_ALPHA_ENTITIES = false;
	static public String EDITABLE_TAG_RE = "(b|i|u|a|img|font|span)";
	static public String HIDDEN_FIELD_RE = "script";
	
	static Properties properties = null;

	//EM 06/06/2006 : char entities is now here
	// Create and load charEntities table
	static public Hashtable alphaentitiesToNum = new Hashtable();
	static public Hashtable numToAlphaentities = new Hashtable();

	final static String charEntityRessource = "charEntities.tsv";

	//IMPORTANT
	//Always init the settings  
	static boolean initOk = init();
	
	/**
	 * 
	 */
	static boolean init(){
		properties = new Properties();
		try{
//			InputStream aIS = new FileInputStream(ServerLocationProperties.getInstance().getRootDir() + "/xml.properties");//XQualifier.class.getResourceAsStream("xml.properties");
//			if(aIS == null){
//				//Not found, keep default
//				log.debug("Can't find XML properties, will use default values");
//				return false;
//			}
//			properties.load(aIS);
//			String aStr;
//			if((aStr = properties.getProperty("nbsp")) != null){
//				nbsp = (char)Integer.parseInt(aStr);
//			}
//			if((aStr = properties.getProperty("poncts")) != null){
//				PONCTS = aStr + nbsp;
//			}
//			if((aStr = properties.getProperty("nl.after.char.re")) != null){
//				NL_AFTER_CHAR_RE = aStr;
//			}
//			if((aStr = properties.getProperty("nl.after.tag.re")) != null){
//				NL_AFTER_TAG_RE = aStr;
//			}
//			if((aStr = properties.getProperty("inline.tag.re")) != null){
//				INLINE_TAG_RE = aStr;
//			}
//			if((aStr = properties.getProperty("editable.tag.re")) != null){
//				EDITABLE_TAG_RE = aStr;
//			}
//			if((aStr = properties.getProperty("hidden.field.re")) != null){
//				HIDDEN_FIELD_RE = aStr;
//			}
//			if((aStr = properties.getProperty("special.empty.re")) != null){
//				SPECIAL_EMPTY_RE = aStr;
//			}
//			if((aStr = properties.getProperty("use.alpha.entities")) != null){
//				USE_ALPHA_ENTITIES = (Integer.parseInt(aStr) > 0);
//			}
		}
		catch(Throwable t){
			System.err.println("Can't load XML properties, will use default values" + t);
			return false;
		}
		
		//EM 06/06/2006
		try{
			loadCharEntities();
		}
		catch(Throwable t){
			System.err.println("Can't load char entities"+t);
			return false;
		}

		return true;
	}

	//EM 06/06/2006 : char entities are now here
	/**
	 * 
	 */
	static void loadCharEntities() {
		if (!alphaentitiesToNum.isEmpty()) {
			// Already loaded ?
			return;
		}
		try {
			// Load charEntities ressource
			BufferedReader aBR = new BufferedReader(new InputStreamReader(XLoader.class.getResourceAsStream(charEntityRessource)));
			String aLine;
			while ((aLine = aBR.readLine()) != null) {
				StringTokenizer aST = new StringTokenizer(aLine, "\t", false);
				String aEntAlpha = aST.nextToken();
				/*String aEntNum =*/ aST.nextToken();
				String aExa = aST.nextToken();
				Integer aNum = Integer.decode(aExa);
				alphaentitiesToNum.put(aEntAlpha, aNum);
				numToAlphaentities.put(aNum,aEntAlpha);
			}
		} catch (Throwable t) {
			System.err.println("Can't load entity char table !"+ t);
			System.exit(-1);
		}
	}

	/**
	 * @param aEntity
	 * @return
	 */
	static public String transcodeFromEntity(String aEntity) {
		Integer aValue = null;
		if ((aValue = (Integer) XQualifier.alphaentitiesToNum.get(aEntity)) != null) {
			// Found in table
			char aChar = (char) (aValue.intValue() & 0xFFFF);
			return "" + aChar;
		}
		if (aEntity.matches("&#[0-9]+;")) {
			// Decode num
			char aChar = (char) (Integer.parseInt(aEntity.substring(2, aEntity.length() - 1)) & 0xFFFF);
			return "" + aChar;
		}
		if (aEntity.matches("&#[xX][0-9A-Fa-f]+;")) {
			// Decode hexa
			String aHexa = "0x" + aEntity.substring(3, aEntity.length() - 1);
			char aChar = (char) (Integer.decode(aHexa).intValue() & 0xFFFF);
			return "" + aChar;
		}
		System.err.println("Unknown entity '" + aEntity + "', will be kept like this");
		// Not found
		return null;
	}

	//EM 06/06/2006 : char entities moved here
	/**
	 * @param aStrIn
	 * @return
	 */
	static public String transcodeToEntities(String aStrIn,boolean aUseLowCharTable) {
		StringBuffer aSB = new StringBuffer();
		char aChar;
		int aInt;
		String aEntity;
		int aEntLen;
		for (int i = 0; i < aStrIn.length(); i++) {
			aChar = aStrIn.charAt(i);
			aInt = aChar & 0xFFFFFFFF;
			//First check for a & < > conversions
			if(aChar == '&'){
				if((aEntLen = XQualifier.entityLen(aStrIn,i)) > 0){
					//Ok it's an entity
					aSB.append(aStrIn.substring(i,i + aEntLen));
					i += aEntLen - 1;
					continue;
				}
				//Not en entity, need to be transcoded
				aSB.append("&amp;");
				continue;
			}
			else if(aChar == '<'){
				//Must be transcoded
				aSB.append("&lt;");
				continue;
			}
			else if(aChar == '>'){
				//Must be transcoded
				aSB.append("&gt;");
				continue;
			}
			//Check for std conversions
			//EM 06/06/2006
			//if(charsetChecked && aInt <= 255) {
			if(aUseLowCharTable && aInt <= 255) {
				//For known charset, use low char table
				aSB.append(aChar);
			}
			else{
				//Search for a conversion
				if(XQualifier.USE_ALPHA_ENTITIES && (aEntity = (String)XQualifier.numToAlphaentities.get(new Integer(aInt))) != null){
					//Ok, alpha entity found
					aSB.append(aEntity);
				}
				else if(aInt <= 255){
					//Style use low char table
					aSB.append(aChar);
				}
				else{
					//Use decimal entity
					aSB.append("&#" + (aChar & 0xFFFF) + ";");
				}
			}  
		}
		return aSB.toString();
	}

	/**
	 * @param aChar
	 * @return
	 */
	static public boolean useAlphaEntities(){
		return USE_ALPHA_ENTITIES;
	}

	/**
	 * @param aChar
	 * @return
	 */
	static public boolean isPonctChar(char aChar){
		return PUNCTS.indexOf(aChar) >= 0;
	}
	
	/**
	 * @param aStr
	 * @param aStartPos
	 * @return
	 */
	static public int entityLen(String aStr,int aStartPos){
		if(aStr.charAt(aStartPos) != '&'){
			//Not possible
			return -1;
		}
		int aPos = aStartPos;
		String aChar;
		while(aPos < aStr.length()){
			aChar = aStr.substring(aPos,aPos+1);
			if(aChar.equals(";")){
				//Could be the end of entity
				if(aStr.substring(aStartPos,aPos + 1).matches("&([a-zA-Z]+|#[0-9]+|#0[xX][0-9a-fA-F]+);")){
					//Ok entity
					return aPos + 1 - aStartPos;
				}
				//Not a good entity
				return -1;
			}
			if(!aChar.matches("[&#0-9a-zA-Z;]")){
				//Impossible
				return -1;
			}
			aPos++;
		}
		return -1;
	}

	/**
	 * @param aXLT
	 * @return
	 */
//	static public boolean nlAfter(XLineToken aXLT) {
//		switch(aXLT.kind){
//		case XLineToken.COLLAPSE:
//			for(int o = 0;o < ((XLineTokenCollapse)aXLT).chain.size();o++){
//				if(nlAfter((XObject)((XLineTokenCollapse)aXLT).chain.elementAt(o))){
//					return true;
//				}
//			}
//			return false;
//		default:
//			return nlAfter(aXLT.xO);
//		}
//		//return false;
//	}

	/**
	 * @param aXO
	 * @return
	 */
	static public boolean nlAfter(XObject aXO) {
		if(aXO.text.matches(NL_AFTER_CHAR_RE)){
			return true;
		}
		if (aXO.kind != XObject.TAG) {
			return false;
		}
		if (((XTag) aXO).tagName.toLowerCase().matches(NL_AFTER_TAG_RE)) {
			return true;
		}
		return false;
	}

	/**
	 * @param aT
	 * @return
	 */
	static public boolean isInline(XTag aT){
		return aT.tagName.toLowerCase().matches(INLINE_TAG_RE);
	}

	/**
	 * @param aT
	 * @return
	 */
	static public boolean isEditableTag(XTag aT){
		return aT.tagName.toLowerCase().matches(EDITABLE_TAG_RE);
	}

	/**
	 * @param aT
	 * @return
	 */
	static public boolean isHiddenField(XTag aT){
		return aT.tagName.toLowerCase().matches(HIDDEN_FIELD_RE);
	}

	/**
	 * @return
	 */
	static public boolean isSpecialEmpty(XTag aT){
		return aT.tagName.toLowerCase().matches(SPECIAL_EMPTY_RE);
	}
	
	/**
	 * @return
	 */
	static public boolean isOpen(XTag aT){
		if(isSpecialEmpty(aT)){
			return false;
		}
		if(aT.text.matches("<( */.*|.*/ *)>")){
			return false;
		}
		return true;
	}

	/**
	 * @return
	 */
	static public boolean isClose(XTag aT){
		return aT.text.matches("< */.*>");
	}
	
	/**
	 * @return
	 */
	static public boolean isEmpty(XTag aT){
		if(isSpecialEmpty(aT)){
			return true;
		}
		return aT.text.matches("<.*/ *>");
	}

}
