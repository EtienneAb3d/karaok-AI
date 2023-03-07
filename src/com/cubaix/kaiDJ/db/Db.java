package com.cubaix.kaiDJ.db;

import java.io.File;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;

import com.cubaix.kaiDJ.KaiDJ;

public class Db {
	KaiDJ parentJDJ;
	static public String mp3DbPath = KaiDJ.kaiDir+"/MP3_v3.hdb";
	String embeddedUrl = "jdbc:hsqldb:file";
	String databaseURL = embeddedUrl + ":" + mp3DbPath;
	String user = "sa";
	String password = "";
	java.sql.Driver d = null;
	java.sql.Connection c = null;
	//Songs
	PreparedStatement psInsert;
	int nbInserted = 0;
	PreparedStatement psGetPath;
	PreparedStatement psGetAll;
	//EM 06/04/2008 : no more used
	//PreparedStatement psSearch;
	PreparedStatement psSearchPath;
	public boolean breakSearch = false;
	PreparedStatement psDelAll;
	//EM 13/09/2008
	PreparedStatement psDelId;
	PreparedStatement psGetToAnalyze;
	PreparedStatement psUpdate;
	//Categories
	PreparedStatement psInsertCat;
	PreparedStatement psGetAllCat;
	PreparedStatement psGetCat;
	PreparedStatement psDelInCat;
	PreparedStatement psCleanCat;
	//EM 13/09/2008
	PreparedStatement psDoubles;

	/**
	 * @param aParentJDJ
	 */
	public Db(KaiDJ aParentJDJ) {
		parentJDJ = aParentJDJ;
	}

