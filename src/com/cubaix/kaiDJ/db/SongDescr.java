package com.cubaix.kaiDJ.db;

import java.io.File;
import java.text.NumberFormat;

import com.cubaix.kai.KaiSrt;

public class SongDescr {
	static NumberFormat ddNF;
	static {
		ddNF = NumberFormat.getInstance();
		ddNF.setMaximumIntegerDigits(2);
		ddNF.setMinimumIntegerDigits(2);
	}

	public Integer id = null;
	public String path = null;
	public String author = "[UNKNOWN]";
	public String title = "[UNKNOWN]";
	public String album = "[UNKNOWN]";

	public long duration = -1;
	String durationDisplay = "";
	public int hash = -1;
	int enablePlay = 0;//0 : undefined (not in play list), 1 : play, -1 : don't play
	
	Integer plID = new Integer((int)(Math.random() * Integer.MAX_VALUE));
	
	public KaiSrt kaiSrt = null;

	/**
	 * @param aId
	 * @param aPath
	 */
	public SongDescr(Integer aId, String aPath) {
		id = aId;
		path = aPath;
		hash = path.toLowerCase().hashCode();
		if(new File(path+".kai").exists()) {
			kaiSrt = new KaiSrt(path);
		}
	}

	/**
	 * @param aId
	 * @param aPath
	 * @param aAuthor
	 * @param aTitle
	 * @param aAlbum
	 * @param aDuration
	 */
	public SongDescr(Integer aId, String aPath, String aAuthor, String aTitle,
			String aAlbum, long aDuration) {
		id = aId;
		path = aPath;
		hash = path.toLowerCase().hashCode();
		set(aAuthor, aTitle, aAlbum, aDuration);
		if(new File(path+".kai").exists()) {
			kaiSrt = new KaiSrt(path);
		}
	}

	/**
	 * @param aId
	 * @param aPath
	 * @param aAuthor
	 * @param aTitle
	 * @param aAlbum
	 * @param aDuration
	 * @param aHash
	 */
	public SongDescr(Integer aId, String aPath, String aAuthor, String aTitle,
			String aAlbum, long aDuration, int aHash) {
		this(aId, aPath, aAuthor, aTitle, aAlbum, aDuration);
		if (aHash != 0) {
			hash = aHash;
		}
	}
	
	//Use this to build a copy with a new plID
	public SongDescr(SongDescr aDescr){
		this(aDescr.id,aDescr.path,aDescr.author,aDescr.title,aDescr.album,aDescr.duration,aDescr.hash);
	}

	/**
	 * @param aAuthor
	 * @param aTitle
	 * @param aAlbum
	 * @param aDuration
	 */
	public void set(String aAuthor, String aTitle, String aAlbum, long aDuration) {
		author = aAuthor;
		title = aTitle;
		album = aAlbum;
		duration = aDuration;

		durationDisplay = durationToDisplay(duration);
	}
	
	/**
	 * @param aTime
	 * @return
	 */
	static public String durationToDisplay(long aTime){
		long aSecAll = aTime / 1000000;
		long aDays = aSecAll / (3600 * 24);
		long aHours = (aSecAll / 3600)%24;
		long aMin = (aSecAll / 60)%60;
		long aSec = aSecAll % 60;
		//long aMs = (aTime % 1000) / 10;
		if(aDays > 0){
			return aDays + ":" + aHours + ":" + ddNF.format(aMin) + ":" + ddNF.format(aSec);
		}
		if(aHours > 0){
			return aHours + ":" + ddNF.format(aMin) + ":" + ddNF.format(aSec);
		}
		return ddNF.format(aMin) + ":" + ddNF.format(aSec);
	}

	public String getAlbum() {
		if (album == null) {
			return "[ERROR]";
		}
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getAuthor() {
		if (author == null) {
			return "[ERROR]";
		}
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getDurationDisplay() {
		if (durationDisplay == null) {
			return "[ERROR]";
		}
		return durationDisplay;
	}

	public void setDurationDisplay(String durationDisplay) {
		this.durationDisplay = durationDisplay;
	}

	public Integer getId() {
		if (id == null) {
			return new Integer(-1);
		}
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getPath() {
		if (path == null) {
			return "[ERROR]";
		}
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	//EM 28/04/2009
	public boolean isAccesible(){
		return new File(this.path).exists();
	}
	
	public String getTitle() {
		if (title == null) {
			return "[ERROR]";
		}
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getHash() {
		return hash;
	}

	public void setHash(int hash) {
		this.hash = hash;
	}

	public int getEnablePlay() {
		return enablePlay;
	}

	public void setEnablePlay(int enablePlay) {
		this.enablePlay = enablePlay;
	}

	public Integer getPlID() {
		return plID;
	}

	public void setPlID(Integer plID) {
		this.plID = plID;
	}
//	protected void finalize() throws Throwable {
//		System.out.println("SONGDESCR Finalizing..");
//	}
}
