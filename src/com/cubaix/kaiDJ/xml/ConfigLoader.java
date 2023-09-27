package com.cubaix.kaiDJ.xml;

import java.util.Vector;

import com.cubaix.kaiDJ.KaiDJ;

public class ConfigLoader {
	public ConfigLoader() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @param parentJDJ
	 * @param aPathIn
	 */
	public static void save(KaiDJ parentJDJ, String aPathIn){

		String aPath;
		if (aPathIn.toLowerCase().endsWith(".xml")) {
			aPath = aPathIn;
		} else {
			aPath = aPathIn + ".xml";
		}
		try {
			// Create X structure
			Vector aXOs = new Vector();
			aXOs.add(new XTag("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
			aXOs.add(new XObject("\n", XObject.PUNCT));
			aXOs.add(new XTag("<version soft=\"" + KaiDJ._VERSION + "\"?>"));
			aXOs.add(new XObject("\n", XObject.PUNCT));
			//String aDecal = "\t";

			aXOs.add(new XTag("<user email=\"" + parentJDJ.userEMail + "\" code=\"" + parentJDJ.userCode + "\" welcomed=\"true\">"));
			aXOs.add(new XObject("\n", XObject.PUNCT));

			aXOs.add(new XTag("<soundcard1 id=\"" + parentJDJ.soundCard1 + "\">"));
			aXOs.add(new XObject("\n", XObject.PUNCT));

			aXOs.add(new XTag("<soundcard2 id=\"" + parentJDJ.soundCard2 + "\">"));
			aXOs.add(new XObject("\n", XObject.PUNCT));

			aXOs.add(new XTag("<soundcardPre id=\"" + parentJDJ.soundCardPre + "\">"));
			aXOs.add(new XObject("\n", XObject.PUNCT));

			new XLoader("UTF-8").save(aPath, aXOs);
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
	
	/**
	 * @param parentJDJ
	 * @param aPath
	 * @return
	 */
	public static Vector load(KaiDJ parentJDJ, String aPath) {
		Vector pl = new Vector();
		Vector aXOs = new XLoader("UTF-8").load(aPath);
		//Vector aGroups = new Vector();
		for (int o = 0; o < aXOs.size(); o++) {
			XObject aXO = (XObject) aXOs.elementAt(o);
			if (aXO.kind == XObject.TAG) {
				XTag aXTag = (XTag) aXO;
				if (aXTag.tagName.equalsIgnoreCase("user")) {
					parentJDJ.userEMail = aXTag.getValue("email");
					parentJDJ.userCode = aXTag.getValue("code");
					if(aXTag.getValue("welcomed") != null) {
						parentJDJ.welcomed = true;
					}
				}
				else if (aXTag.tagName.equalsIgnoreCase("soundcard1")) {
					parentJDJ.soundCard1 = Integer.parseInt(aXTag.getValue("id"));
				}
				else if (aXTag.tagName.equalsIgnoreCase("soundcard2")) {
					parentJDJ.soundCard2 = Integer.parseInt(aXTag.getValue("id"));
				}
				else if (aXTag.tagName.equalsIgnoreCase("soundcardPre")) {
					parentJDJ.soundCardPre = Integer.parseInt(aXTag.getValue("id"));
				}
			}
		}
		return pl;
	}
}
