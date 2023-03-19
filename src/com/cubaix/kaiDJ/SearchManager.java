package com.cubaix.kaiDJ;

import java.util.TreeSet;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.cubaix.kaiDJ.db.SongDescr;
import com.cubaix.kaiDJ.swt.KaiButton;

public class SearchManager extends Composite {
	KaiDJ parentKDJ;
	KaiButton shuffleAllB;
	boolean randomAllFlag = false;
	Text searchText;
	boolean mustSearch = false;
	String mustSearchText = null;
	public PlayList pl;

	public SearchManager(KaiDJ aParentKDJ,Composite parent, int style) {
		super(parent, style);
		parentKDJ = aParentKDJ;

		//Main panel
		GridLayout aGL = new GridLayout();
		aGL.horizontalSpacing = 0;
		//aGL.verticalSpacing = 4;
		aGL.marginHeight = 0;
		aGL.marginWidth = 0;
		aGL.numColumns = 1;
		this.setLayout(aGL);
		GridData aGD = new GridData(GridData.FILL_BOTH);
		this.setLayoutData(aGD);
		setBackground(parentKDJ.mainBckC);
		
		//Buttons
		Composite aButPanel = new Composite(this,SWT.NULL);
		aButPanel.setBackground(parentKDJ.mainBckC);
		aGL = new GridLayout();
		//aGL.horizontalSpacing = 4;
		aGL.verticalSpacing = 0;
		aGL.marginHeight = 0;
		aGL.marginWidth = 0;
		aGL.numColumns = 2;
		aButPanel.setLayout(aGL);
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aButPanel.setLayoutData(aGD);

		shuffleAllB = new KaiButton(parentKDJ,aButPanel, SWT.TOGGLE);
		shuffleAllB.setBackground(parentKDJ.mainBckC);
		shuffleAllB.setToolTipText("Shuffle selection");
		shuffleAllB.setImagePath("img2/shuffleBut.png");
		shuffleAllB.setText("Shuffle");
		aGD = new GridData();// GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		shuffleAllB.setLayoutData(aGD);
		shuffleAllB.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				randomAllFlag = shuffleAllB.getSelection();
			}
		});
		// Search Text
		searchText = new Text(aButPanel, SWT.BORDER);
		searchText.setToolTipText("Enter keywords");
        String os = System.getProperty("os.name");
        if(!os.toLowerCase().startsWith("mac")){
        	//Mac seems to not support properly background colors here
        	searchText.setBackground(parentKDJ.mainBckC);
        	searchText.setForeground(parentKDJ.whiteC);
        }
		aGD = new GridData(GridData.FILL_BOTH);
		searchText.setLayoutData(aGD);
		searchText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				mustSearch = true;
				parentKDJ.db.breakSearch = true;
				doSearch();
			}
		});
//		searchText.addKeyListener(new KeyListener() {
//			public void keyPressed(KeyEvent arg0) {
//			}
//
//			public void keyReleased(KeyEvent arg0) {
//				int aCharCode = arg0.character & 0xFF;
//				if (aCharCode > 32 || arg0.keyCode == 8 || (arg0.keyCode >= 32 && arg0.keyCode < 65535)) {
//					mustSearch = true;
//					parentKDJ.db.breakSearch = true;
//					doSearch();
//				}
//			}
//		});
		
		//PL
		pl = new PlayList(parentKDJ, this, SWT.NONE);
		pl.listTC.setToolTip("Browse songs: \n"//
			+ "- Click to load a song on the pre-listen player\n"//
			+ "- Double-click to add a song at the end of the playlist or the current category\n"//
			+ "- Ctrl+Click to add a song after the current selection or the current playing song\n"//
			+ "- Shift+Click to add a range of songs after the current selection or the current playing song");
		aGD = new GridData(GridData.FILL_BOTH);
		pl.setLayoutData(aGD);
		
		//Listeners
		pl.listTC.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent aEvt) {
				if (pl.isClickInTopBar(aEvt.x, aEvt.y)) {
					// Nothing to do
				} else {
					SongDescr aDescr = pl.getMp3Descr(aEvt.x, aEvt.y);
					if (aDescr != null) {
						if(parentKDJ.managerCategories != null){
							//Add song to category
							parentKDJ.managerCategories.addSong(aDescr);
						}
						else{
							//Add song to playlist
							parentKDJ.managerPlay.addSong(aDescr);
						}
					}
				}
			}

			public void mouseDown(MouseEvent aEvt) {
				if (pl.isClickInTopBar(aEvt.x, aEvt.y)) {
					// Nothing to do
				} else {
					if(aEvt.stateMask == SWT.CTRL){
						//Add after
						SongDescr aDescr = pl.getMp3Descr(aEvt.x, aEvt.y);
						if (aDescr != null) {
							pl.selectedPlIds = new TreeSet();
							pl.selectedPlIds.add(aDescr.getPlID());
							parentKDJ.managerPlay.addSongAfter(aDescr);
							pl.listTC.needRedraw();
						}
					}
					else if(aEvt.stateMask == SWT.SHIFT){
						//Add after
						Integer aBefPlId = pl.getLastSelectedPlId();
						SongDescr aDescr = pl.getMp3Descr(aEvt.x, aEvt.y);
						if (aDescr != null) {
							int aBefIndex = pl.getIndex(aBefPlId);
							int aIndex = pl.getIndex(aDescr.getPlID());
							if(aBefIndex >= 0 && aIndex >= 0 && aBefIndex <= aIndex){
								pl.selectedPlIds = new TreeSet();
								for(int i = aBefIndex;i <= aIndex;i++){
									aDescr = (SongDescr)pl.pl.elementAt(i);
									pl.selectedPlIds.add(aDescr.getPlID());
									//Add song to playlist
									parentKDJ.managerPlay.addSongAfter(aDescr);
								}
								pl.listTC.needRedraw();
							}
						}
					}
				}
			}

			public void mouseUp(MouseEvent arg0) {
			}
		});
	}

	/**
	 * 
	 */
	public void doSearch(){
		// Start main refresh thread
		Thread aThread = new Thread(new Runnable(){
			public void run() {
				try {
					/*while (true)*/ {
						if (mustSearch) {
							parentKDJ.shell.getDisplay().syncExec(new Runnable() {
								public void run() {
									mustSearchText = searchText.getText();
								}
							});
							mustSearch = false;
							Vector aRes = new Vector();
							pl.setPl(aRes);
							parentKDJ.db.search(aRes, mustSearchText);
							pl.listTC.needRedraw();
						}
						//Thread.sleep(100);
					}
				} catch (Throwable t) {
					System.err.println("Main loop error : " + t);
					t.printStackTrace(System.err);
				}
			}
			
		}, "Search request");
		aThread.setPriority(Thread.MIN_PRIORITY);
		aThread.start();
	}
}
