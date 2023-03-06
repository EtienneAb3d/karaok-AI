package com.cubaix.kaiDJ;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
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

public class PlayManager extends Composite {
	KaiDJ parentJDJ;
	KaiButton addFolderB;
	Text folderText;
	PlayList pl;
	public PlayManager(KaiDJ aParentJDJ,Composite parent, int style) {
		super(parent, style);
		parentJDJ = aParentJDJ;
		
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
		setBackground(parentJDJ.mainBckC);

		//Buttons
		Composite aButPanel = new Composite(this,SWT.NULL);
		aButPanel.setBackground(parentJDJ.mainBckC);
		aGL = new GridLayout();
		//aGL.horizontalSpacing = 4;
		aGL.verticalSpacing = 0;
		aGL.marginHeight = 0;
		aGL.marginWidth = 0;
		aGL.numColumns = 2;
		aButPanel.setLayout(aGL);
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aButPanel.setLayoutData(aGD);

		addFolderB = new KaiButton(parentJDJ,aButPanel, SWT.NULL);
		addFolderB.setBackground(parentJDJ.mainBckC);
		addFolderB.setToolTipText("Add named group");
		addFolderB.setImagePath("img2/addGroupBut.png");
		aGD = new GridData();// GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		addFolderB.setLayoutData(aGD);
		addFolderB.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				pl.addGroup(folderText.getText());
			}
		});
		// Group name Text
        String os = System.getProperty("os.name");
		folderText = new Text(aButPanel, SWT.BORDER);
		folderText.setToolTipText("Edit group name");
        if(!os.toLowerCase().startsWith("mac")){
        	//Mac seems to not support properly background colors here
        	folderText.setBackground(parentJDJ.mainBckC);
        	folderText.setForeground(parentJDJ.whiteC);
        }
		aGD = new GridData(GridData.FILL_BOTH);
		// aGD.heightHint = 20;
		folderText.setLayoutData(aGD);
		folderText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {
			}

			public void keyReleased(KeyEvent arg0) {
				int aCharCode = arg0.character & 0xFF;
				if (aCharCode > 32 || arg0.keyCode == 8 || (arg0.keyCode >= 32 && arg0.keyCode < 65535)) {
					pl.setGroupLabel(folderText.getText());
				}
			}
		});
		
		//PL
		pl = new PlayList(aParentJDJ, this, SWT.NONE);
		pl.listTC.setToolTip("Edit playlist: \n"//
			+ "- Click to load a song on the pre-listen player\n"//
			+ "- Double-click to play and mix a song\n"//
			+ "- Click&Drag to move a song or a group, or resize a group\n"//
			+ "- Ctrl+Click to move a song or a group after the current selection or the current playing song\n"//
			+ "- Suppr key to remove a song or a group");
		aGD = new GridData(GridData.FILL_BOTH);
		pl.setLayoutData(aGD);
		
		//Listeners
		pl.listTC.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent aEvt) {
				if (pl.isClickInTopBar(aEvt.x, aEvt.y)) {
					// Nothing to do
				} else {
					final SongDescr aDescr = pl.getMp3Descr(aEvt.x, aEvt.y);
					//No need to use SWT thread from here. Thread should end fastly, but player load may block (?)
					Thread aTh = new Thread(new Runnable(){
						public void run() {
							SongDescr aNewDescr; 
							if (aDescr == null || (aNewDescr =  pl.getFirstPlayable(aDescr.getPlID())) == null) {
								// Nothing detected, try to get something new
								parentJDJ.timeToNext(null);
								return;
							}
							pl.select(aNewDescr);
							if (parentJDJ.player1.playState <= 0 && parentJDJ.player2.playState <= 0) {
								// First play
								pl.setSelectedPlaying();
								parentJDJ.player1.currentGain = 0.8;
								parentJDJ.player1.load(aNewDescr.path);
								parentJDJ.player1.play();
								parentJDJ.player2.currentGain = 0;
								parentJDJ.player2.fadeOut();
							} else if (parentJDJ.player1.playState <= 0 || parentJDJ.player1.playState == 2) {
								// Cross fade
								pl.setSelectedPlaying();
								parentJDJ.player1.load(aNewDescr.path);
								parentJDJ.player1.play();
								parentJDJ.player1.fadeIn();
								parentJDJ.player2.fadeOut();
							} else if (parentJDJ.player2.playState <= 0 || parentJDJ.player2.playState == 2) {
								// Cross fade
								pl.setSelectedPlaying();
								parentJDJ.player2.load(aNewDescr.path);
								parentJDJ.player2.play();
								parentJDJ.player2.fadeIn();
								parentJDJ.player1.fadeOut();
							}
						}
					},"TMP (MANAGER PLAYER DBL-CLICK)");
					aTh.start();
				}
			}

			public void mouseDown(MouseEvent aEvt) {
				if(aEvt.stateMask == SWT.CTRL){
					//Add after
					if (pl.isClickInTopBar(aEvt.x, aEvt.y)) {
						// Nothing to do
					} else {
						Integer aSelPlId = pl.getLastSelectedPlId();
						SongDescr aDescr = pl.getMp3Descr(aEvt.x, aEvt.y);
						if (aDescr != null) {
							pl.clickedPlId = aDescr.getPlID();
							//Move song/group to playlist
							SongDescr aLastDescr = pl.moveAt(pl.getIndex(aSelPlId) + 1);
							if(aLastDescr != null){
								pl.select(aLastDescr);
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
	 * @param aDescr
	 */
	public void addSong(SongDescr aDescr){
		pl.addSong(aDescr);
	}
	
	/**
	 * @param aDescr
	 */
	public void addSongAfter(SongDescr aDescr){
		pl.addSongAfter(aDescr);
	}

	
	/**
	 * @param aPath
	 * @param aClear
	 */
	public void load(String aPath, boolean aClear) {
		pl.load(aPath, aClear, 1);
	}

}
