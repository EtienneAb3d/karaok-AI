package com.cubaix.kaiDJ.db;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

import org.tritonus.share.sampled.TAudioFormat;
import org.tritonus.share.sampled.file.TAudioFileFormat;

import com.cubaix.kaiDJ.KaiDJ;

public class Id3Analyzer implements BasicPlayerListener {
	KaiDJ parentJDJ;
	BasicPlayer player;
	SongDescr currentDescr = null;
	Db db;
	public boolean stopScan = false;

	public Id3Analyzer(KaiDJ aParentJDJ,final Db aDao) {
		super();
		parentJDJ = aParentJDJ;
		db = aDao;
		scan();
	}

	public synchronized void scan(){
		Thread aTh = new Thread(new Runnable(){
			public void run() {
					try{
						scan(db);
					}
					catch(Throwable t){
						t.printStackTrace(System.err);
					}
			}
		},"ID3 Analyzer");
		aTh.setPriority(Thread.MIN_PRIORITY);
		aTh.start();
	}
	/**
	 * @param aDao
	 */
	synchronized void scan(Db aDao) {
		Vector aV = null;
		aV = aDao.getToAnalyze();
		stopScan = false;
		//EM 02/11/2008 : faster with an array
		Object[] aVs = aV.toArray();
		for (int m = 0; !stopScan && m < aVs.length; m++) {
			SongDescr aDescr = (SongDescr) aVs[m];//aV.elementAt(m);
			analyze(aDao, aDescr);
			//Display and commit only evry N
			if((m % 50) == 0){
				parentJDJ.setMsg("Analyzing ID3 .. " + m + " / " + aV.size() + " .. " + aDescr.path);
				aDao.commit();
			}
			// Let a bit CPU for other threads
//			try {
//				Thread.sleep(100);
//			} catch (Throwable t) {
//			}
		}
		if (aV.size() > 0) {
			aDao.commit();
		}
		parentJDJ.setMsg("");
	}
	
	public void analyze(Db aDao,SongDescr aDescr){
		if (player == null) {
			// Instantiate BasicPlayer.
			//EM 29/04/2009 : parentJDJ
			player = new BasicPlayer(parentJDJ);
			player.addBasicPlayerListener(this);
		}
		try{
			currentDescr = aDescr;
			try{
				//player.open(new File(aDescr.path));
				File file = new File(aDescr.path);
			    AudioFileFormat m_audioFileFormat;
		        m_audioFileFormat = AudioSystem.getAudioFileFormat(file);
	            Map properties = null;
	            if (m_audioFileFormat instanceof TAudioFileFormat)
	            {
	                // Tritonus SPI compliant audio file format.
	                properties = ((TAudioFileFormat) m_audioFileFormat).properties();
	                // Clone the Map because it is not mutable.
	                HashMap map = new HashMap();
	                if (properties != null)
	                {
	                    Iterator it = properties.keySet().iterator();
	                    while (it.hasNext())
	                    {
	                        Object key = it.next();
	                        Object value = properties.get(key);
	                        map.put(key, value);
	                    }
	                }
	                properties = map;
	            }
	            else properties = new HashMap();
	            // Add JavaSound properties.
	            if (m_audioFileFormat.getByteLength() > 0) properties.put("audio.length.bytes", new Integer(m_audioFileFormat.getByteLength()));
	            if (m_audioFileFormat.getFrameLength() > 0) properties.put("audio.length.frames", new Integer(m_audioFileFormat.getFrameLength()));
	            if (m_audioFileFormat.getType() != null) properties.put("audio.type", (m_audioFileFormat.getType().toString()));
	            // Audio format.
	            AudioFormat audioFormat = m_audioFileFormat.getFormat();
	            if (audioFormat.getFrameRate() > 0) properties.put("audio.framerate.fps", new Float(audioFormat.getFrameRate()));
	            if (audioFormat.getFrameSize() > 0) properties.put("audio.framesize.bytes", new Integer(audioFormat.getFrameSize()));
	            if (audioFormat.getSampleRate() > 0) properties.put("audio.samplerate.hz", new Float(audioFormat.getSampleRate()));
	            if (audioFormat.getSampleSizeInBits() > 0) properties.put("audio.samplesize.bits", new Integer(audioFormat.getSampleSizeInBits()));
	            if (audioFormat.getChannels() > 0) properties.put("audio.channels", new Integer(audioFormat.getChannels()));
	            if (audioFormat instanceof TAudioFormat)
	            {
	                // Tritonus SPI compliant audio format.
	                Map addproperties = ((TAudioFormat) audioFormat).properties();
	                properties.putAll(addproperties);
	            }
	            // Add SourceDataLine
	            //properties.put("basicplayer.sourcedataline", m_line);
	            
	            opened(null,properties);
			}
			catch(Throwable t){
				//Invalide ??
				currentDescr.author = "[INVALIDE]";
				currentDescr.title = "[INVALIDE]";
				currentDescr.album = "[INVALIDE]";
				currentDescr.duration = -1;
			}
			aDao.update(currentDescr);
			currentDescr = null;
		}
		catch(Throwable t){
			t.printStackTrace(System.err);
		}
	}
	
	public void opened(Object stream, Map properties) {
		//Get all props
		//Iterator aIt = properties.entrySet().iterator();
		/*
		while(aIt.hasNext()){
			Object aKey = aIt.next(); 
			System.out.println("Prop : " + aKey);
		}
		*/
//		System.out.println("MP3 ID=" + currentDescr.id);
//		System.out.println("GET : author = " + properties.get("author"));
//		System.out.println("GET : title = " + properties.get("title"));
//		System.out.println("GET : album = " + properties.get("album"));
//		System.out.println("GET : duration = " + properties.get("duration"));
		currentDescr.set((String)properties.get("author"),(String)properties.get("title"),(String)properties.get("album"),((Long)properties.get("duration")).longValue());
	}

	public void progress(int bytesread, long microseconds, byte[] pcmdata,
			Map properties) {
		// TODO Auto-generated method stub

	}

	public void stateUpdated(BasicPlayerEvent event) {
		// TODO Auto-generated method stub

	}

	public void setController(BasicController controller) {
		// TODO Auto-generated method stub

	}

}
