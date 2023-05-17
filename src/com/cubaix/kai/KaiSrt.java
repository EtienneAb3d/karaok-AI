package com.cubaix.kai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Vector;

import com.cubAIx.WhisperTimeSync.WhisperTimeSync;
import com.cubaix.kaiDJ.utils.FileUtils;

public class KaiSrt {
	static final boolean _DEBUG = false;
	String path = null;
	String text = null;
	String srt = null;
	String lng = "xx";
	
	public Vector<ChunkStr> chunks = new Vector<ChunkStr>();
	
//	public Vector<String> chunkTexts = new Vector<String>();
//	public Vector<Long> chunkStarts = new Vector<Long>();
//	public Vector<Long> chunkStops = new Vector<Long>();

	public KaiSrt(String aPath) {
		path = aPath;
		load();
	}
	
	public int newGetChunkIdx(long timeStart, long timeEnds) {
		if(chunks.size() <= 0) {
			return -1;
		}
		for (int i = 0; i < chunks.size(); i++) {
			if(chunks.get(i).getStartTime() == timeStart && chunks.get(i).getEndTime() == timeEnds) return i;
		}
		return -1;
	}
	
//	public int newGetChunkIdx(long timeStart, long timeEnds) {
//		if(chunkStarts.size() <= 0) {
//			return -1;
//		}
//		for (int i = 0; i < chunkStarts.size(); i++) {
//			if(chunkStarts.get(i) == timeStart && chunkStops.get(i) == timeEnds) return i;
//		}
//		return -1;
//	}
	
	public int getChunkIdx(long aTimeMS) {
		if(chunks.size() <= 0) {
			return -1;
		}
		if(aTimeMS < chunks.elementAt(0).getStartTime()) {
			return -1;
		}
		for(int l = 0;l < chunks.size();l++) {
			if(chunks.elementAt(l).getStartTime() <= aTimeMS && chunks.elementAt(l).getEndTime() > aTimeMS) {
				return l;
			}
		}
		for(int l = 0;l < chunks.size();l++) {
			if(chunks.elementAt(l).getEndTime() > aTimeMS) {
				return l-1;
			}
		}
		if(aTimeMS > chunks.elementAt(chunks.size()-1).getEndTime()) {
			return chunks.size();
		}
		return -1;
	}
	
//	public int getChunkIdx(long aTimeMS) {
//		if(chunks.size() <= 0) {
//			return -1;
//		}
//		if(aTimeMS < chunkStarts.elementAt(0)) {
//			return -1;
//		}
//		for(int l = 0;l < chunkTexts.size();l++) {
//			if(chunkStarts.elementAt(l) <= aTimeMS && chunkStops.elementAt(l) > aTimeMS) {
//				return l;
//			}
//		}
//		for(int l = 0;l < chunkTexts.size();l++) {
//			if(chunkStops.elementAt(l) > aTimeMS) {
//				return l-1;
//			}
//		}
//		if(aTimeMS > chunkStops.elementAt(chunkStops.size()-1)) {
//			return chunkStops.size();
//		}
//		return -1;
//	}
	
	public int getChunkIdxJustBefore(long aTimeMS) {
		for(int l = 0;l < chunks.size();l++) {
			if(chunks.elementAt(l).getStartTime() <= aTimeMS+1000 && chunks.elementAt(l).getStartTime() > aTimeMS) {
				return l;
			}
		}
		return -1;
	}
	
//	public int getChunkIdxJustBefore(long aTimeMS) {
//		for(int l = 0;l < chunkTexts.size();l++) {
//			if(chunkStarts.elementAt(l) <= aTimeMS+1000 && chunkStarts.elementAt(l) > aTimeMS) {
//				return l;
//			}
//		}
//		return -1;
//	}
	
	public String getChunkText(long aTimeMS) {
		int aIdx = getChunkIdx(aTimeMS);
		if(aIdx < 0) {
			return "";
		}
		return chunks.elementAt(aIdx).getText();
	}
//	
//	public String getChunkText(long aTimeMS) {
//		int aIdx = getChunkIdx(aTimeMS);
//		if(aIdx < 0) {
//			return "";
//		}
//		return chunkTexts.elementAt(aIdx);
//	}
	
