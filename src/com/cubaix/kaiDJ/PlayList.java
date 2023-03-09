package com.cubaix.kaiDJ;

import java.text.NumberFormat;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.cubaix.kaiDJ.db.SongDescr;
import com.cubaix.kaiDJ.db.SongGroup;
import com.cubaix.kaiDJ.swt.KaiScrollBar;
import com.cubaix.kaiDJ.swt.PlayListListener;
import com.cubaix.kaiDJ.swt.TimedCanvas;
import com.cubaix.kaiDJ.xml.PlayListLoader;

public class PlayList extends Composite {
	protected KaiDJ parentKDJ;
	public TimedCanvas listTC = null;
	KaiScrollBar vSB = null;
	KaiScrollBar hSB = null;

	Vector pl = new Vector();

	int offsetX = 0;
	int offsetIndex = 0;

	boolean movingColMark = false;

	// int selectedIndex = -1;
	TreeSet selectedPlIds = new TreeSet();

	// int beforeClickedIndex = -1;//selection before the click
	Integer beforeClickedPlId = null;

	// int clickedIndex = -1;//clicked pos stored while dragging
	Integer clickedPlId = null;

	//int playingIndex = -1;
	Integer playingPlId = null;

	NumberFormat ddddNF;

	Image dblBuf = null;
	GC dblBufGC;

	Rectangle plBounds;

	final int lineHeight = 15;

	int[] colWidths = new int[] { 40, 50, 100, 150, 400 };

	Vector playListListeners = new Vector();
	
	//EM 06/01/2008
	TreeMap rndDone = new TreeMap();
	int rndMaxPlay = 0;

	/**
	 * @param aParentKDJ
	 * @param aPosX
	 * @param aPosY
	 * @param aPosW
	 * @param aPosH
	 */
	PlayList(KaiDJ aParentKDJ, Composite parent, int style) {
		super(parent, style | SWT.NO_BACKGROUND | SWT.TRANSPARENT);
		parentKDJ = aParentKDJ;
		
		ddddNF = NumberFormat.getInstance();
		ddddNF.setMaximumIntegerDigits(5);
		ddddNF.setMinimumIntegerDigits(3);
		
		initInterface();
	}

	/**
	 * @param aL
	 */
	public void addPlayListListener(PlayListListener aL) {
		playListListeners.add(aL);
	}

	/**
	 * @param aL
	 */
	public void removePlayListListener(PlayListListener aL) {
		playListListeners.remove(aL);
	}

