package com.cubaix.kaiDJ.swt;

import java.io.InputStream;
import java.util.Vector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.cubaix.kaiDJ.KaiDJ;

public class KaiButton extends Canvas {
	public static final int _WIDTH = 60;
	public static final int _HEIGHT = 25;
	
	KaiDJ parentJDJ;
	
	static Image butBck = null;
	static Image butBckMid = null;
	static Image butBckDark = null;
	String imgPath = null;
	Image img = null;

	String text;

	boolean isToggle = false;
	boolean selection = false;
	Vector selectionListeners = new Vector();
	int state = 0;//0 current selection state, 1 over
	
	String toolTip = null;
	
	Rectangle buttonBounds = null;

	public KaiButton(KaiDJ aParentJDJ,Composite aParent, int aStyle) {
		super(aParent, aStyle);
		parentJDJ = aParentJDJ;
		isToggle = (aStyle & SWT.TOGGLE) != 0;
		this.addPaintListener(new PaintListener(){
			public void paintControl(PaintEvent aPE) {
				buttonBounds = getClientArea();
				paint();
			}
		});
		this.addMouseTrackListener(new MouseTrackListener(){
			public void mouseEnter(MouseEvent arg0) {
				parentJDJ.logoPanel.help = toolTip;
				parentJDJ.logoPanel.needRedraw();
				state = 1;
				needRedraw();
			}
			public void mouseExit(MouseEvent arg0) {
				parentJDJ.logoPanel.help = null;
				parentJDJ.logoPanel.needRedraw();
				state = 0;
				if(!isToggle){
					//Always deselected
					selection = false;
				}
				needRedraw();
			}
			public void mouseHover(MouseEvent arg0) {
			}
		});
		this.addMouseListener(new MouseListener(){
			public void mouseDoubleClick(MouseEvent aEvt) {
			}
			public void mouseDown(MouseEvent aEvt) {
//				state = 0;
				if(isToggle){
					selection = !selection;
				}
				else{
					selection = true;
				}
				needRedraw();
				notifySelection();
			}
			public void mouseUp(MouseEvent aEvt) {
//				state = 0;
				if(!isToggle){
					//Always deselected
					selection = false;
				}
				needRedraw();
//				notifySelection();
			}
			
		});
	}
	
	void notifySelection(){
		for(int i = 0;i < selectionListeners.size();i++){
			((SelectionListener)selectionListeners.elementAt(i)).widgetSelected(null);
		}
	}
	
	public void setSelection(boolean aSel){
		selection = aSel;
	}
	public boolean getSelection(){
		return selection;
	}
	
	public void setImagePath(String aPath){
		imgPath = aPath;
	}
	
	public void setText(String aText){
		text = aText;
	}
	
	public void setToolTipText(String aText){
		toolTip = aText;
	}
	
	public void addSelectionListener(SelectionListener aListener){
		selectionListeners.add(aListener);
	}
	
	void needRedraw() {
		if (parentJDJ.display == null || parentJDJ.display.isDisposed()) {
			// Wait until it is available
			return;
		}
		// Use SWT async
		final KaiButton aThis = this;
		parentJDJ.display.asyncExec(new Runnable() {
			public void run() {
				try {
					buttonBounds = aThis.getClientArea();
					paint();
				} catch (Throwable t) {
				}
			}
		});
	}
	
	/**
	 * 
	 */
	void loadBckImg() {
		if (parentJDJ == null || parentJDJ.display == null || parentJDJ.display.isDisposed()) {
			return;
		}
		InputStream aIS = KaiDJ.class.getResourceAsStream("img2/butBck.png");
		butBck = new Image(parentJDJ.display, aIS);
		aIS = KaiDJ.class.getResourceAsStream("img2/butBckMid.png");
		butBckMid = new Image(parentJDJ.display, aIS);
		aIS = KaiDJ.class.getResourceAsStream("img2/butBckDark.png");
		butBckDark = new Image(parentJDJ.display, aIS);
	}

	void loadImg() {
		if (parentJDJ == null || parentJDJ.display == null || parentJDJ.display.isDisposed()) {
			return;
		}
		if(imgPath == null){
			return;
		}
		InputStream aIS = KaiDJ.class.getResourceAsStream(imgPath);
		img = new Image(parentJDJ.display, aIS);
	}

	void paint() {
		if (butBck == null) {
			loadBckImg();
		}
		if (img == null) {
			loadImg();
		}
		if(butBck == null || img == null || buttonBounds == null){
			return;
		}
		try{
			GC aGC = new GC(this);
//			aPlayerGC.setClipping(playerBounds.x, playerBounds.y,
//					playerBounds.width, playerBounds.height);
			
			aGC.setClipping(buttonBounds.x, buttonBounds.y, buttonBounds.width, buttonBounds.height);
			aGC.setForeground(parentJDJ.blackC);
			aGC.setBackground(parentJDJ.mainBckC);
			aGC.fillRectangle(buttonBounds);

			Image aImgBck;
			if(state == 1){
				//Over
				aImgBck = butBck;
			}
			else{
				//Current selection
				if(selection){
					aImgBck = butBckMid;
				}
				else{
					aImgBck = butBckDark;
				}
			}

			aGC.drawImage(aImgBck, buttonBounds.x, buttonBounds.y);
			aGC.drawImage(img, buttonBounds.x + 8, buttonBounds.y + 5);
//			aGC.setFont(parentJDJ.initialFont);
//			aGC.drawText(text,32 - (int)(3.7 * (text.length()/2)),0,true);

			if (isToggle) {
				if (selection) {
					aGC.setBackground(parentJDJ.playerC);
				} else {
					//EM 06/07/2008 : background color for off state
					aGC.setBackground(parentJDJ.mainBckC/*.redC*/);
				}
				aGC.setForeground(parentJDJ.playerBarC);
				aGC.fillOval(buttonBounds.x + 40, buttonBounds.y + 8, 8, 8);
				aGC.drawOval(buttonBounds.x + 40, buttonBounds.y + 8, 8, 8);
			}
			
			if(state == 1){
				//Over
				aGC.setForeground(parentJDJ.playerC);
			}
			else{
				aGC.setForeground(parentJDJ.playerBarC);
			}
			aGC.setLineWidth(1);
			aGC.drawRectangle(buttonBounds.x,buttonBounds.y + 1,_WIDTH - 1,_HEIGHT - 2);

			aGC.dispose();
		} catch (Throwable t) {
			System.err.println("Can't paint button : " + t);
			t.printStackTrace(System.err);
		}
	}
}
