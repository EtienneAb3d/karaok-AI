package com.cubaix.kaiDJ;

/*
 * BasicPlayerTest.
 * 
 * JavaZOOM : jlgui@javazoom.net http://www.javazoom.net
 * 
 * ----------------------------------------------------------------------- This program is free software; you can redistribute it and/or modify it under the terms of the GNU Library General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public License along with this program; if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. ----------------------------------------------------------------------
 */
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerListener;
import kj.dsp.KJDigitalSignalProcessor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import com.cubaix.kaiDJ.swt.TimedCanvas;

/**
 * This class implements a simple player based on BasicPlayer. BasicPlayer is a threaded class providing most features of a music player. BasicPlayer works with underlying JavaSound SPIs to support multiple audio formats. Basically JavaSound supports WAV, AU, AIFF audio formats. Add MP3 SPI (from JavaZOOM) and Vorbis SPI( from JavaZOOM) in your CLASSPATH to play MP3 and Ogg Vorbis file.
 */
public class Player extends TimedCanvas implements BasicPlayerListener, KJDigitalSignalProcessor{
	
	//EM 06/07/2008 
	static final int _WIDTHINT = 150;
	static final int _HEIGHTINT = 100;
	
	private PrintStream out = null;

	//EM 13/09/2008 : turn static
	private static NumberFormat ddNF;
	static {
		ddNF = NumberFormat.getInstance();
		ddNF.setMaximumIntegerDigits(2);
		ddNF.setMinimumIntegerDigits(2);
	}

	Rectangle playerBounds = null;

	BasicPlayer player;

	BasicController control;

	int destCard = 0;

	int playState = -1;// -1 = nothing, 0 = pause, 1 = playing, 2 = fadeout, 3 = fadein

	Map openProperties;

	Map currentProperties;

	double currentGain = 0.8;

	String currentAuthor = "";

	String currentTitle = "";

	long currentDuration = 0;

	long currentPositionMs = 0;

	long currentLengthBytes = 0;

	long fadeDuration = 10000;// 10s in ms

	Image playerBck = null, dblBuf = null;

	GC dblBufGC;

	Rectangle progessBarR = new Rectangle(0, 0, 0, 0);

	Rectangle volumeBarR = new Rectangle(0, 0, 0, 0);

