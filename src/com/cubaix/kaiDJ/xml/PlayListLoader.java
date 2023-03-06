package com.cubaix.kaiDJ.xml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.TreeMap;
import java.util.Vector;

import com.cubaix.kaiDJ.KaiDJ;
import com.cubaix.kaiDJ.db.SongDescr;
import com.cubaix.kaiDJ.db.SongGroup;

public class PlayListLoader {

	public PlayListLoader() {
	}

	/**
	 * @param parentJDJ
	 * @param pl
	 * @param aPathIn
	 */
	public static void save(KaiDJ parentJDJ, Vector pl, String aPath) {
		if (aPath.toLowerCase().endsWith(".m3u")) {
			saveM3U(parentJDJ, pl, aPath,"ISO-8859-1");
			return;
		}
		if (aPath.toLowerCase().endsWith(".m3u8")) {
			saveM3U(parentJDJ, pl, aPath,"UTF-8");
			return;
		}
		saveJDJ(parentJDJ, pl, aPath);
	}

	/**
	 * @param parentJDJ
	 * @param pl
	 * @param aPath
	 */
	public static void saveM3U(KaiDJ parentJDJ, Vector pl, String aPath,String currentCharset) {
		try {
			BufferedWriter aOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(aPath), currentCharset));
			aOut.write("#JDJ version soft=\"" + KaiDJ._VERSION + "\" pl=\"" + "1.0" + "\"\n");
			for (int s = 0; s < pl.size(); s++) {
				SongDescr aDescr = (SongDescr) pl.elementAt(s);
				if (aDescr instanceof SongGroup) {
					if (((SongGroup) aDescr).kind == SongGroup._KIND_OPEN) {
						aOut.write("#JDJ group name=\"" + aDescr.getPath() + "\"\n");
					} else {
						aOut.write("#JDJ /group\n");
					}
				} else {
					aOut.write(aDescr.getPath() + "\n");
				}
			}
			aOut.flush();
			aOut.close();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	/**
	 * @param parentJDJ
	 * @param pl
	 * @param aPathIn
	 */
	public static void saveJDJ(KaiDJ parentJDJ, Vector pl, String aPathIn) {
		String aPath;
		if (aPathIn.toLowerCase().endsWith(".jdj")) {
			aPath = aPathIn;
		} else {
			aPath = aPathIn + ".jdj";
		}
		try {
			// Create X structure
			Vector aXOs = new Vector();
			aXOs.add(new XTag("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
			aXOs.add(new XObject("\n", XObject.PUNCT));
			aXOs.add(new XTag("<version soft=\"" + KaiDJ._VERSION + "\" pl=\"" + "1.0" + "\"?>"));
			aXOs.add(new XObject("\n", XObject.PUNCT));
			String aDecal = "\t";
			for (int s = 0; s < pl.size(); s++) {
				SongDescr aDescr = (SongDescr) pl.elementAt(s);
				if (aDescr instanceof SongGroup) {
					if (((SongGroup) aDescr).kind == SongGroup._KIND_OPEN) {
						aXOs.add(new XObject(aDecal, XObject.PUNCT));
						aDecal += "\t";
						aXOs.add(new XTag("<group name=\"" + aDescr.getPath().replace("\"", "&ldquo;") + "\">"));
					} else {
						aDecal = aDecal.substring(1);
						aXOs.add(new XObject(aDecal, XObject.PUNCT));
						aXOs.add(new XTag("</group>"));
					}
				} else {
					aXOs.add(new XObject(aDecal, XObject.PUNCT));
					aXOs.add(new XTag("<song path=\"" + aDescr.getPath().replace("\"", "&ldquo;") + "\"\\>"));
				}
				aXOs.add(new XObject("\n", XObject.PUNCT));
			}
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
		if (aPath.toLowerCase().endsWith(".m3u")) {
			return loadM3U(parentJDJ, aPath,"ISO-8859-1");
		}
		if (aPath.toLowerCase().endsWith(".m3u8")) {
			return loadM3U(parentJDJ, aPath,"UTF-8");
		}
		return loadJDJ(parentJDJ, aPath);
	}

	/**
	 * @param parentJDJ
	 * @param aPath
	 * @return
	 */
	public static Vector loadM3U(KaiDJ parentJDJ, String aPath,String currentCharset) {
		Vector pl = new Vector();
		try {
			BufferedReader aIn = new BufferedReader(new InputStreamReader(new FileInputStream(aPath), currentCharset));
			String aLine;
			Vector aGroups = new Vector();
			String aDir = new File(aPath).getParentFile().getCanonicalPath();
			//EM 13/09/2008 : faster with all in a TreeMap
			TreeMap aAll = parentJDJ.db.getAllByPaths();
			while ((aLine = aIn.readLine()) != null) {
				if (aLine.toLowerCase().startsWith("#jdj group ")) {
					XTag aXTag = new XTag("<" + aLine + ">");
					pl.add(new SongGroup(SongGroup._KIND_OPEN, aXTag.getValue("name")));
					aGroups.add(pl.elementAt(pl.size() - 1));
				} else if (aLine.toLowerCase().startsWith("#jdj /group")) {
					//XTag aXTag = new XTag("<" + aLine + ">");
					pl.add(new SongGroup(SongGroup._KIND_CLOSE, ((SongGroup) aGroups.elementAt(aGroups.size() - 1)).getPath()));
					aGroups.remove(aGroups.size() - 1);
				} else if (!aLine.toLowerCase().startsWith("#")) {
					String aNewPath = aLine.trim();
					if(!aNewPath.matches("[a-zA-Z]:.*")){
						//Build a full path
						aNewPath = new File(aDir,aNewPath).getCanonicalPath();
					}
					//EM 13/09/2008 : faster with all in a TreeMap
					SongDescr aDescr = (SongDescr)aAll.get(aNewPath);//parentJDJ.db.searchPath(aNewPath);
					if (aDescr == null) {//Direct search
						aDescr = parentJDJ.db.searchPath(aNewPath);
					}
					if (aDescr == null) {
						aDescr = new SongDescr(new Integer(-1), aNewPath);
					}
					pl.add(aDescr);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
		return pl;
	}

	/**
	 * @param parentJDJ
	 * @param aPath
	 * @return
	 */
	public static Vector loadJDJ(KaiDJ parentJDJ, String aPath) {
		Vector pl = new Vector();
		Vector aXOs = new XLoader("UTF-8").load(aPath);
		Vector aGroups = new Vector();
		//EM 13/09/2008 : faster with all in a TreeMap
		TreeMap aAll = parentJDJ.db.getAllByPaths();
		for (int o = 0; o < aXOs.size(); o++) {
			XObject aXO = (XObject) aXOs.elementAt(o);
			if (aXO.kind == XObject.TAG) {
				XTag aXTag = (XTag) aXO;
				if (aXTag.tagName.equalsIgnoreCase("group")) {
					pl.add(new SongGroup(SongGroup._KIND_OPEN, aXTag.getValue("name")));
					aGroups.add(pl.elementAt(pl.size() - 1));
				} else if (aXTag.tagName.equalsIgnoreCase("/group")) {
					pl.add(new SongGroup(SongGroup._KIND_CLOSE, ((SongGroup) aGroups.elementAt(aGroups.size() - 1)).getPath()));
					aGroups.remove(aGroups.size() - 1);
				} else if (aXTag.tagName.equalsIgnoreCase("song")) {
					String aNewPath = aXTag.getValue("path").replaceAll("&ldquo;", "\"");
					//EM 13/09/2008 : faster with all in a TreeMap
					SongDescr aDescr = (SongDescr)aAll.get(aNewPath);//parentJDJ.db.searchPath(aNewPath);
					if (aDescr == null) {//Direct search
						aDescr = parentJDJ.db.searchPath(aNewPath);
					}
					if (aDescr == null) {
						aDescr = new SongDescr(new Integer(-1), aNewPath);
					}
					pl.add(aDescr);
				}
			}
		}
		return pl;
	}
}
