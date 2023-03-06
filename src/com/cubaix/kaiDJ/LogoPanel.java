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


	public LogoPanel(KaiDJ aParentJDJ, Composite parent, int style) {
		super(aParentJDJ, parent, style);
		parentJDJ = aParentJDJ;
		final LogoPanel aThis = this;
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent aPE) {
				panelBounds = aThis.getClientArea();
				// panelGC = aPE.gc;
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

	/* (non-Javadoc)
	 * @see em.jDJ.TimedCanvas#paint()
	 */
	protected void paint() {
		if (panelBck == null) {
			loadImg();
		}
		if(panelBck == null || panelBounds == null){
			return;
		}
		try {
			GC panelGC = new GC(this);
			Rectangle aImgR = panelBck.getBounds();
			panelGC.setClipping(panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height);
			panelGC.setForeground(parentJDJ.blackC);
			panelGC.setBackground(parentJDJ.mainBckC);
			panelGC.fillRectangle(panelBounds);
			if (help == null) {
				panelGC.drawImage(panelBck, panelBounds.x /* + (panelBounds.width - aImgR.width) / 2 */, panelBounds.y);
			} else {
				// panelGC.drawImage(panelTransBck, panelBounds.x + (panelBounds.width - aImgR.width) / 2, panelBounds.y);
				panelGC.drawImage(panelBck, panelBounds.x, panelBounds.y);
				StringTokenizer aTok = new StringTokenizer(help, "\n");
				int aPosY = 0;
				panelGC.setForeground(parentJDJ.playerC);
				// panelGC.drawText("** HELP **", aImgR.width, aPosY, true);
				aPosY += lineHeight;
				while (aTok.hasMoreTokens()) {
					String aStr = aTok.nextToken();
					panelGC.drawText(aStr, panelBounds.x + aImgR.width, panelBounds.y + aPosY, true);
					aPosY += lineHeight;
				}
			}
			panelGC.dispose();
		} catch (Throwable t) {
			System.err.println("Can't paint logo : " + t);
			t.printStackTrace(System.err);
		}
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
