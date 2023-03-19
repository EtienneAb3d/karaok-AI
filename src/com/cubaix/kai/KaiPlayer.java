package com.cubaix.kai;

import org.eclipse.swt.widgets.Composite;

import com.cubaix.kaiDJ.KaiDJ;
import com.cubaix.kaiDJ.db.SongDescr;
import com.cubaix.kaiDJ.DJPlayer;

public class KaiPlayer extends DJPlayer {
	SongDescr song = null;
	String stem = null;
	
	public KaiPlayer(KaiDJ aParentKDJ, Composite parent, int style, int aDest) {
		super(aParentKDJ, parent, style, aDest,false);
	}
	
	@Override
	protected void extractProperties() {
		super.extractProperties();
		if(stem != null) {
			currentAuthor = stem;
		}
		if(song != null) {
			currentTitle = song.title+" / "+song.author;
		}
	}

	void load(SongDescr aSong,String aPath,String aStem) {
		song = aSong;
		stem = aStem;
		super.load(aPath);
	}

	@Override
	public void timeToNext() {
		//Do nothing
	}
	
	
}
