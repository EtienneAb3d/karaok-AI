package com.cubaix.kai;

import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import com.cubaix.kaiDJ.KaiDJ;
import com.cubaix.kaiDJ.db.SongDescr;
import com.cubaix.kaiDJ.swt.TimedCanvas;

public class KaiScreener extends TimedCanvas {
	KaiViewer parentKV = null; 

	Rectangle panelBounds = null;
	Image dblBuf = null;
	Rectangle dblBounds = new Rectangle(0, 0, 1280, 720);
	GC dblBufGC;
	
	Image fullScreenBut = null;

	int currentKaiIdx = -1;
	int currentKaiIdxJustBefore = -1;

	public KaiScreener(KaiViewer aParentKV, Composite parent, int style) {
		super(aParentKV.parentKE.parentKDJ, parent, style);
		parentKV = aParentKV;
		
		InputStream aIS = getClass().getResourceAsStream("img2/fullscreen.png");
		fullScreenBut = new Image(parentKDJ.display, aIS);

		final KaiScreener aThis = this;
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent aPE) {
				panelBounds = getClientArea();
				GC aPlGC = new GC(aThis);
				paintDbl(aPlGC);
				aPlGC.dispose();
				needRedraw();
			}
		});
		addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent arg0) {
			}
			
			@Override
			public void keyPressed(KeyEvent arg0) {
			}
		});
		Thread aTrackTh = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!aThis.isDisposed()) {
					try {
						SongDescr aSong = parentKV.parentKE.song;
						if(aSong != null && aSong.kaiSrt != null) {
							int aKaiIdx = aSong.kaiSrt.getChunkIdx(parentKV.parentKE.playerVocals.getPositionMs());
							int aJustBef = aSong.kaiSrt.getChunkIdxJustBefore(parentKV.parentKE.playerVocals.getPositionMs());
							if(aKaiIdx != currentKaiIdx || aJustBef != currentKaiIdxJustBefore) {
								currentKaiIdx = aKaiIdx;
								currentKaiIdxJustBefore = aJustBef;
								needRedraw();
								parentKV.parentKE.logoPanel.needRedraw();
							}
						}
						Thread.sleep(10);
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
			}
		});
		aTrackTh.start();
	}

	@Override
	protected void paintTimed() {
		if(KaiDJ._DEBUG_PAINT) {
			System.out.println("SCREENER.paintTimed()");
		}
		try {
			if (dblBuf == null) {
				dblBuf = new Image(parentKDJ.display,dblBounds.width,dblBounds.height);
				dblBufGC = new GC(dblBuf);
			}

			dblBufGC.setClipping(0, 0,dblBounds.width,dblBounds.height);
			dblBufGC.setBackground(parentKDJ.mainBckC);
			dblBufGC.fillRectangle(0, 0,dblBounds.width,dblBounds.height);
			
			SongDescr aSong = parentKV.parentKE.song;
			if(aSong != null && aSong.kaiSrt != null) {
				currentKaiIdx = aSong.kaiSrt.getChunkIdx(parentKV.parentKE.playerVocals.getPositionMs());
				int aJustBef = aSong.kaiSrt.getChunkIdxJustBefore(parentKV.parentKE.playerVocals.getPositionMs());

				dblBufGC.setFont(parentKDJ.viewerFont);
				int aPadding = 5;

				Point aC = dblBufGC.textExtent("M");
				int aPosY = dblBounds.y+dblBounds.height/2;
				for(int i = currentKaiIdx;i >= 0 && i >= currentKaiIdx-5;i--) {
					if(i >= aSong.kaiSrt.chunkTexts.size()) {
						aPosY -= aC.y;
						continue;
					}
					String aKaiText = aSong.kaiSrt.chunkTexts.elementAt(i);
					if(i == currentKaiIdx) {
						Point aE = dblBufGC.textExtent(aKaiText);
						aPosY -= aE.y/2;
						continue;
					}

					for(String aT : aKaiText.split("\n")) {
						aPosY -= aC.y;
					}
					aPosY -= aC.y;
				}
				for(int i = currentKaiIdx-5;i < aSong.kaiSrt.chunkTexts.size();i++) {
					if(aPosY > dblBounds.height) {
						break;
					}
					if(i < 0) {
						continue;
					}
//					if(i == aJustBef) {
//						dblBufGC.setForeground(parentKDJ.logoLightC);
//						dblBufGC.drawLine(dblBounds.x, aPosY, dblBounds.x+dblBounds.width, aPosY);
//					}
					String aKaiText = aSong.kaiSrt.chunkTexts.elementAt(i);
					if(i == currentKaiIdx) {
						dblBufGC.setForeground(parentKDJ.logoLightC);
					}
					else {
						dblBufGC.setForeground(parentKDJ.logoDarkC);
					}
					for(String aT : aKaiText.split("\n")) {
						aT = aT.trim();
						if(i == aJustBef) {
							aT = "⇢⇢⇢  "+aT+"  ⇠⇠⇠";
						}
						Point aE = dblBufGC.textExtent(aT);
						dblBufGC.drawText(aT,dblBounds.x + dblBounds.width/2 - aE.x/2, dblBounds.y + aPosY, true);
						aPosY += aC.y;
					}
					aPosY += aC.y;
				}
				
			}

			dblBufGC.drawImage(fullScreenBut, dblBounds.width-40, dblBounds.height-40);
			
			// Draw final image
			GC aPlGC = new GC(this);
			paintDbl(aPlGC);
			aPlGC.dispose();
		} catch (Throwable t) {
			System.err.println("Can't paint screen : " + t);
			t.printStackTrace(System.err);
		}
	}

	void paintDbl(GC aPlGC) {
		if(dblBuf == null) {
			return;
		}
		aPlGC.setAntialias(SWT.ON);
		aPlGC.setInterpolation(SWT.HIGH);
		aPlGC.drawImage(dblBuf
				,0, 0, dblBounds.width, dblBounds.height
				, 0, 0,panelBounds.width,panelBounds.height);
	}

}
