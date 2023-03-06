package com.cubaix.kaiDJ;

import com.cubaix.kaiDJ.db.SongDescr;

public interface PlayListListener {
	public void removed(SongDescr aDescr);
	public void added(SongDescr aDescr);
}
