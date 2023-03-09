package com.cubaix.kaiDJ;

import java.io.InputStream;
import java.util.StringTokenizer;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import com.cubaix.kaiDJ.swt.TimedCanvas;

public class LogoPanel extends TimedCanvas {
	private KaiDJ parentJDJ;
	Image panelBck = null;
//	Image panelTransBck = null;
	public String help = null;
	final int lineHeight = 15;

	Rectangle panelBounds = null;
	Image dblBuf = null;
	GC dblBufGC;

	public LogoPanel(KaiDJ aParentJDJ, Composite parent, int style) {
		super(aParentJDJ, parent, style);
		parentJDJ = aParentJDJ;
		final LogoPanel aThis = this;
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent aPE) {
				panelBounds = getClientArea();
				GC aPlGC = new GC(aThis);
				paintDbl(aPlGC);
				aPlGC.dispose();
				needRedraw();
			}
		});
		
		//EM 15/11/2008
		createSoundProfileThread();
	}
	
	void loadImg() {
		if (parentJDJ == null || parentJDJ.display == null || parentJDJ.display.isDisposed()) {
			return;
		}
		InputStream aIS = getClass().getResourceAsStream("img/LogoSmall.png");
		panelBck = new Image(parentJDJ.display, aIS);
		
//		panelTransBck = new Image(parentJDJ.display, aIS);
//		byte[] aAlpha = panelTransBck.getImageData().alphaData;
//		if(aAlpha != null){
//			for(int i = 0;i < aAlpha.length;i++){
//				aAlpha[i] = 127;
//			}
//		}
	}

	protected void paintTimed() {
		if(KaiDJ._DEBUG_PAINT) {
			System.out.println("LOGOPANEL.paintTimed()");
		}
		if (panelBck == null) {
			loadImg();
		}
		if(panelBck == null || panelBounds == null){
			return;
		}
		try {
			Rectangle aRectDbl = null;
			if (dblBuf != null) {
				aRectDbl = dblBuf.getBounds();
			}
			if (dblBuf == null || panelBounds.width != aRectDbl.width || panelBounds.height != aRectDbl.height) {
				if (dblBuf != null) {
					dblBuf.dispose();
					dblBuf = null;
					dblBufGC.dispose();
				}
				dblBuf = new Image(parentKDJ.display, panelBounds);
				dblBufGC = new GC(dblBuf);
			}

			Rectangle aImgR = panelBck.getBounds();
			dblBufGC.setClipping(panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height);
			dblBufGC.setForeground(parentJDJ.blackC);
			dblBufGC.setBackground(parentJDJ.mainBckC);
			dblBufGC.fillRectangle(panelBounds);
			if (help == null) {
				dblBufGC.drawImage(panelBck, panelBounds.x /* + (panelBounds.width - aImgR.width) / 2 */, panelBounds.y);
			} else {
				// dblBufGC.drawImage(panelTransBck, panelBounds.x + (panelBounds.width - aImgR.width) / 2, panelBounds.y);
				dblBufGC.drawImage(panelBck, panelBounds.x, panelBounds.y);
				StringTokenizer aTok = new StringTokenizer(help, "\n");
				int aPosY = 0;
				dblBufGC.setForeground(parentJDJ.playerC);
				// dblBufGC.drawText("** HELP **", aImgR.width, aPosY, true);
				aPosY += lineHeight;
				while (aTok.hasMoreTokens()) {
					String aStr = aTok.nextToken();
					dblBufGC.drawText(aStr, panelBounds.x + aImgR.width, panelBounds.y + aPosY, true);
					aPosY += lineHeight;
				}
			}

			// Draw final image
			GC aPlGC = new GC(this);
			paintDbl(aPlGC);
			aPlGC.dispose();
		} catch (Throwable t) {
			System.err.println("Can't paint logo : " + t);
			t.printStackTrace(System.err);
		}
	}
	
	void paintDbl(GC aPlGC) {
		if(dblBuf == null) {
			return;
		}
		aPlGC.drawImage(dblBuf, 0, 0);
	}

	//EM 15/11/2008
	static final int _SOUNDPROFILESIZE = 128;
	float[] soundProfile1 = new float[_SOUNDPROFILESIZE];
	float[] soundProfile2 = new float[_SOUNDPROFILESIZE];
	int soundProfilePos = 0;
	void createSoundProfileThread(){
		Thread aTh = new Thread(new Runnable(){
			public void run() {
				while(true){
					try{
						Thread.sleep(40);
						//Read values
						soundProfile1[soundProfilePos] = (float)parentJDJ.player1.currentGain * (parentJDJ.player1.vuMeterRight + parentJDJ.player1.vuMeterLeft) / 2;
						soundProfile2[soundProfilePos] = (float)parentJDJ.player2.currentGain * (parentJDJ.player2.vuMeterRight + parentJDJ.player2.vuMeterLeft) / 2;
						soundProfilePos++;
						soundProfile1[soundProfilePos] = (float)parentJDJ.player1.currentGain * (parentJDJ.player1.vuMeterRightB + parentJDJ.player1.vuMeterLeftB) / 2;
						soundProfile2[soundProfilePos] = (float)parentJDJ.player2.currentGain * (parentJDJ.player2.vuMeterRightB + parentJDJ.player2.vuMeterLeftB) / 2;
						soundProfilePos++;
						soundProfilePos = soundProfilePos % _SOUNDPROFILESIZE;

						//EM 28/04/2009 : only if activated
						if(parentJDJ.showSoundProfile){
							parentJDJ.display.asyncExec(new Runnable() {
								public void run() {
									try {
										paintSoundProfile();
									} catch (Throwable t) {
									}
								}
							});
						}

					}
					catch(Throwable t){
						//??
					}
				}
			}
		});
		aTh.start();
	}
	
	static final int _SOUNDPROFILEHEIGHT = 20;
	void paintSoundProfile(){
		if(KaiDJ._DEBUG_PAINT) {
			System.out.println("LOGOPANEL.paintSoundProfile()");
		}
		if(panelBck == null || panelBounds == null){
			return;
		}
		try {
			GC panelGC = new GC(this);
			Rectangle aImgR = panelBck.getBounds();
			//panelGC.setClipping(panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height);
			
			for(int i = 0; i < _SOUNDPROFILESIZE;){
				panelGC.setForeground(parentJDJ.mainBckC);
				panelGC.drawLine(panelBounds.x + i, panelBounds.y + aImgR.height + 28, panelBounds.x + i, panelBounds.y + aImgR.height + 10 - _SOUNDPROFILEHEIGHT);
				panelGC.drawLine(panelBounds.x + i + 1, panelBounds.y + aImgR.height + 28, panelBounds.x + i + 1, panelBounds.y + aImgR.height + 10 - _SOUNDPROFILEHEIGHT);
				panelGC.setForeground(parentJDJ.redC);
				panelGC.drawLine(panelBounds.x + i, panelBounds.y + aImgR.height + 10, panelBounds.x + i, panelBounds.y + aImgR.height + 10 - (int)(_SOUNDPROFILEHEIGHT*soundProfile1[(soundProfilePos + i)%_SOUNDPROFILESIZE]));
				//EM 28/04/2009 : bottom to top
				panelGC.drawLine(panelBounds.x + i, panelBounds.y + aImgR.height + 28, panelBounds.x + i, panelBounds.y + aImgR.height + 28 - (int)(_SOUNDPROFILEHEIGHT*soundProfile2[(soundProfilePos + i)%_SOUNDPROFILESIZE]));
				i++;
				panelGC.setForeground(parentJDJ.yellowC);
				panelGC.drawLine(panelBounds.x + i, panelBounds.y + aImgR.height + 10, panelBounds.x + i, panelBounds.y + aImgR.height + 10 - (int)(_SOUNDPROFILEHEIGHT*soundProfile1[(soundProfilePos + i)%_SOUNDPROFILESIZE]));
				//EM 28/04/2009 : bottom to top
				panelGC.drawLine(panelBounds.x + i, panelBounds.y + aImgR.height + 28, panelBounds.x + i, panelBounds.y + aImgR.height + 28 - (int)(_SOUNDPROFILEHEIGHT*soundProfile2[(soundProfilePos + i)%_SOUNDPROFILESIZE]));
				i++;
			}
			panelGC.dispose();
		} catch (Throwable t) {
			System.err.println("Can't paint logo : " + t);
			t.printStackTrace(System.err);
		}
	}
}