	public void load() {
		try {
			String aKsrtPath = path+".ksrt";
			if(!new File(aKsrtPath).exists()) {
				FileUtils.unzipDir(path+".kai", new File(aKsrtPath).getParentFile().getCanonicalPath(), false);
			}
			if(new File(aKsrtPath+".edited").exists()) {
				aKsrtPath = aKsrtPath+".edited";
			}
			BufferedReader aBR = new BufferedReader(new InputStreamReader(new FileInputStream(aKsrtPath), "utf-8"));
			String aLine;
			StringBuffer aSB = new StringBuffer(); 
			while((aLine = aBR.readLine()) != null) {
				aLine = aLine.trim().replaceAll("\\\\n", "\n")
						.replaceAll("\r*\n", "\n");
				if(aSB.length() > 0) {
					aSB.append("\n");
				}
				aSB.append(aLine);
			}
			String aKsrt = aSB.toString();
			if(_DEBUG) {
				System.out.println(aKsrt);
			}
			if(aKsrt.indexOf("\n1\n") >= 0) {
				text = aKsrt.substring(0, aKsrt.indexOf("\n1\n00:"));
				srt = ("\n\n"+aKsrt.substring(aKsrt.indexOf("\n1\n00:")+1))
						.replaceAll("\n\n[0-9]+\n", "\n\n").substring(2);
			}
			else if(aKsrt.indexOf("\n00:") >= 0) {
				text = aKsrt.substring(0, aKsrt.indexOf("\n00:"));
				srt = ("\n\n"+aKsrt.substring(aKsrt.indexOf("\n00:")+1))
						.replaceAll("\n\n[0-9]+\n", "\n\n").substring(2);
			}
			else if(aKsrt.indexOf("\n") >= 0){
				text = aKsrt.substring(0, aKsrt.indexOf("\n"));
				srt = ("\n\n"+aKsrt.substring(aKsrt.indexOf("\n")+1))
						.replaceAll("\n\n[0-9]+\n", "\n\n").substring(2);
			}
			aBR.close();
			
			if(_DEBUG) {
				System.out.println("LOADED\n"
						+ "----------\n"
						+ "TEXT="+text+"\n"
						+ "----------\n"
						+ "SRT="+srt+"\n"
						);
			}
			
			resyncText2Srt();
			srt2Text();
			
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	public void save(String aPath) {
		try {
			BufferedWriter aBW = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(aPath)
					,"UTF8"));
			aBW.write(text+"\n"+srt);
			aBW.flush();
			aBW.close();
		} catch (Exception e) {
		}
	}
	
	public void resyncText2Srt() throws Exception {
		WhisperTimeSync aWTS = new WhisperTimeSync();
		srt = srt.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("\r*\n", "\n");
		text = text.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("\r*\n", "\n");
		String aSync = aWTS.processString(srt, text, lng);
		aSync = aSync.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("\r*\n", "\n");
		boolean aEmptyFound=true;
		StringBuffer aSB = new StringBuffer();
		
//		chunkTexts = new Vector<String>();
//		chunkStarts = new Vector<Long>();
//		chunkStops = new Vector<Long>();
		chunks = new Vector<ChunkStr>();
		
		for(String aL : aSync.split("\n")) {
			
			ChunkStr chunk = new ChunkStr();
			
			aL = aL.trim();
			if(aL.isEmpty()) {
				aSB.append("\n");
				aEmptyFound = true;
				continue;
			}
			if(aEmptyFound && aL.matches("[0-9]+")) {
				//Ignore
				continue;
			}
			if(aL.matches("[0-9][0-9]:[0-9][0-9]:[0-9][0-9][.,][0-9][0-9][0-9] --> [0-9][0-9]:[0-9][0-9]:[0-9][0-9][.,][0-9][0-9][0-9]")) {
				
//				int firstLength = aSB.length()-1;
				aSB.append(aL);
//				int secondLength = aSB.length()-1;
				
				aSB.append("\n");
				
//				chunk.setEditorTimestampLine(firstLength, secondLength);
//				chunkTexts.add("");
				chunk.setText("");
				Integer aStartH = Integer.parseInt(aL.substring(0, 2));
				Integer aStartM = Integer.parseInt(aL.substring(3, 5));
				Integer aStartS = Integer.parseInt(aL.substring(6, 8));
				Integer aStartMS = Integer.parseInt(aL.substring(9, 12));
//				chunkStarts.add((long)(aStartH*60*60*1000+aStartM*60*1000+aStartS*1000+aStartMS));
				chunk.setStartTime((long)(aStartH*60*60*1000+aStartM*60*1000+aStartS*1000+aStartMS));
				Integer aStopH = Integer.parseInt(aL.substring(17, 19));
				Integer aStopM = Integer.parseInt(aL.substring(20, 22));
				Integer aStopS = Integer.parseInt(aL.substring(23, 25));
				Integer aStopMS = Integer.parseInt(aL.substring(26, 29));
//				chunkStops.add((long)(aStopH*60*60*1000+aStopM*60*1000+aStopS*1000+aStopMS));
				chunk.setEndTime((long)(aStopH*60*60*1000+aStopM*60*1000+aStopS*1000+aStopMS));
				chunks.add(chunk);
				continue;
			}
			if(chunks.size() <= 0) {
				//No chunk??
				aSB.append(aL);
				aSB.append("\n");
				continue;
			}
//			String aCurrent = chunkTexts.elementAt(chunkTexts.size()-1);
			String aCurrent = chunks.elementAt(chunks.size()-1).getText();
			if(aCurrent.length() > 0) {
				aSB.append("\n");
				aCurrent += "\n";
			}
			aSB.append(aL);
			aCurrent += aL;
//			chunkTexts.setElementAt(aCurrent,chunkTexts.size()-1);
			chunks.get(chunks.size()-1).setText(aCurrent);
			
		}
		srt = aSB.toString().trim();
	}
	