	/**
	 * 
	 */
	synchronized public void initDb() {
		//File aFile = new File(mp3DbPath);
		try {
			Class.forName("org.hsqldb.jdbcDriver");
			c = DriverManager.getConnection(databaseURL, user, password);
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			parentJDJ.display.syncExec(new Runnable() {
				public void run() {
					MessageBox aMsg = new MessageBox(parentJDJ.shell, SWT.OK);
					aMsg.setText("Error");
					aMsg.setMessage("Can't open Db, certainly an other JDJ is running...");
					aMsg.open();
				}
			});
			System.exit(0);
		}
		// Now that we have a connection, let's try to get some meta data...
		int aNbTables = countTables(c);
		java.sql.Statement s = null;
		if (aNbTables > 0) {
			// Tables exists, just use them
			initStatements();
		} else {
			// No table, need a fresh init
			try {
				s = c.createStatement();
				s.executeUpdate("create cached table mp3 " + "(" + //
						"id IDENTITY, " + //
						"path char(1024) not null," + // 
						"author char(128) not null," + // 
						"title char(128) not null," + // 
						"album char(128) not null," + // 
						"duration char(15) not null, " + //
						"hash char(15) not null " + //
						")");
				s.executeUpdate("create cached table cat " + "(" + //
						"id IDENTITY, " + //
						"mp3_id int, " + //
						"name char(128) not null," + // 
						"hash int not null " + //
						")");
				c.commit();
				aNbTables = countTables(c);
			} catch (Throwable t) {
				t.printStackTrace(System.err);
			}
			initStatements();
			nbInserted = 0;
//			// Check for an import of old db
//			importV_old();
		}
		try {
			if (s != null)
				s.close();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	/**
	 * 
	 */
//	synchronized void importV_old() {
//		File aFile;
//		aFile = new File(Db_v2.mp3DbPath + ".data");
//		if (aFile.exists()) {
//			// Need to import
//			Db_v2 aDb = new Db_v2(parentJDJ);
//			aDb.initDb();
//			Vector aAll = new Vector();
//			aDb.readAll(aAll);
//			for (int i = 0; i < aAll.size(); i++) {
//				insert((SongDescr) aAll.elementAt(i));
//				if ((i % 100) == 0) {
//					aDb.commit();
//					this.parentJDJ.setMsg("Converting old db to new format... " + ((100 * i) / aAll.size()) + " %");
//				}
//			}
//			aDb.commit();
//			aDb.close();
//			this.parentJDJ.setMsg("");
//			// Done !
//			return;
//		}
////		aFile = new File(Db_v1.mp3DbPath);
////		if (aFile.exists()) {
////			// Need to import
////			Db_v1 aDb = new Db_v1(parentJDJ);
////			aDb.initDb();
////			Vector aAll = new Vector();
////			aDb.readAll(aAll);
////			for (int i = 0; i < aAll.size(); i++) {
////				insert((SongDescr) aAll.elementAt(i));
////				if ((i % 100) == 0) {
////					aDb.commit();
////					this.parentJDJ.setMsg("Converting old db to new format... " + ((100 * i) / aAll.size()) + " %");
////				}
////			}
////			aDb.commit();
////			aDb.close();
////			this.parentJDJ.setMsg("");
////			// Done !
////			return;
////		}
////		aFile = new File(Db_v0.mp3DbPath);
////		if (aFile.exists()) {
////			// Need to import
////			Db_v0 aDb = new Db_v0();
////			aDb.initDb();
////			Vector aAll = new Vector();
////			aDb.readAll(aAll);
////			for (int i = 0; i < aAll.size(); i++) {
////				insert(((SongDescr) aAll.elementAt(i)).path);
////				if ((i % 100) == 0) {
////					aDb.commit();
////					this.parentJDJ.setMsg("Converting old db to new format... " + ((100 * i) / aAll.size()) + " %");
////				}
////			}
////			aDb.commit();
////			aDb.close();
////			this.parentJDJ.setMsg("");
////		}
//	}

	/**
	 * 
	 */
	synchronized void initStatements() {
		try {
			//Main song table
			psInsert = c.prepareStatement("insert into mp3 values(" + //
					"NULL," + // "gen_id(gen_mp3_id,1), " + //
					"?," + // path
					"?," + // author
					"?," + // title
					"?," + // album
					"?," + // duration
					"?" + // hash
					")");
			psGetPath = c.prepareStatement("SELECT path FROM mp3 WHERE id=?");
			psGetAll = c.prepareStatement("SELECT id,path,author,title,album,duration,hash FROM mp3");
			//EM 06/04/2008 : no more used
			//psSearch = c.prepareStatement("SELECT id,path,author,title,album,duration,hash FROM mp3 WHERE " + "    LOWER(path) LIKE ?" + " OR LOWER(author) LIKE ?" + " OR LOWER(title) LIKE ?" + " OR LOWER(album) LIKE ?");
			psSearchPath = c.prepareStatement("SELECT id,path,author,title,album,duration,hash FROM mp3 WHERE " + "    LOWER(path) = ?");
			psDelAll = c.prepareStatement("DELETE FROM mp3");
			//EM 13/09/2008
			psDelId = c.prepareStatement("DELETE FROM mp3 WHERE id = ?");
			psGetToAnalyze = c.prepareStatement("SELECT id, path FROM mp3 WHERE " + // 
					"author = '[UNKNOWN]' AND " + //
					"title = '[UNKNOWN]' AND " + //
					"album = '[UNKNOWN]' AND " + //
					"duration = '[UNKNOWN]'" //
			);
			psUpdate = c.prepareStatement("UPDATE mp3 SET " + // 
					"author=?," + //
					"title=?," + //
					"album=?," + //
					"duration=?," + //
					"hash=?" + //
					"WHERE id=?"//
			);
			//EM 13/09/2008
			psDoubles = c.prepareStatement("SELECT DISTINCT m1.id, m2.id FROM mp3 m1, mp3 m2 WHERE m1.id < m2.id AND m1.hash = m2.hash");

			//Categories table
			psInsertCat = c.prepareStatement("insert into cat values(" + //
					"NULL," + // id
					"?," + // mp3_id
					"?," + // name
					"?" + // hash
					")");
			psGetAllCat = c.prepareStatement("SELECT DISTINCT name FROM cat");
			psGetCat = c.prepareStatement("SELECT m.id,m.path,m.author,m.title,m.album,m.duration,m.hash FROM mp3 m " //
					+ "WHERE m.hash IN (SELECT c.hash FROM cat c WHERE c.name = ?)");
			psDelInCat = c.prepareStatement("DELETE from cat WHERE name = ? AND hash = ?");
			psCleanCat = c.prepareStatement("DELETE from cat WHERE hash NOT IN (SELECT hash FROM mp3)");
		} catch (Throwable t) {
			System.err.println("Can't prepare statements : " + t);
			t.printStackTrace(System.err);
		}
	}

	/**
	 * 
	 */
	synchronized public void commit() {
		try {
			c.commit();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	/**
	 * 
	 */
	synchronized public void delAll() {
		try {
			psDelAll.executeUpdate();
			c.commit();
		} catch (java.sql.SQLException e) {
			System.out.println("Can't delete db data.");
			showSQLException(e);
		}
	}

	/**
	 * @param aDescr
	 * @param aCat
	 */
	synchronized public void delInCat(SongDescr aDescr,String aCat) {
		try {
			psDelInCat.setObject(1, aCat);// name
			psDelInCat.setObject(2, "" + aDescr.getHash());// hash
			psDelInCat.executeUpdate();
			c.commit();
		} catch (java.sql.SQLException e) {
			System.out.println("Can't delete db data.");
			showSQLException(e);
		}
	}

	SongDescr newFromRS(java.sql.ResultSet rs) {
		Integer aId = new Integer(-1);
		String aPath = "ERROR";
		String aAuthor = "ERROR";
		String aTitle = "ERROR";
		String aAlbum = "ERROR";
		long aDuration = -1;
		int aHash = -1;
		try {
			aId = new Integer(rs.getInt(1));
			aPath = rs.getString(2).trim();// Path
			aAuthor = rs.getString(3).trim();// Author
			aTitle = rs.getString(4).trim();// Title
			aAlbum = rs.getString(5).trim();// Album
			try {
				aDuration = Long.parseLong(rs.getString(6).trim());// Duration
			} catch (Throwable ignore) {
				aDuration = -1;
			}
			try {
				aHash = Integer.parseInt(rs.getString(7).trim());// Hash
			} catch (Throwable ignore) {
				aHash = -1;
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
		return new SongDescr(aId, aPath, aAuthor, aTitle, aAlbum, aDuration,aHash);
	}

	/**
	 * Search selections are done with a low priority Java thread to avoid play troubles
	 * 
	 * @param aSearch
	 */
	synchronized public void search(Vector aRes, String aSearch) {
		if (aSearch == null || aSearch.length() <= 0) {
			getAll(aRes);
			return;
		}
		// boolean aFullCase = (aSearch.toLowerCase() != aSearch);
		java.sql.ResultSet rs = null;
		try {
			rs = psGetAll.executeQuery();// s.executeQuery("select id, path
			// from mp3");
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return;
		}
		try {
			String aSearchL = aSearch.toLowerCase();
			breakSearch = false;
			int aCount = 0;
			while (rs.next() && !breakSearch) {
				SongDescr aDescr = newFromRS(rs);
				if (aDescr.path.toLowerCase().indexOf(aSearchL) >= 0 || aDescr.album.toLowerCase().indexOf(aSearchL) >= 0 || aDescr.author.toLowerCase().indexOf(aSearchL) >= 0 || aDescr.title.toLowerCase().indexOf(aSearchL) >= 0) {
					aRes.add(aDescr);
					aCount++;
					if ((aCount % 100) == 0) {
						parentJDJ.managerSearch.pl.listTC.needRedraw();
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return;
		}
		try {
			if (rs != null)
				rs.close();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return;
		}
		return;
	}

	//EM 13/09/2008
	synchronized public void cleanDoubles() {
		java.sql.ResultSet rs = null;
		try {
			rs = psGetAll.executeQuery();// s.executeQuery("select id, path
			// from mp3");
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return;
		}
		try {
			TreeMap aPaths = new TreeMap();
			breakSearch = false;
			int aCount = 0;
			while (rs.next() && !breakSearch) {
				SongDescr aDescr = newFromRS(rs);
				Integer aId = (Integer)aPaths.get(aDescr.path); 
				if(aId != null){
					System.out.println("Double : " + aDescr.path);
					//Delete older to keep the more recent
					try{
						psDelId.setObject(1, aId);
						psDelId.executeUpdate();
					} catch (Throwable t) {
						t.printStackTrace(System.err);
					}
				}
				aPaths.put(aDescr.path,aDescr.id);
			}
			c.commit();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return;
		}
		try {
			if (rs != null)
				rs.close();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return;
		}
		return;
}

	/**
	 * @param aSearch
	 * @return
	 */
	synchronized public SongDescr searchPath(String aSearch) {
		if (aSearch == null || aSearch.length() <= 0) {
			return null;// new SongDescr(new Integer(-1),aSearch);
		}
		// boolean aFullCase = (aSearch.toLowerCase() != aSearch);
		java.sql.ResultSet rs = null;
		try {
			psSearchPath.setObject(1, aSearch.toLowerCase());// Path
			rs = psSearchPath.executeQuery();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}
		//EM 06/04/2008 : store song
		SongDescr aSongDescr = null;
		try {
			//String aSearchL = aSearch.toLowerCase();
			breakSearch = false;
			//int aCount = 0;
			if (rs.next()) {
				//EM 06/04/2008 : store song
				aSongDescr = newFromRS(rs);
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
		try {
			if (rs != null)
				rs.close();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
		//EM 06/04/2008 : is the path not found ? Try partial path
		if(aSongDescr == null){
			return searchPathPartial(aSearch);
		}
		
		//EM 06/04/2008 : return song
		return aSongDescr;
	}
	
	//EM 06/04/2008
	/**
	 * @param aSearch
	 * @return
	 */
	synchronized public SongDescr searchPathPartial(String aSearch) {
		if (aSearch == null || aSearch.length() <= 0) {
			return null;
		}
		System.out.println("Search relocation for : " + aSearch);
		// boolean aFullCase = (aSearch.toLowerCase() != aSearch);
		java.sql.ResultSet rs = null;
		try {
			rs = psGetAll.executeQuery();// s.executeQuery("select id, path
			// from mp3");
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return null;
		}
		SongDescr aSongDescr = null;
		try {
			String aSearchL = aSearch.toLowerCase();
			breakSearch = false;
			String aSR = aSearch.replaceAll("\\\\","/").toLowerCase();
			int aPos = aSR.lastIndexOf("/");
			if(aPos < 0){
				//??can't find ??
				return null;
			}
			String aName = aSR.substring(aPos);
			int aBestCount = 0;
			while (rs.next() && !breakSearch) {
				SongDescr aDescr = newFromRS(rs);
				String aSRdb = aDescr.path.replaceAll("\\\\","/").toLowerCase();
				aPos = aSRdb.lastIndexOf("/");
				if(aPos < 0){
					//??can't finf ??
					aPos = 0;
				}
				
				String aNamedb = aSRdb.substring(aPos);
				if(aName.equalsIgnoreCase(aNamedb)){
					//Ok perhaps a match. Count matches
					int aPosSR = aSR.length() - 1;
					int aPosSRdb = aSRdb.length() - 1;
					int aCount = 0;
					while(aPosSR >=0 && aPosSRdb >= 0){
						if(aSR.charAt(aPosSR) != aSRdb.charAt(aPosSRdb)){
							break;
						}
						aCount++;
						aPosSR--;
						aPosSRdb--;
					}
					if(aCount > aBestCount){
						System.out.println("Relocation found : " + aDescr.path);
						aBestCount = aCount;
						aSongDescr = aDescr;
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
		try {
			if (rs != null)
				rs.close();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
		return aSongDescr;
	}

	/**
	 * 
	 */
	synchronized public void getAll(Vector aRes) {
		java.sql.ResultSet rs = null;
		//int aNb = 0;
		try {
			rs = psGetAll.executeQuery();// s.executeQuery("select id, path
			// from mp3");
		} catch (java.sql.SQLException e) {
			System.out.println("Can't get mp3 data.");
			showSQLException(e);
			return;
		}
		try {
			breakSearch = false;
			int aCount = 0;
			while (rs.next() && !breakSearch) {
				aRes.add(newFromRS(rs));
				aCount++;
				if ((aCount % 100) == 0) {
					parentJDJ.managerSearch.pl.listTC.needRedraw();
				}
			}
		} catch (java.sql.SQLException e) {
			System.out.println("Unable to step thru results of query");
			showSQLException(e);
			return;
		}
		// System.out.println("");
		try {
			if (rs != null)
				rs.close();
		} catch (java.sql.SQLException e) {
			showSQLException(e);
		}
	}
	
	//EM 13/09/2008
	synchronized public TreeMap getAllByPaths() {
		TreeMap aRes = new TreeMap();
		java.sql.ResultSet rs = null;
		//int aNb = 0;
		try {
			rs = psGetAll.executeQuery();// s.executeQuery("select id, path
			// from mp3");
		} catch (java.sql.SQLException e) {
			System.out.println("Can't get mp3 data.");
			showSQLException(e);
			return aRes;
		}
		try {
			breakSearch = false;
			int aCount = 0;
			while (rs.next() && !breakSearch) {
				SongDescr aDescr = newFromRS(rs);
				aRes.put(aDescr.path,aDescr);
				aCount++;
				if ((aCount % 100) == 0) {
					parentJDJ.managerSearch.pl.listTC.needRedraw();
				}
			}
		} catch (java.sql.SQLException e) {
			System.out.println("Unable to step thru results of query");
			showSQLException(e);
			return aRes;
		}
		// System.out.println("");
		try {
			if (rs != null)
				rs.close();
		} catch (java.sql.SQLException e) {
			showSQLException(e);
		}
		return aRes;
	}
	
	/**
	 * @return
	 */
	synchronized public Vector getAllCat() {
		Vector aRes = new Vector();
		java.sql.ResultSet rs = null;
		//int aNb = 0;
		try {
			rs = psGetAllCat.executeQuery();// s.executeQuery("select id, path
			// from mp3");
		} catch (java.sql.SQLException e) {
			System.out.println("Can't get mp3 data.");
			showSQLException(e);
			return aRes;
		}
		try {
			while (rs.next()) {
				aRes.add(rs.getString(1).trim());
			}
		} catch (java.sql.SQLException e) {
			System.out.println("Unable to step thru results of query");
			showSQLException(e);
			return aRes;
		}
		// System.out.println("");
		try {
			if (rs != null)
				rs.close();
		} catch (java.sql.SQLException e) {
			showSQLException(e);
		}
		return aRes;
	}

	/**
	 * @param aRes
	 */
	synchronized public void getCat(Vector aRes,String aCat) {
		java.sql.ResultSet rs = null;
		//int aNb = 0;
		try {
			psGetCat.setObject(1, aCat);// name
			rs = psGetCat.executeQuery();
		} catch (java.sql.SQLException e) {
			System.out.println("Can't get mp3 data.");
			showSQLException(e);
			return;
		}
		try {
			while (rs.next()) {
				aRes.add(newFromRS(rs));
			}
		} catch (java.sql.SQLException e) {
			System.out.println("Unable to step thru results of query");
			showSQLException(e);
			return;
		}
		// System.out.println("");
		try {
			if (rs != null)
				rs.close();
		} catch (java.sql.SQLException e) {
			showSQLException(e);
		}
	}
	
	/**
	 * @param aDescr
	 */
	synchronized void insert(SongDescr aDescr) {
		try {
			psInsert.setObject(1, aDescr.getPath());// Path
			psInsert.setObject(2, aDescr.getAuthor());// Author
			psInsert.setObject(3, aDescr.getTitle());// Title
			psInsert.setObject(4, aDescr.getAlbum());// Album
			psInsert.setObject(5, "" + aDescr.getDuration());// Duration
			psInsert.setObject(6, "" + aDescr.getHash());// Hash
			psInsert.executeUpdate();
		} catch (Throwable t) {
			System.err.println("Can't insert : " + t);
			t.printStackTrace(System.err);
		}
	}

	/**
	 * @param aPath
	 */
	synchronized void insert(String aPath) {
		try {
			psInsert.setObject(1, aPath);// Path
			psInsert.setObject(2, "[UNKNOWN]");// Author
			psInsert.setObject(3, "[UNKNOWN]");// Title
			psInsert.setObject(4, "[UNKNOWN]");// Album
			psInsert.setObject(5, "[UNKNOWN]");// Duration
			psInsert.setObject(6, "0");// Hash
			psInsert.executeUpdate();
		} catch (Throwable t) {
			System.err.println("Can't insert : " + t);
			t.printStackTrace(System.err);
		}
	}
	
	/**
	 * @param aDescr
	 * @param aCat
	 */
	public synchronized void insertCat(SongDescr aDescr,String aCat) {
		try {
			psInsertCat.setObject(1, aDescr.getId());// mp3_id
			psInsertCat.setObject(2, aCat);// cat
			psInsertCat.setObject(3, "" + aDescr.getHash());// Hash
			psInsertCat.executeUpdate();
			psCleanCat.executeUpdate();
		} catch (Throwable t) {
			System.err.println("Can't insert : " + t);
			t.printStackTrace(System.err);
		}
	}


	/**
	 * @param aDescr
	 */
	public synchronized void update(SongDescr aDescr) {
		try {
			psUpdate.setObject(1, aDescr.getAuthor());// Author
			psUpdate.setObject(2, aDescr.getTitle());// Title
			psUpdate.setObject(3, aDescr.getAlbum());// Album
			psUpdate.setObject(4, "" + aDescr.getDuration());// Duration
			psUpdate.setObject(5, "" + aDescr.getHash());// Hash
			psUpdate.setObject(6, aDescr.getId());// Id
			psUpdate.executeUpdate();
			parentJDJ.updatedDescr(aDescr);
		} catch (Throwable t) {
			System.err.println("Can't update : " + t);
			t.printStackTrace(System.err);
		}
	}

	/**
	 * @param aPath
	 */
	synchronized public void scanDir(String aPath) {
		if (aPath == null) {
			return;
		}
		// System.out.println(nbInserted + " Scanning : " + aPath);
		File aDir = new File(aPath);
		if (!aDir.exists()) {
			// Error in provided path
			return;
		}
		if (!aDir.isDirectory() && aPath.toLowerCase().endsWith(".mp3")) {
			nbInserted++;
			insert(aPath);
			return;
		}
		//EM 02/11/2008 : display info only for dir, not for files
		this.parentJDJ.setMsg("Scanning dir... " + aPath);
		
		File[] aSub = aDir.listFiles();
		for (int i = 0; aSub != null && i < aSub.length; i++) {
			scanDir(aSub[i].getAbsolutePath());
		}
		// Commit after each dir
		try {
			c.commit();
		} catch (Throwable t) {
			System.err.println("Can't commit : " + t);
			t.printStackTrace(System.err);
		}
	}

	/**
	 * @return
	 */
	public synchronized Vector getToAnalyze() {
		Vector aRes = new Vector();
		/*
		 * java.sql.Statement s = null; try { s = c.createStatement(); } catch (java.sql.SQLException e) { showSQLException(e); }
		 */
		java.sql.ResultSet rs = null;
		//int aNb = 0;
		try {
			rs = psGetToAnalyze.executeQuery();// s.executeQuery("select id,
			// path from mp3");
		} catch (java.sql.SQLException e) {
			System.out.println("Can't get mp3 data.");
			showSQLException(e);
			return aRes;
		}
		try {
			while (rs != null && rs.next()) {
				// System.out.println(rs.getInt(1));
				aRes.add(new SongDescr(new Integer(rs.getInt(1)), rs.getString(2).trim()));
				/*
				 * if((aNb++ % 100) == 0){ System.out.print("."); }
				 */
			}
		} catch (java.sql.SQLException e) {
			System.out.println("Unable to step thru results of query");
			showSQLException(e);
			return aRes;
		}
		// System.out.println("");
		try {
			if (rs != null)
				rs.close();
		} catch (java.sql.SQLException e) {
			showSQLException(e);
		}
		return aRes;
	}

	/**
	 * @param aId
	 * @return
	 */
	synchronized public String getPath(Integer aId) {
		java.sql.ResultSet rs = null;
		try {
			psGetPath.setObject(1, aId);
			rs = psGetPath.executeQuery();
		} catch (java.sql.SQLException e) {
			System.out.println("Can't get mp3 data.");
			showSQLException(e);
			return "";
		}
		try {
			while (rs.next()) {
				// System.out.println(rs.getInt(1));
				return rs.getString(1);
			}
		} catch (java.sql.SQLException e) {
			System.out.println("Unable to step thru results of query");
			showSQLException(e);
			return "";
		}
		try {
			if (rs != null)
				rs.close();
		} catch (java.sql.SQLException e) {
			showSQLException(e);
		}
		return "";
	}

	/**
	 * 
	 */
	synchronized public void close() {
		try {
			if (c != null) {
				// c.rollback();
				c.commit();
			}
		} catch (java.sql.SQLException e) {
			showSQLException(e);
		}
		try {
			if (c != null)
				c.close();
		} catch (java.sql.SQLException e) {
			showSQLException(e);
		}
	}

	// Display an SQLException which has occured in this application.
	/**
	 * @param e
	 */
	private static void showSQLException(java.sql.SQLException e) {
		// Notice that a SQLException is actually a chain of SQLExceptions,
		// let's not forget to print all of them...
		java.sql.SQLException next = e;
		while (next != null) {
			System.out.println(next.getMessage());
			System.out.println("Error Code: " + next.getErrorCode());
			System.out.println("SQL State: " + next.getSQLState());
			next = next.getNextException();
		}
	}

	/**
	 * @param c
	 * @return
	 */
	synchronized int countTables(java.sql.Connection c) {
		int aNbTables = 0;
		try {
			java.sql.DatabaseMetaData dbMetaData = c.getMetaData();
			// Ok, let's query a driver/database capability
			if (dbMetaData.supportsTransactions()) {
				System.out.println("Transactions are supported.");
			} else {
				System.out.println("Transactions are not supported.");
			}
			// What are the views defined on this database?
			java.sql.ResultSet tables = dbMetaData.getTables(null, null, "%", new String[] { "TABLE" });
			while (tables.next()) {
				aNbTables++;
				System.out.println(tables.getString("TABLE_NAME") + " table found.");
			}
			tables.close();
		} catch (java.sql.SQLException e) {
			System.out.println("Unable to extract database meta data.");
			showSQLException(e);
			// What the heck, who needs meta data anyway ;-(, let's continue
			// on...
		}
		return aNbTables;
	}
}
