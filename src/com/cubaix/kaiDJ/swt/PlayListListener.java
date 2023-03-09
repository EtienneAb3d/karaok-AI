package com.cubaix.kaiDJ.swt;

import com.cubaix.kaiDJ.db.SongDescr;

public interface PlayListListener {
	public void removed(SongDescr aDescr);
	public void added(SongDescr aDescr);
}