	public void srt2Text() {
		boolean aInText = false;
		StringBuffer aSB = new StringBuffer();
		srt = srt.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("\r*\n", "\n");
		if(_DEBUG) {
			System.out.println("srt2Text() srt="+srt);
		}
		for(String aL : srt.split("\n")) {
			aL = aL.trim();
			if(_DEBUG) {
				System.out.println("srt2Text() aL="+aL);
			}
			if(aL.isEmpty()) {
				aInText = false;
				if(_DEBUG) {
					System.out.println("srt2Text() INTEXT OFF");
				}
			}
			if(aL.matches("[0-9][0-9]:[0-9][0-9]:[0-9][0-9][.,][0-9][0-9][0-9] --> [0-9][0-9]:[0-9][0-9]:[0-9][0-9][.,][0-9][0-9][0-9]")) {
				aInText = true;
				if(_DEBUG) {
					System.out.println("srt2Text() INTEXT ON");
				}
				continue;
			}
			if(aInText) {
				aSB.append(aL);
				aSB.append("\n");
				if(_DEBUG) {
					System.out.println("srt2Text() ADDED TXT="+aL);
				}
			}
		}
		text = aSB.toString();
		if(_DEBUG) {
			System.out.println("srt2Text() text="+text);
		}
	}
	
	public static void main(String[] args) {
		try {
			KaiSrt aSrt = new KaiSrt("/home/cubaix/Musique/MP3/Soiree3eme/Best of 2011/Katy Perry - Firework.mp3");
			for(int l = 0;l < aSrt.chunks.size();l++) {
				System.out.println(aSrt.chunks.elementAt(l).getStartTimeToFormatString()+" --> "+aSrt.chunks.elementAt(l).getEndTimeToFormatString()+" "+aSrt.chunks.elementAt(l).getText());
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	public void setLinesChunk(String text2) {
		// TODO Auto-generated method stub
		for (ChunkStr chunk : chunks) {
			String timestampLine = chunk.getStartTimeToFormatTimestamp() +" --> "+chunk.getEndTimeToFormatTimestamp();
			int index = text2.indexOf(timestampLine);
			chunk.setEditorTimestampLine(index, index+29);
		}
	}
	
//	public static void main(String[] args) {
//		try {
//			KaiSrt aSrt = new KaiSrt("/home/cubaix/Musique/MP3/Soiree3eme/Best of 2011/Katy Perry - Firework.mp3");
//			for(int l = 0;l < aSrt.chunkTexts.size();l++) {
//				System.out.println(aSrt.chunkStarts.elementAt(l)+" --> "+aSrt.chunkStops.elementAt(l)+" "+aSrt.chunkTexts.elementAt(l));
//			}
//		} catch (Exception e) {
//			e.printStackTrace(System.err);
//		}
//	}

}
