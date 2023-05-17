package com.cubaix.kai;

import java.io.InputStream;
import java.util.StringTokenizer;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import com.cubaix.kaiDJ.DJPlayer;
import com.cubaix.kaiDJ.KaiDJ;
import com.cubaix.kaiDJ.swt.TimedCanvas;

public class LogoPanel extends TimedCanvas {
	private KaiEditor parentKE;
	Image panelBck = null;
//	Image panelTransBck = null;
	public String help = null;
	final int lineHeight = 15;

	Rectangle panelBounds = null;
	Image dblBuf = null;
	GC dblBufGC;

	public LogoPanel(KaiEditor aParentKE, Composite parent, int style) {
		super(aParentKE.parentKDJ, parent, style);
		parentKE = aParentKE;
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
	}
	
	void loadImg() {
		if (parentKDJ == null || parentKDJ.display == null || parentKDJ.display.isDisposed()) {
			return;
		}
		InputStream aIS = getClass().getResourceAsStream("img/LogoSmall.png");
		panelBck = new Image(parentKDJ.display, aIS);
		
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
			dblBufGC.setForeground(parentKDJ.blackC);
			dblBufGC.setBackground(parentKDJ.mainBckC);
			dblBufGC.fillRectangle(panelBounds);
			if (help == null) {
				dblBufGC.drawImage(panelBck, panelBounds.x /* + (panelBounds.width - aImgR.width) / 2 */, panelBounds.y);
			} else {
				// dblBufGC.drawImage(panelTransBck, panelBounds.x + (panelBounds.width - aImgR.width) / 2, panelBounds.y);
				dblBufGC.drawImage(panelBck, panelBounds.x, panelBounds.y);
				StringTokenizer aTok = new StringTokenizer(help, "\n");
				int aPosY = 0;
				dblBufGC.setFont(parentKDJ.initialFont);
				dblBufGC.setForeground(parentKDJ.playerC);
				// dblBufGC.drawText("** HELP **", aImgR.width, aPosY, true);
				aPosY += lineHeight;
				while (aTok.hasMoreTokens()) {
					String aStr = aTok.nextToken();
					dblBufGC.drawText(aStr, panelBounds.x + aImgR.width, panelBounds.y + aPosY, true);
					aPosY += lineHeight;
				}
			}

			dblBufGC.setForeground(parentKDJ.playerC);
			if(parentKE.song != null && parentKE.song.kaiSrt != null) {
				dblBufGC.setFont(parentKDJ.kaiFont);
				dblBufGC.setForeground(parentKDJ.playerC);
				int aPadding = 5;

				int aKaiIdx = parentKE.song.kaiSrt.getChunkIdx(parentKE.playerVocals.getPositionMs());
				String aKaiText = (aKaiIdx < 0) ? "" : ((aKaiIdx >= parentKE.song.kaiSrt.chunks.size()) ? "" :parentKE.song.kaiSrt.chunks.elementAt(aKaiIdx).getText());
				aKaiText = aKaiText.replaceAll("\n", " ").trim();
				
				for(int i = aKaiIdx+1;i < parentKE.song.kaiSrt.chunks.size();i++) {
					String aKaiTextNext = parentKE.song.kaiSrt.chunks.elementAt(i).getText();
					aKaiText += (aKaiText.length() > 0 ? " — ":"")+aKaiTextNext.replaceAll("\n", " ").trim();
				}
				
				dblBufGC.drawText(aKaiText,panelBounds.x + aPadding, panelBounds.y + panelBounds.height - aPadding - 18, true);
			}
			if(parentKDJ.player1.kaiSrt != null || parentKDJ.player2.kaiSrt != null) {
				needRedrawSlow();
			}
			
			// Draw final image
			GC aPlGC = new GC(this);
			paintDbl(aPlGC);
			aPlGC.dispose();
		} 
		catch (Throwable t) {
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
}