	//EM 11/11/2008
	float vuMeterLeft  = 0.0f;
	float vuMeterRight = 0.0f;
	float vuMeterLeftB  = 0.0f;
	float vuMeterRightB = 0.0f;

	
	/**
	 * @param aParent
	 * @param parent
	 * @param style
	 */
	public Player(KaiDJ aParentJDJ, Composite parent, int style, int aDest) {
		super(aParentJDJ, parent, style | SWT.NO_BACKGROUND);
		//EM 12/11/2008 : refresh more often for the vumeter
		refreshRate = 50;
		
		out = System.out;
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent aPE) {
				playerBounds = getClientArea();
				// playerGC = aPE.gc;
				paintDbl(aPE.gc);
				needRedraw();
			}
		});
		// playerGC = new GC(this);
		destCard = aDest;
		setToolTip("Player:\n"//
				+ "- Click on player to start, pause or resume\n"//
				+ "- Click on bottom bar to change playing position\n"//
				+ "- Click on right bar to change volume");
	}

	/**
	 * @param x
	 * @param y
	 */
	public boolean click(int x, int y) {
		try {
			if (openProperties != null && progessBarR.width > 0 && x >= progessBarR.x && x < progessBarR.x + progessBarR.width && y >= progessBarR.y && y < progessBarR.y + progessBarR.height) {
				double aPos = (double) (x - progessBarR.x) / (double) progessBarR.width;
				long aSeek = (long) (aPos * currentLengthBytes);
				player.seek(aSeek);
				currentPositionMs = (long) (currentDuration * aPos);
				needRedraw();
				return true;
			} else if (openProperties != null && volumeBarR.width > 0 && x >= volumeBarR.x && x < volumeBarR.x + volumeBarR.width && y <= volumeBarR.y && y > volumeBarR.y + volumeBarR.height) {
				double aPos = (double) (y - volumeBarR.y) / (double) volumeBarR.height;
				setGain(aPos);
				needRedraw();
				return true;
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
		return false;
	}

	/**
	 * @param aDestCard
	 */
	public void setDestCard(int aDestCard) {
		destCard = aDestCard;
		if (player != null) {
			//EM 24/08/2008 : use direct mixer index
			//List aMixers = player.getMixers();
			//if (destCard < aMixers.size()) {
			    //EM 24/08/2008 : don't use name that can be duplicated, use index
				((BasicPlayer) player).setMixerIndex(destCard);//.setMixerName((String) aMixers.get(destCard));
			//}
		}
	}

	/**
	 * @param filename
	 */
	public void load(String filename) {
		try {
			if (player == null) {
				// Instantiate BasicPlayer.
				//EM 29/04/2009 : provide with parentJDJ
				player = new BasicPlayer(parentKDJ);
				//EM 01/11/2008 : try to get the same buffer size on each 
				player.setLineBufferSize(65535);
				
				setDestCard(destCard);
				// BasicPlayer is a BasicController.
				control = (BasicController) player;
				// Register BasicPlayerTest to BasicPlayerListener events.
				// It means that this object will be notified on BasicPlayer
				// events such as : opened(...), progress(...),
				// stateUpdated(...)
				player.addBasicPlayerListener(this);
				
				// Gain for first play
				// currentGain = 0.85;
			} else {
				control.stop();
			}
			// Reinit
			openProperties = null;
			currentProperties = null;
			currentAuthor = "";
			currentTitle = "";
			currentDuration = 0;
			currentPositionMs = 0;
			currentLengthBytes = 0;
			// Open file, or URL or Stream (shoutcast) to play.
			control.open(new File(filename));
			// control.open(new URL("http://yourshoutcastserver.com:8000"));
			// Start playback in a thread.
			// Playing
			playState = -1;
			// Set Volume (0 to 1.0).
			// setGain should be called after control.play().
			// control.setGain(currentGain);
			// Set Pan (-1.0 to 1.0).
			// setPan should be called after control.play().
			// control.setPan(0.0);
			// If you want to pause/resume/pause the played file then
			// write a Swing player and just call control.pause(),
			// control.resume() or control.stop().
			// Use control.seek(bytesToSkip) to seek file
			// (i.e. fast forward and rewind). seek feature will
			// work only if underlying JavaSound SPI implements
			// skip(...). True for MP3SPI (JavaZOOM) and SUN SPI's
			// (WAVE, AU, AIFF).
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	/**
	 * 
	 */
	public void play() {
		try {
			playState = 1;
			control.play();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	/**
	 * @param aGain
	 */
	public void setGain(double aGain) {
		currentGain = aGain;
		try {
			control.setGain(currentGain);
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	/**
	 * 
	 */
	public void fadeOut() {
		try {
			if (control != null) {
				playState = 2;
				control.resume();
				control.setGain(currentGain);
				Thread aThread = new Thread("Fade out") {
					public void run() {
						long aFadeStep = fadeDuration / 50;
						while (playState == 2) {
							try {
								double aFade = 0.8 * (double) (aFadeStep) / (double) fadeDuration;
								currentGain -= aFade;
								// System.out.println("Gain fade out : " + currentGain);
								if (currentGain <= 0) {
									control.setGain(currentGain = 0);
									playState = 0;
									control.pause();
								}
								control.setGain(currentGain);
								needRedraw();
								Thread.sleep(aFadeStep);
							} catch (Throwable t) {
								t.printStackTrace(System.err);
							}
						}
					}
				};
				aThread.start();
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	/**
	 * 
	 */
	public void fadeIn() {
		try {
			if (control != null) {
				playState = 3;
				control.resume();
				control.setGain(currentGain);
				Thread aThread = new Thread("Fade in") {
					public void run() {
						long aFadeStep = fadeDuration / 50;
						while (playState == 3) {
							try {
								double aFade = 0.8 * (double) (aFadeStep) / (double) fadeDuration;
								currentGain += aFade;
								if (currentGain >= 0.8) {
									control.setGain(currentGain = 0.8);
									playState = 1;
								}
								control.setGain(currentGain);
								needRedraw();
								Thread.sleep(aFadeStep);
							} catch (Throwable t) {
								t.printStackTrace(System.err);
							}
						}
					}
				};
				aThread.start();
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	/**
	 * 
	 */
	public void togglePlay() {
		if (control == null) {
			return;
		}
		try {
			if (playState < 0) {
				control.play();
				playState = 1;
			} else if (playState == 1) {
				control.pause();
				playState = 0;
			} else {
				control.resume();
				playState = 1;
				control.setGain(currentGain);
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	/**
	 * Open callback, stream is ready to play.
	 * 
	 * properties map includes audio format dependant features such as bitrate, duration, frequency, channels, number of frames, vbr flag, id3v2/id3v1 (for MP3 only), comments (for Ogg Vorbis), ...
	 * 
	 * @param stream
	 *            could be File, URL or InputStream
	 * @param properties
	 *            audio stream properties.
	 */
	public void opened(Object stream, Map properties) {
		// Pay attention to properties. It's useful to get duration,
		// bitrate, channels, even tag such as ID3v2.
		display("opened : " + properties.toString());
		openProperties = properties;
		extractProperties();
		needRedraw();
	}
	
	

	//EM 11/11/2008 : DSP
	@Override
	public void process(float[] leftChannel, float[] rightChannel, float frameRateRatioHint) {
		if(!parentKDJ.showSoundProfile || player.getStatus() != BasicPlayer.PLAYING){
			vuMeterLeftB = 0;
			vuMeterRightB = 0;

			vuMeterLeft = 0;
			vuMeterRight = 0;

			return;
		}
		
		//EM 29/04/2009 : BAD calculation !! Should use the FFT, like in KJFFT.calculate()
		//	see KJScopeAndSpectrumAnalyser.drawSpectrumAnalyser()
		
		int aBand = leftChannel.length / 4;
		
		float wLeftLow  = 0.0f;
		float wRightLow = 0.0f;
		for( int a = 0; a < aBand; a++ ) {
			wLeftLow  = Math.max(wLeftLow, leftChannel[ a ] );
			wRightLow = Math.max(wRightLow, rightChannel[ a ] );

		}
		
		wLeftLow  *= 2.0;
		wRightLow *= 2.0;

		float wLeftMid  = 0.0f;
		float wRightMid = 0.0f;
		
		for( int a = aBand;a < leftChannel.length - aBand; a++ ) {
			wLeftMid  = Math.max(wLeftMid, leftChannel[ a ] );
			wRightMid = Math.max(wRightMid, rightChannel[ a ] );
		}
		
		wLeftMid  *= 2.0;
		wRightMid *= 2.0;

		float wLeftHigh  = 0.0f;
		float wRightHigh = 0.0f;
		for( int a = leftChannel.length - aBand; a < leftChannel.length; a++ ) {
			wLeftHigh  = Math.max(wLeftHigh,  leftChannel[ a ] );
			wRightHigh = Math.max(wRightHigh,  rightChannel[ a ] );

		}
		
		wLeftHigh  *= 2.0;
		wRightHigh *= 2.0;


		if ( wLeftLow > 1.0f ) {
			wLeftLow = 1.0f;
		}
		
		if ( wRightLow > 1.0f ) {
			wRightLow = 1.0f;
		}

		if ( wLeftMid > 1.0f ) {
			wLeftMid = 1.0f;
		}
		
		if ( wRightMid > 1.0f ) {
			wRightMid = 1.0f;
		}

		if ( wLeftHigh > 1.0f ) {
			wLeftHigh = 1.0f;
		}
		
		if ( wRightHigh > 1.0f ) {
			wRightHigh = 1.0f;
		}
		
		vuMeterLeftB = wLeftLow;
		vuMeterRightB = wRightLow;

		vuMeterLeft = wLeftHigh;
		vuMeterRight = wRightHigh;

		needRedraw();
	}

	/**
	 * Progress callback while playing.
	 * 
	 * This method is called severals time per seconds while playing. properties map includes audio format features such as instant bitrate, microseconds position, current frame number, ...
	 * 
	 * @param bytesread
	 *            from encoded stream.
	 * @param microseconds
	 *            elapsed (<b>reseted after a seek !</b>).
	 * @param pcmdata
	 *            PCM samples.
	 * @param properties
	 *            audio stream parameters.
	 */
	public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties) {
			synchronized (this) {
				currentProperties = properties;
				extractProperties();
				// long aCurrentDoneMs = getPositionMs();
				long aCurrentLeftMs = getLeftTimeMs();
				if (aCurrentLeftMs < fadeDuration && parentKDJ.autoMix
						// Be sure this is the active player
						&& playState == 1 && currentGain > 0.1) {
					parentKDJ.timeToNext(this);
				}
				needRedraw();
			}
	}

	/**
	 * Notification callback for basicplayer events such as opened, eom ...
	 * 
	 * @param event
	 */
	public void stateUpdated(BasicPlayerEvent event) {
		// Be careful, this place is not a good one to manage events : called asynchronously
		if (event.getCode() == BasicPlayerEvent.EOM
				// Be sure this is the active player
				&& playState == 1 && currentGain > 0.1) {
			parentKDJ.timeToNext(this);
		}
		needRedraw();
	}

	/**
	 * A handle to the BasicPlayer, plugins may control the player through the controller (play, stop, ...)
	 * 
	 * @param controller :
	 *            a handle to the player
	 */
	public void setController(BasicController controller) {
		display("setController : " + controller);
	}

	/**
	 * @param msg
	 */
	public void display(String msg) {
		if (out == null) {
			return;
		}
		if (out != null)
			out.println(msg);
	}

	/**
	 * 
	 */
	void loadImg() {
		if (parentKDJ == null) {
			return;
		}
		InputStream aIS = getClass().getResourceAsStream("img/playerBck.png");
		playerBck = new Image(parentKDJ.display, aIS);
		aIS = getClass().getResourceAsStream("img/playerBck.png");
		dblBuf = new Image(parentKDJ.display, aIS);
		dblBufGC = new GC(dblBuf);
	}

	/**
	 * 
	 */
	void extractProperties() {
		if (openProperties != null) {
			currentAuthor = (String) openProperties.get("author");
			if (currentAuthor == null) {
				currentAuthor = "[ERROR]";
			}
			currentTitle = (String) openProperties.get("title");
			if (currentTitle == null) {
				currentTitle = "[ERROR]";
			}
			currentDuration = ((Long) openProperties.get("duration")).longValue() / 1000;
			currentLengthBytes = ((Integer) openProperties.get("mp3.length.bytes")).longValue();
		}
		if (currentProperties != null) {
			currentPositionMs = ((Long) currentProperties.get("mp3.position.microseconds")).longValue() / 1000;
		}
	}

	/**
	 * 
	 */
	protected void paint() {
		if (playerBck == null) {
			loadImg();
		}
		if (dblBuf == null || playerBounds == null) {
			return;
		}
		try {
			GC aGC = dblBufGC;
			int aPosX = 0, aPosY = 0;
			aGC.setClipping(aPosX, aPosY, playerBounds.width, playerBounds.height);
			// aGC.fillRectangle(aPosX, aPosY, posW, posH);
			aGC.drawImage(playerBck, aPosX, aPosY);
			aGC.setForeground(parentKDJ.playerBarC);
			aGC.setLineWidth(2);
			aGC.drawRectangle(aPosX, aPosY, playerBounds.width, playerBounds.height);
			// Advance (num)
			Color aPlayColor = parentKDJ.playerC;
			Color aBarColor = parentKDJ.playerBarC;
			aGC.setClipping(aPosX + 5, aPosY + 5, playerBounds.width - 10, playerBounds.height - 10);
			aGC.setForeground(aPlayColor);// parentJDJ.whiteC);
			aGC.setFont(parentKDJ.initialFont);
			aGC.drawText("" + currentAuthor, aPosX + 5, aPosY + 5, true);
			aGC.drawText("" + currentTitle, aPosX + 5, aPosY + 20, true);
			aGC.setFont(parentKDJ.largeFont);
			long aLeftMs = getLeftTimeMs();
			long aDoneMs = getPositionMs();
			long aDurationMs = getDurationTimeMs();
			if (aLeftMs > 0 && aLeftMs < 2 * fadeDuration && playState > 0) {
				aGC.setForeground(parentKDJ.redC);
				aGC.drawText("" + getLeftTimeFormated(), aPosX + 12, aPosY + 33, true);
				aGC.setForeground(aPlayColor);
			} else {
				aGC.drawText("" + getLeftTimeFormated(), aPosX + 12, aPosY + 33, true);
			}
			aGC.setFont(parentKDJ.initialFont);
			aGC.drawText("" + getDoneTimeFormated() + " / " + getDurationTimeFormated(), aPosX + 5, playerBounds.height - 32, true);
			// Progress (bar)
			if (aLeftMs > 0 && aLeftMs < 2 * fadeDuration && playState > 0) {
				aGC.setBackground(parentKDJ.redC);
			} else {
				aGC.setBackground(aBarColor);
			}

			//EM 06/11/2008 : display mix range
			if(aDurationMs > 0){
				double aPercentMix = (double) fadeDuration  / (double) aDurationMs;
				aGC.setForeground(parentKDJ.redC);
				aGC.setLineWidth(1);
				//aGC.drawRectangle(progessBarR.x + progessBarR.width - (int) (progessBarR.width * aPercentMix) - 1, progessBarR.y + 1, (int) (progessBarR.width * aPercentMix) - 1, progessBarR.height - 2);
				aGC.drawLine(progessBarR.x + progessBarR.width - (int) (progessBarR.width * aPercentMix) - 1, progessBarR.y + 1, 
						progessBarR.x + progessBarR.width - (int) (progessBarR.width * aPercentMix) - 1, progessBarR.y + progessBarR.height);
				aGC.drawLine(progessBarR.x + progessBarR.width - (int) (2.0 * progessBarR.width * aPercentMix) - 1, progessBarR.y + 4, progessBarR.x + progessBarR.width - (int) (2.0 * progessBarR.width * aPercentMix) - 1, progessBarR.y + progessBarR.height - 3);
			}

			double aProgress = (double) aDoneMs / (double) aDurationMs;
			progessBarR.x = aPosX + 5;
			progessBarR.y = aPosY + playerBounds.height - 16;
			progessBarR.width = playerBounds.width - 23;
			progessBarR.height = 10;
			aGC.setForeground(aBarColor);// parentJDJ.blackC);
			aGC.fillRectangle(progessBarR.x, progessBarR.y, (int) (progessBarR.width * aProgress), progessBarR.height);
			aGC.setLineWidth(1);
			aGC.drawRectangle(progessBarR.x, progessBarR.y, progessBarR.width - 1, progessBarR.height);
			
			// Volume factor
			aGC.setLineWidth(1);
			aGC.setBackground(aBarColor);
			aGC.setForeground(aBarColor);// parentJDJ.blackC);
			volumeBarR.x = aPosX + playerBounds.width - 14;
			volumeBarR.y = aPosY + playerBounds.height - 6;
			volumeBarR.width = 8;
			volumeBarR.height = -((playerBounds.height - 11));
			aGC.fillRectangle(volumeBarR.x, volumeBarR.y, volumeBarR.width, (int) (volumeBarR.height * currentGain));
			aGC.drawRectangle(volumeBarR.x, volumeBarR.y, volumeBarR.width, volumeBarR.height);

			//EM 11/11/2008 : vu meters
			if(parentKDJ.showSoundProfile){
				aGC.setBackground(parentKDJ.yellowC);
				aGC.fillRectangle(volumeBarR.x + 4, volumeBarR.y, 1, (int) (volumeBarR.height * currentGain * vuMeterLeft));
				aGC.fillRectangle(volumeBarR.x + 5, volumeBarR.y, 1, (int) (volumeBarR.height * currentGain * vuMeterRight));
				aGC.setBackground(parentKDJ.redC);//.playerC);
				aGC.fillRectangle(volumeBarR.x + 3, volumeBarR.y, 1, (int) (volumeBarR.height * currentGain * vuMeterLeftB));
				aGC.fillRectangle(volumeBarR.x + 6, volumeBarR.y, 1, (int) (volumeBarR.height * currentGain * vuMeterRightB));
			}
			
			// Spectrum
			// if(player != null){
			// for(int i = 0;i < player.frequencyCount;i++){
			// aGC.fillRectangle(aPosX + playerBounds.width - 12 - (
			// player.frequencyCount - i), aPosY + playerBounds.height - 5, 2,
			// -(int) ((playerBounds.height - 10) * player.sum[i] / 10)); }
			// }
			
			GC aPlayerGC = new GC(this);
			paintDbl(aPlayerGC);
			aPlayerGC.dispose();
			
		} catch (Throwable t) {
			System.out.println("Warning, can't paint : " + t);
			// t.printStackTrace(System.out);
		}
	}
	
	void paintDbl(GC aPlayerGC) {
		if(dblBuf == null) {
			return;
		}
		aPlayerGC.setClipping(playerBounds.x, playerBounds.y, playerBounds.width, playerBounds.height);
		aPlayerGC.setForeground(parentKDJ.blackC);
		aPlayerGC.drawImage(dblBuf, playerBounds.x, playerBounds.y);
	}

	/**
	 * @return
	 */
	long getLeftTimeMs() {
		return currentDuration - currentPositionMs;
	}

	/**
	 * @return
	 */
	String getLeftTimeFormated() {
		return formatTimeMs(getLeftTimeMs());
	}

	/**
	 * @return
	 */
	long getPositionMs() {
		return currentPositionMs;
	}

	/**
	 * @return
	 */
	String getDoneTimeFormated() {
		return formatTimeMs(getPositionMs());
	}

	/**
	 * @return
	 */
	long getDurationTimeMs() {
		return currentDuration;
	}

	String getDurationTimeFormated() {
		return formatTimeMs(getDurationTimeMs());
	}

	/**
	 * @param aMsAll
	 * @return
	 */
	//EM 13/09/2008 : turn to public static 
	public static String formatTimeMs(long aMsAll) {
		long aTime = aMsAll;
		long aSecAll = aTime / 1000;
		long aMin = aSecAll / 60;
		long aSec = aSecAll % 60;
		long aMs = (aTime % 1000) / 10;
		return ddNF.format(aMin) + ":" + ddNF.format(aSec) + ":" + ddNF.format(aMs);
	}
}