	/**
	 * 
	 */
	void initInterface() {
		GridLayout aGL = new GridLayout(2, false);
		aGL.horizontalSpacing = 0;
		aGL.verticalSpacing = 0;
		aGL.marginHeight = 0;
		aGL.marginWidth = 0;
		this.setLayout(aGL);
		setBackground(parentKDJ.mainBckC);
//		setRedraw(false);
		
		final PlayList aThis = this;
		listTC = new TimedCanvas(parentKDJ,this,SWT.NONE) {
			@Override
			protected void paintTimed() {
				// TODO Auto-generated method stub
				aThis.paintTimed();
			}
		};
		
		GridData aGD = null;
		
		aGD = new GridData(GridData.FILL,GridData.FILL,true,true);
//		aGD.horizontalAlignment = GridData.FILL;
//		aGD.verticalAlignment = GridData.FILL;
		listTC.setLayoutData(aGD);

		vSB = new KaiScrollBar(parentKDJ,this, SWT.VERTICAL);
		aGD = new GridData(GridData.FILL,GridData.FILL,false,false);
		aGD.widthHint = 10;
		vSB.setLayoutData(aGD);
		vSB.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent evt) {
				setOffsetIndex(vSB.getSelection());
				listTC.needRedraw();
			}
		});
		listTC.addMouseWheelListener(vSB);
		
		hSB = new KaiScrollBar(parentKDJ,this, SWT.HORIZONTAL );
		aGD = new GridData(GridData.FILL,GridData.FILL,false,false);
		aGD.heightHint = 10;
		aGD.horizontalSpan = 2;
		hSB.setLayoutData(aGD);
		hSB.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent evt) {
				setOffsetX(hSB.getSelection());
				listTC.needRedraw();
			}
		});

		listTC.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent aEvt) {
				if (movingColMark) {
					moveColMark(aEvt.x);
					return;
				}

				synchronized (pl) {
					// if (clickedIndex >= 0) {
					if (clickedPlId != null) {
						// Update selected
						SongDescr aDragged = getMp3Descr(aEvt.x, aEvt.y);
						// Search for a move
						// if (clickedIndex != selectedIndex) {
						if (aDragged != null && !aDragged.getPlID().equals(clickedPlId)){//getLastSelectedPlId()) {
							move(clickedPlId,aDragged.getPlID(),false);
						}
					}
				}
			}
		});
		listTC.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent arg0) {
			}

			public void mouseDown(MouseEvent aEvt) {
				if (aEvt.y < lineHeight) {
					movingColMark = true;
					moveColMark(aEvt.x);
					return;
				}
				
				//CTRL & SHIFT are defined in the manager according to specific pl features
				if(aEvt.stateMask == SWT.CTRL || aEvt.stateMask == SWT.SHIFT){
					return;
				}

				// beforeClickedIndex = selectedIndex;
				beforeClickedPlId = getLastSelectedPlId();

				// Just to set selected pos
				final SongDescr aDescr = getMp3Descr(aEvt.x, aEvt.y);

				// clickedIndex = selectedIndex;
				if (aDescr != null) {// clickedIndex >= 0 && clickedIndex <
					// pl.size()) {
					// Set selected list
					clickedPlId = aDescr.getPlID();
					selectedPlIds = new TreeSet();
					selectedPlIds.add(clickedPlId);

					// final SongDescr aDescr =
					// aDescrClicked;//(SongDescr)pl.elementAt(clickedIndex);
					if (aDescr instanceof SongGroup) {
						final String aLabel = aDescr.path;
						parentKDJ.display.asyncExec(new Runnable() {
							public void run() {
								parentKDJ.managerPlay.folderText.setText(aLabel);
							}
						});
					} else {
						// No need to use SWT thread from here. Thread should
						// end fastly, but player load may block (?)
						Thread aTh = new Thread(new Runnable() {
							public void run() {
								parentKDJ.loadPre(aDescr.path);
							}
						}, "TMP (PLAYLIST MOUSE-DOWN)");
						aTh.start();
					}
					if (aEvt.x + offsetX > colWidths[0] - 10 && aEvt.x + offsetX < colWidths[0]) {
						// Should set enable ON/OFF
						int aNewEnable = -aDescr.getEnablePlay();
						int aBeg = getIndex(aDescr.getPlID());// clickedIndex;
						int aEnd = aBeg;
						if (aDescr instanceof SongGroup) {
							if (((SongGroup) aDescr).kind == SongGroup._KIND_OPEN) {
								aEnd = getGroupEnd(aBeg);// selectedIndex);
							}
						}
						for (int s = aBeg; s <= aEnd; s++) {
							SongDescr aDescrTMP = (SongDescr) pl.elementAt(s);
							if (!(aDescrTMP instanceof SongGroup && ((SongGroup) aDescrTMP).kind == SongGroup._KIND_CLOSE)) {
								aDescrTMP.setEnablePlay(aNewEnable);
							}
						}
					}
					listTC.needRedraw();
				}
			}

			public void mouseUp(MouseEvent arg0) {
				movingColMark = false;
				clickedPlId = null;// clickedIndex = -1;
			}
		});
		listTC.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent aPE) {
				plBounds = listTC.getClientArea();
				GC aPlGC = new GC(listTC);
				paintDbl(aPlGC);
				aPlGC.dispose();
//				listTC.needRedraw();
			}
		});
		listTC.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {
				System.out.println("Key=" + arg0.keyCode);
				if (arg0.keyCode == 127 || arg0.keyCode == 8) {
					// Del
					Integer aSelectedPlId = getLastSelectedPlId();
					if (aSelectedPlId != null && !aSelectedPlId.equals(playingPlId)){//selectedIndex >= 0 && playingIndex != selectedIndex) {
						removeSelected();
					}
				}
				if (arg0.keyCode == SWT.ARROW_DOWN) {
					int aSelectedIndex = getIndex(getLastSelectedPlId());
					if (aSelectedIndex >= 0 && aSelectedIndex < pl.size() - 1) {
						aSelectedIndex++;
						selectedPlIds = new TreeSet();
						selectedPlIds.add(((SongDescr)pl.elementAt(aSelectedIndex)).getPlID());
						listTC.needRedraw();
					}
				}
				if (arg0.keyCode == SWT.ARROW_UP) {
					int aSelectedIndex = getIndex(getLastSelectedPlId());
					if (aSelectedIndex > 0 && aSelectedIndex < pl.size()) {
						aSelectedIndex--;
						selectedPlIds = new TreeSet();
						selectedPlIds.add(((SongDescr)pl.elementAt(aSelectedIndex)).getPlID());
						listTC.needRedraw();
					}
				}
			}

			public void keyReleased(KeyEvent arg0) {
			}
		});
		// plGC = new GC(this);
	}

	/**
	 * @param aPlId
	 * @return
	 */
	int getIndex(Integer aPlId) {
		for (int i = 0; i < pl.size(); i++) {
			if (((SongDescr) pl.elementAt(i)).getPlID().equals(aPlId)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * @return
	 */
	Integer getLastSelectedPlId() {
		if (selectedPlIds.size() <= 0) {
			return null;
		}
		return (Integer) selectedPlIds.last();
	}

	/**
	 * @param x
	 * @param y
	 * @return
	 */
	boolean isClickInTopBar(int x, int y) {
		if (y < lineHeight) {
			return true;
		}
		return false;
	}

	/**
	 * @param x
	 */
	void moveColMark(int xClick) {
		int x = xClick + offsetX;
		// Move widh mark
		int aD = 0;
		int aDist;
		int aBestDist = -1;
		int aBestIndex = 0;
		for (int c = 0; c < colWidths.length; c++) {
			aD += colWidths[c];
			aDist = Math.abs(x - aD);
			if (aBestDist < 0 || aBestDist > aDist) {
				aBestDist = aDist;
				aBestIndex = c;
			}
		}
		// Move it
		if (aBestIndex >= 0 && aBestIndex < colWidths.length) {
			aD = 0;
			for (int c = 0; c < aBestIndex; c++) {
				aD += colWidths[c];
			}
			if (aD < x) {
				colWidths[aBestIndex] = x - aD;
			}
			listTC.needRedraw();
		}
	}

	/**
	 * @return
	 */
	public Vector getPl() {
		return pl;
	}

	/**
	 * @param pl
	 */
	public void setPl(Vector pl) {
		this.pl = pl;
		offsetIndex = 0;
		selectedPlIds = new TreeSet();//selectedIndex = -1;
		playingPlId = null;//playingIndex = -1;
		
		//EM 01/06/2008
		rndDone = new TreeMap();
		rndMaxPlay = 0;

		listTC.needRedraw();
	}

	/**
	 * @param aPath
	 */
	public void save(String aPathIn) {
		PlayListLoader.save(parentKDJ, pl, aPathIn);
	}

	/**
	 * @param aPath
	 */
	public void load(String aPath, boolean aClear, int aEnable) {
		SongDescr aDescr;
		if (aClear) {
			for (int s = 0; s < pl.size(); s++) {
				aDescr = (SongDescr) pl.elementAt(s);
				for (int l = 0; l < playListListeners.size(); l++) {
					((PlayListListener) playListListeners.get(l)).removed(aDescr);
				}
			}
			pl.clear();
		}
		Vector aPl = PlayListLoader.load(parentKDJ, aPath);
		for (int s = 0; s < aPl.size(); s++) {
			aDescr = (SongDescr) aPl.elementAt(s);
			if (!(aDescr instanceof SongGroup && ((SongGroup) aDescr).kind == SongGroup._KIND_CLOSE)) {
				aDescr.setEnablePlay(aEnable);
			}
			pl.add(aDescr);
			for (int l = 0; l < playListListeners.size(); l++) {
				((PlayListListener) playListListeners.get(l)).added(aDescr);
			}
		}
	}

	/**
	 * @param aDescr
	 */
	public void updatedDescr(SongDescr aDescr) {
		for (int d = 0; d < pl.size(); d++) {
			SongDescr aOld = (SongDescr) pl.elementAt(d);
			if (aOld.id.equals(aDescr.id)) {
				pl.set(d, aDescr);
				listTC.needRedrawSlow();
			}
		}
	}

	/**
	 * @param aPlIdFrom
	 * @param aPlIdTo (if -1, move to end)
	 * @param aRelativePos
	 * @return last SongDescr
	 */
	SongDescr move(Integer aPlIdFrom,Integer aPlIdTo,boolean aRelativePos) {
		synchronized (pl) {
			int aStart = getIndex(aPlIdFrom);//clickedIndex;
			int aEnd = aStart;//clickedIndex;
			int aDest = getIndex(aPlIdTo);//selectedIndex;
			if(aDest < 0){
				aDest = pl.size();
			}
			Object aStartSD = pl.elementAt(aStart);
			
			//Prepare start/end according to groups
			if (aStartSD instanceof SongGroup) {
				// Search for a multiple move
				if (((SongGroup) aStartSD).kind == SongGroup._KIND_OPEN) {
					// Search for corresponding end
					if ((aEnd = getGroupEnd(aStart)) < 0) {
						aEnd = aStart;
					}
				} else if (((SongGroup) aStartSD).kind == SongGroup._KIND_CLOSE) {
					if (aDest < aStart) {
						// Search for a possible folder limits to push
						Object aObject;
						while (aStart >= 1 && (aObject = pl.elementAt(aStart - 1)) != null && aObject instanceof SongGroup) {
							aStart--;
							if (aStart <= aDest) {
								aDest--;
							}
							// System.out.println("New Start
							// : " + aStart);
						}
					}
					if (aDest > aStart) {
						// Search for folder limits to push
						Object aObject;
						while (aEnd <= pl.size() - 2 && (aObject = pl.elementAt(aEnd + 1)) != null && aObject instanceof SongGroup) {
							aEnd++;
						}
					}
				}
			}
			
			// Check validity
			boolean aUp = aStart > aDest;
			if ((aUp && (aDest < 0 || aStart < 0 || aEnd < 0)) || (!aUp && (aDest > pl.size() || aStart >= pl.size() - 1 || aEnd >= pl.size() - 1))) {
				// Out of the PL. No move, restore selected to clicked to avoid
				// bad move effect
				//selectedIndex = clickedIndex;
				//needRedraw();
				return null;//selectedIndex;
			}
			// Remove / Store
//			Object aPlaying = null;
//			if (playingPlId >= 0){//playingIndex >= 0) {
//				aPlaying = pl.elementAt(getIndex(playingPlId));//playingIndex);
//			}
			
			// Move entry
			Vector aStored = new Vector();
			for (int i = 0; i <= aEnd - aStart; i++) {
				aStored.add(pl.remove(aStart));
				// System.out.println("Removed : " + aStart);
				if (aRelativePos && aDest > aStart + 1) {
					// Position is relative to an existing song, should update
					// dest
					aDest--;
				}
			}
			if (aDest >= pl.size()) {
				// End of pl
				aDest = pl.size();
			}
			for (int i = 0; i <= aEnd - aStart; i++) {
				pl.add(aDest + i, aStored.elementAt(i));
				// System.out.println("Added : " + (aDest + i));
			}
			// Restore
//			if (playingIndex >= 0) {
//				playingIndex = pl.indexOf(aPlaying);
//			}
			//clickedIndex = aDest;// selectedIndex;
			listTC.needRedraw();
			return (SongDescr)aStored.elementAt(aStored.size() - 1);//aDest + (aEnd - aStart);
		}
	}

	/**
	 * 
	 */
	void removeSelected() {
		//For the moment, remove only one selected pos (multi-select not enabled)
		Integer aPlId = getLastSelectedPlId();
		if(aPlId == null){
			//Nothing to do
			return;
		}
		int aIndex = getIndex(aPlId);
		if (aIndex < 0 || aIndex >= pl.size()) {
			return;
		}
		SongDescr aMD = (SongDescr) pl.get(aIndex);
		int aEnd = -1;
		if (aMD instanceof SongGroup) {
			if (((SongGroup) aMD).kind == SongGroup._KIND_OPEN) {
				aEnd = getGroupEnd(aIndex);
			} else {
				// Don't remove end of folder
				return;
			}
		}
		SongDescr aDescr = (SongDescr) pl.remove(aIndex);
		for (int l = 0; l < playListListeners.size(); l++) {
			((PlayListListener) playListListeners.get(l)).removed(aDescr);
		}
		
		//Set selection
		selectedPlIds = new TreeSet();
		if (pl.size() <= 0) {
			playingPlId = null;
		}
		else if(aIndex >= pl.size()){
			aIndex = pl.size() - 1;
		}
		if(aIndex >= 0 && aIndex < pl.size()){
			selectedPlIds.add(((SongDescr)pl.elementAt(aIndex)).getPlID());
		}
		
		//Possibly remove end of group
		if (aEnd >= 0) {
			aEnd--;
			pl.remove(aEnd);
			if (pl.size() <= 0) {
				playingPlId = null;
			}
		}
		listTC.needRedraw();
	}

	/**
	 * @param aDescr
	 */
	public void addSong(SongDescr aDescr) {
		SongDescr aNewDescr = new SongDescr(aDescr);
		aNewDescr.setEnablePlay(1);
		pl.add(aNewDescr);
		listTC.needRedraw();
	}

	/**
	 * @param aDescr
	 */
	public void addSongAfter(SongDescr aDescr) {
		int aPos = -1;
		Integer aSelectedPlId = getLastSelectedPlId();
		if (aSelectedPlId != null) {
			aPos = getIndex(aSelectedPlId);
		} else if (playingPlId != null) {
			aPos = getIndex(playingPlId);
		}
		SongDescr aNewDescr = new SongDescr(aDescr);
		aNewDescr.setEnablePlay(1);
		if (aPos >= 0) {
			// Ok, add after
			pl.add(aPos + 1, aNewDescr);
		} else {
			// Add at end
			pl.add(aNewDescr);
		}
		selectedPlIds = new TreeSet();//
		selectedPlIds.add(aNewDescr.getPlID());
		listTC.needRedraw();
	}

	/**
	 * @param aDest
	 * @return last SongDescr
	 */
	public SongDescr moveAt(int aDest) {
		int aPos = -1;
		if (aDest >= 0) {
			aPos = aDest;
		} else if (playingPlId != null) {
			aPos = getIndex(playingPlId);
		}
		if (aPos < 0) {
			// Do nothing
			return null;
		}
		Integer aDestPlId = null;
		if(aPos < pl.size()){
			aDestPlId = ((SongDescr)pl.elementAt(aPos)).getPlID();
		}
		return move(clickedPlId,aDestPlId,true);
	}

	/**
	 * 
	 */
	public void addGroup(String aLabel) {
		if (pl.size() <= 0) {
			return;
		}
		int aSelectedIndex = getIndex(getLastSelectedPlId()); 
		if (aSelectedIndex < 0) {
			aSelectedIndex = 0;
		}
		
		// Jump over already existing folders
		while (aSelectedIndex < pl.size() && pl.elementAt(aSelectedIndex) instanceof SongGroup) {
			aSelectedIndex++;
		}
		// Add
		SongDescr aDescr;
		pl.add(aSelectedIndex, aDescr = new SongGroup(SongGroup._KIND_OPEN, aLabel));
		aDescr.setEnablePlay(1);
		if (aSelectedIndex < pl.size()) {
			pl.add(aSelectedIndex + 2, new SongGroup(SongGroup._KIND_CLOSE, aLabel));
		} else {
			pl.add(aSelectedIndex + 1, new SongGroup(SongGroup._KIND_CLOSE, aLabel));
		}
		listTC.needRedraw();
	}

	/**
	 * @param aIndex
	 * @return
	 */
	int getGroupEnd(int aIndex) {
		int aInclude = 0;
		for (int i = aIndex; i < pl.size(); i++) {
			Object aEntry = pl.elementAt(i);
			if (aEntry instanceof SongGroup) {
				if (((SongGroup) aEntry).kind == SongGroup._KIND_OPEN) {
					aInclude++;
				} else {
					aInclude--;
					if (aInclude <= 0) {
						return i;
					}
				}
			}
		}
		return -1;
	}

	/**
	 * @param aLabel
	 */
	void setGroupLabel(String aLabel) {
		int aSelectedIndex = getIndex(getLastSelectedPlId()); 
		if (aSelectedIndex < 0) {
			return;
		}
		SongDescr aDescr = (SongDescr) pl.elementAt(aSelectedIndex);
		if (aDescr instanceof SongGroup) {
			aDescr.path = aLabel;
			if (((SongGroup) aDescr).kind == SongGroup._KIND_OPEN) {
				aDescr = (SongDescr) pl.elementAt(getGroupEnd(aSelectedIndex));
				aDescr.path = aLabel;
			}
		}
		listTC.needRedraw();
	}
	
	/**
	 * @return
	 */
//	SongDescr getFirstPlayable() {
//		return getFirstPlayable(playingPlId);
//		
//	}
	
	SongDescr getFirstPlayable(Integer aPlId) {
		
		if(pl.size() <= 0){
			return null;
		}
		int aPlayingIndex = getIndex(aPlId); 
		if (aPlayingIndex < 0) {
			return null;
		}
		SongDescr aDescr;
		while (aPlayingIndex < pl.size() && ((aDescr = (SongDescr) pl.elementAt(aPlayingIndex)) instanceof SongGroup 
				|| aDescr.getEnablePlay() < 0
				//EM 28/04/2009
				|| !aDescr.isAccesible())) {
			aPlayingIndex++;
		}
		if(aPlayingIndex < pl.size()){
			return (SongDescr) pl.elementAt(aPlayingIndex);
		}
		return null;
	}

	/**
	 * @return
	 */
	public SongDescr nextPlayable() {
		if (pl.size() <= 0) {
			return null;
		}
		int aPlayingIndex = getIndex(playingPlId); 
		if (aPlayingIndex < 0) {
			aPlayingIndex = -1;
		}
		aPlayingIndex++;
		if(aPlayingIndex > pl.size() - 1){
			aPlayingIndex = 0;
		}
		SongDescr aDescr =  (SongDescr) pl.elementAt(aPlayingIndex);
		aDescr = getFirstPlayable(aDescr.getPlID());
		if(aDescr != null){
			playingPlId = aDescr.getPlID();
			listTC.needRedraw();
			return aDescr;
		}
		playingPlId = null;
		listTC.needRedraw();
		return (SongDescr)pl.elementAt(0);
	}

	/**
	 * @param aDescr
	 */
	public void select(SongDescr aDescr){
		selectedPlIds = new TreeSet();
		selectedPlIds.add(aDescr.getPlID());
	}


	/**
	 * @return
	 */
	public SongDescr getSelected() {
		int aIndex = getIndex(getLastSelectedPlId());
		if (aIndex >= 0 && aIndex < pl.size()) {
			return (SongDescr) pl.elementAt(aIndex);
		}
		return null;
	}

	/**
	 * 
	 */
	public void setSelectedPlaying() {
		playingPlId = getLastSelectedPlId();
		listTC.needRedraw();
	}

	/**
	 * @param aX
	 * @param aY
	 * @return
	 */
	String getPath(int aX, int aY) {
		SongDescr aDescr = getMp3Descr(aX, aY);
		if (aDescr == null) {
			return null;
		}
		return aDescr.path;
	}

	/**
	 * @param aX
	 * @param aY
	 * @return
	 */
	SongDescr getMp3Descr(int aX, int aY) {
		synchronized (pl) {
			if (aY < lineHeight) {
				return null;
			}
			Rectangle aBounds = this.getClientArea();
			int aPosY = aBounds.y;
			aPosY += 2 * lineHeight;
			for (int i = offsetIndex; i < pl.size() && aPosY < aBounds.height; i++) {
				SongDescr aDescr = (SongDescr) pl.elementAt(i);
				aPosY += lineHeight;
				if (aPosY >= aY) {
					return aDescr;
				}
			}
			return null;
		}
	}

	/**
	 * @return
	 */
	public SongDescr getRandomPlaying() {
		if (pl.size() <= 0) {
			return null;
		}
		SongDescr aDescr = null;
		//EM 01/06/2008 : intelligent rand  
		boolean aFound = false;
		while(!aFound && rndMaxPlay < 100){
			aFound = false;
			for(int aTry = 0;aTry < 100;aTry++){
				//Change playing randomly
				int aIndex = (int) (Math.random() * pl.size());
				aDescr = getFirstPlayable(((SongDescr)pl.elementAt(aIndex)).getPlID());
				playingPlId = aDescr.getPlID();
				Integer aCount = (Integer)rndDone.get(aDescr.id);
				if(aCount == null || aCount.intValue() <= rndMaxPlay){
					//Ok
					if(aCount == null){
						rndDone.put(aDescr.getId(),new Integer(1));
					}
					else{
						rndDone.put(aDescr.getId(),new Integer(aCount.intValue() + 1));
					}
					aFound = true;
					break;
				}
				System.out.println("'"+ aDescr.author + " / " + aDescr.title + "' Already played " + aCount + " time(s), trying to randomize again");
			}
			if(!aFound){
				//Not found, be more tolerant
				rndMaxPlay++;
			}
		}
		listTC.needRedraw();
		return aDescr;
	}

	/**
	 * @return
	 */
	public int getOffsetIndex() {
		return offsetIndex;
	}

	/**
	 * @param indexOffset
	 */
	public void setOffsetIndex(int indexOffset) {
		offsetIndex = indexOffset;
	}

	/**
	 * @param aOffset
	 */
	public void setOffsetX(int aOffset) {
		offsetX = aOffset;
	}

	@Override
	public void drawBackground(GC gc, int x, int y, int width, int height, int offsetX, int offsetY) {
	}

	/**
	 * 
	 */
	protected void paintTimed() {
		if(KaiDJ._DEBUG_PAINT) {
			System.out.println("PLAYLIST.paintTimed()");
		}
		if (plBounds == null) {
			return;
		}
		synchronized (this) {
			try {
				if (parentKDJ == null) {
					return;
				}
				// Calculate all durations
				Vector aDurations = new Vector();
				aDurations.add(new Long(0));
				long aCurrentDuration = -1;
				//EM 18/05/2008 : search double songs
				TreeSet aViewed = new TreeSet();

				Rectangle aRect = listTC.getBounds();
				Rectangle aRectDbl = null;
				if (dblBuf != null) {
					aRectDbl = dblBuf.getBounds();
				}
				if (dblBuf == null || aRect.width != aRectDbl.width || aRect.height != aRectDbl.height) {
					if (dblBuf != null) {
						dblBuf.dispose();
						dblBuf = null;
						dblBufGC.dispose();
					}
					dblBuf = new Image(parentKDJ.display, aRect);
					dblBufGC = new GC(dblBuf);
				}

				GC aGC = dblBufGC;
				// Background
				aGC.setClipping(0, 0, plBounds.width, plBounds.height);
				Color aBck = parentKDJ.mainBckC;
				aGC.setBackground(aBck);
				aGC.setForeground(parentKDJ.playerBarC);
				aGC.fillRectangle(0, 0, plBounds.width, plBounds.height);
				// Draw widths selectors
				int aD = -offsetX;
				aGC.setBackground(parentKDJ.playerBarC);
				aGC.setForeground(parentKDJ.playerC);
				aGC.fillRectangle(0, lineHeight / 3, plBounds.width, lineHeight / 3);
				for (int c = 0; c < colWidths.length; c++) {
					aD += colWidths[c];
					aGC.drawLine(aD, 0, aD, lineHeight);
				}
				int aPosX = 5 - offsetX;
				int aPosY = 0;
				aPosY += lineHeight;
				// Main font, draw all
				aGC.setClipping(5, 5, plBounds.width - 10, plBounds.height - 10);
				aGC.setBackground(aBck);
				aGC.setFont(parentKDJ.initialFont);
				aPosY += lineHeight;
				int aTab = 0;
				SongDescr aDescr;
				int aCount = 0;
				boolean aIsFolder, aIsOpen = false;
				aGC.setForeground(parentKDJ.whiteC);
				aGC.setLineWidth(1);
				for (int i = 0; i < pl.size() /* && aPosY < posY + posH */; i++) {
					aDescr = ((SongDescr) pl.elementAt(i));
					Integer aPlId = aDescr.getPlID();
					//EM 28/04/2009
					String aPath = aDescr.getPath();
					boolean aAccessible = aDescr.isAccesible();
					//EM 18/05/2008
					boolean aAlready = aViewed.contains(aDescr.getId());
					aIsFolder = aDescr instanceof SongGroup;
					if (aIsFolder) {
						aIsOpen = (((SongGroup) aDescr).kind == SongGroup._KIND_OPEN);
						if (((SongGroup) aDescr).kind == SongGroup._KIND_CLOSE) {
							aTab--;
						}
						// Update duration set
						if (aIsOpen) {
							// New duration
							aDurations.add(0, new Long(0));
						} else {
							// Remove duration
							if (aDurations.size() > 0) {// Should never be <=0,
								// just a careful test
								// in case something
								// wrong occur
								aCurrentDuration = ((Long) aDurations.remove(0)).longValue();
							}
							if (aDurations.size() > 0) {// Should never be <=0,
								// just a careful test
								// in case something
								// wrong occur
								aDurations.setElementAt(new Long(((Long) aDurations.elementAt(0)).longValue() + aCurrentDuration), 0);
							}
						}
					} else {
						aCount++;
						// Update current group duration
						if (aDurations.size() > 0) {// Should never be <=0, just
							// a careful test in case
							// something wrong occur
							aDurations.setElementAt(new Long(((Long) aDurations.elementAt(0)).longValue() + aDescr.getDuration()), 0);
						}
					}
					if (i >= offsetIndex && aPosY + lineHeight < plBounds.height) {
						// In display zone
						// Select colors
						if (selectedPlIds.contains(aPlId)) {
							aGC.setBackground(parentKDJ.selectedC);
							aGC.setForeground(parentKDJ.whiteC);
							aGC.fillRectangle(aPosX, aPosY, plBounds.width - 10, lineHeight);
						}
						if (aPlId.equals(playingPlId)) {
							aGC.setForeground(parentKDJ.playerC);
						}
						if(!aAccessible){
							aGC.setForeground(parentKDJ.redC);
						}
						// Draw text
						if (!aIsFolder) {
							// Draw Mp3Descr
							try {
								aD = 0;
								// Num
								aGC.drawText(ddddNF.format(aCount), aPosX, aPosY);
								aD += colWidths[0];
								// Time
								aGC.drawText(aDescr.getDurationDisplay(), aD + aPosX + (aTab * 30), aPosY);
								aD += colWidths[1];
								// Author
								aGC.setClipping(aD + aPosX + (aTab * 30), aPosY, colWidths[2] - 5, lineHeight);
								aGC.drawText(aDescr.getAuthor(), aD + aPosX + (aTab * 30), aPosY);
								aD += colWidths[2];
								// Title
								aGC.setClipping(aD + aPosX + (aTab * 30), aPosY, colWidths[3] - 5, lineHeight);
								aGC.drawText(aDescr.getTitle(), aD + aPosX + (aTab * 30), aPosY);
								aD += colWidths[3];
								// Path
								aGC.setClipping(aD + aPosX + (aTab * 30), aPosY, colWidths[4] - 5, lineHeight);
								aGC.drawText(aPath, aD + aPosX + (aTab * 30), aPosY);
								aD += colWidths[4];
								// Restore clipping
								aGC.setClipping(5, 5, plBounds.width - 10, plBounds.height - 10);
							} catch (Throwable t) {
								// Something wrong in values ? due concurrent
								// updates ?
							}
						} else {
							// Group
							aGC.setForeground(parentKDJ.playerBarC);
							aD = colWidths[0];
							if (aIsOpen) {
								aGC.drawLine(aD + aPosX + (aTab * 30), aPosY + 7, aD + aPosX + ((aTab + 1) * 30), aPosY + 7);
								aGC.drawLine(aD + aPosX + ((aTab + 1) * 30) - 10, aPosY + 5, aD + aPosX + ((aTab + 1) * 30), aPosY + 7);
								aGC.drawLine(aD + aPosX + ((aTab + 1) * 30) - 10, aPosY + 9, aD + aPosX + ((aTab + 1) * 30), aPosY + 7);
								aGC.drawLine(aD + aPosX + ((aTab + 1) * 30) - 10, aPosY + 5, aD + aPosX + ((aTab + 1) * 30) - 10, aPosY + 9);
								aGC.drawText(aDescr.getPath(), aD + aPosX + ((aTab + 1) * 30), aPosY);
							} else {
								aGC.drawLine(aD + aPosX + ((aTab + 1) * 30), aPosY + 3, aD + aPosX + ((aTab + 1) * 30) - 10, aPosY + 7);
								aGC.drawLine(aD + aPosX + ((aTab + 1) * 30), aPosY + 12, aD + aPosX + ((aTab + 1) * 30) - 10, aPosY + 7);
								aGC.drawText("[" + SongDescr.durationToDisplay(aCurrentDuration) + "]", aD + aPosX + ((aTab + 1) * 30), aPosY);
								aGC.drawText(aDescr.getPath(), aD + aPosX + 50 + ((aTab + 1) * 30), aPosY);
							}
						}
						// Draw folders
						if (aTab > 0) {
							aGC.setForeground(parentKDJ.playerBarC);
							aD = colWidths[0] + 25;
							for (int j = 0; j < aTab; j++) {
								aGC.drawLine(aD + aPosX + (j * 30), aPosY - lineHeight, aD + aPosX + (j * 30), aPosY + lineHeight);
							}
						}
						// Draw play choice
						int aEnablePlay = aDescr.getEnablePlay();
						switch (aEnablePlay) {
						case -1:// Don't play
							aGC.setBackground(parentKDJ.redC);
							break;
						case 1:// Play
							//EM 18/05/2008
							if(!aIsFolder && aAlready){
								aGC.setBackground(parentKDJ.yellowC);
							}
							else{
								aGC.setBackground(parentKDJ.playerC);
							}
							break;
						case 0:
						default:
							// Nothing
							break;
						}
						if (aEnablePlay != 0) {
							aGC.fillOval(colWidths[0] - 9 - offsetX, aPosY + 1, 8, 8);
						}
						// Set back normal colors
						if (aPlId == playingPlId || selectedPlIds.contains(aPlId) || aIsFolder || aTab > 0 || aEnablePlay != 0) {
							aGC.setBackground(aBck);
							aGC.setForeground(parentKDJ.whiteC);
						}
						// Down
						aPosY += lineHeight;
					}
					if (aIsFolder) {
						if (((SongGroup) aDescr).kind == SongGroup._KIND_OPEN) {
							aTab++;
						}
					}
					//EM 26/05/2008 : store
					if (!aIsFolder){
						aViewed.add(aDescr.getId());
					}

				}
				// Nb
				aPosX = 5;
				aPosY = lineHeight;
				aGC.setForeground(parentKDJ.playerC);
				long aTotalDuration = 0;
				if (aDurations.size() > 0) {// Should never be <=0, just a
					// careful test in case something
					// wrong occur
					aTotalDuration = ((Long) aDurations.remove(0)).longValue();
				}
				aGC.drawText("Nb : " + aCount + " [" + SongDescr.durationToDisplay(aTotalDuration) + "]", aPosX, aPosY, true);
				parentKDJ.display.asyncExec(new Runnable() {
					public void run() {
						int aD = 0;
						for (int c = 0; c < colWidths.length; c++) {
							aD += colWidths[c];
						}
						aD += colWidths[colWidths.length - 1] / 2;
//						ScrollBar aSB = null;
//						aSB = getHorizontalBar(); 
						if(offsetX != hSB.getSelection()
								|| aD != hSB.getMaximum()
//								|| plBounds.width != aSB.getThumb()
								) {
							hSB.setSelection(offsetX);
							hSB.setMaximum(aD);
							hSB.setThumb(plBounds.width);
							hSB.redraw();
//							aSB.setValues(offsetX, 0, aD, plBounds.width, plBounds.width / 10, 8 * plBounds.width / 10);
						}
//						aSB = getVerticalBar();
						if(offsetIndex != vSB.getSelection()
								|| pl.size() + (plBounds.height / 2) / lineHeight != vSB.getMaximum()
//								|| plBounds.height / lineHeight != aSB.getThumb()
								) {
							vSB.setSelection(offsetIndex);
							vSB.setMaximum(pl.size() + (plBounds.height / 2) / lineHeight);
							vSB.setThumb(plBounds.height / lineHeight);
							vSB.redraw();
//							aSB.setValues(offsetIndex, 0, pl.size() + (plBounds.height / 2) / lineHeight, plBounds.height / lineHeight, 1, 8 * plBounds.height / (lineHeight * 10));
						}
					}
				});
				// Borders
				aGC.setClipping(plBounds.x, plBounds.y, plBounds.width, plBounds.height);
				aGC.setLineWidth(2);
				aGC.drawRectangle(plBounds.x, plBounds.y, plBounds.width, plBounds.height);
				
				// Draw final image
				GC aPlGC = new GC(listTC);
				paintDbl(aPlGC);
				aPlGC.dispose();
			} catch (Throwable t) {
				System.err.println("Can't paint : " + t);
				t.printStackTrace(System.err);
			}
		}
	}
	
	void paintDbl(GC aPlGC) {
		if(dblBuf == null) {
			return;
		}
		aPlGC.drawImage(dblBuf, 0, 0);
	}
}
