package com.cubaix.kaiDJ.db;


public class SongGroup extends SongDescr {
	public static final int _KIND_OPEN = 0;
	public static final int _KIND_CLOSE = 1;
	
	public int kind = _KIND_OPEN;
	
	public SongGroup(int aKing,String aLabel) {
		super(new Integer(-1), aLabel);
		kind = aKing;
	}

//	@Override
//	protected void finalize() throws Throwable {
//		System.out.println("SONGGROUP Finalizing..");
//	}
}
