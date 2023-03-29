package com.cubaix.kaiDJ;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.cubaix.kai.KaiClient;
import com.cubaix.kai.KaiEditor;
import com.cubaix.kaiDJ.db.Db;
import com.cubaix.kaiDJ.db.Id3Analyzer;
import com.cubaix.kaiDJ.db.SongDescr;
import com.cubaix.kaiDJ.swt.KaiButton;
import com.cubaix.kaiDJ.web.BrowserControl;
import com.cubaix.kaiDJ.xml.ConfigLoader;

// Main class, for all in JDJ
public class KaiDJ {
	static final public String _VERSION = "0.6.1";

	static public boolean _SIZE_FOR_SCREENSHOTS = false;
	static final public int _SIZE_FOR_SCREENSHOTS_W = 1024;
	static final public int _SIZE_FOR_SCREENSHOTS_H = 576;
	public static final boolean _DEBUG_PAINT = false;
	
	public static String kaiDir = System.getProperty("user.home")+File.separatorChar+"karaok-AI";
	static {
		if(!new File(kaiDir).exists()) {
			new File(kaiDir).mkdirs();
		}
	}
	public String configPath = kaiDir+File.separatorChar+"config.xml";
	String currentPLPath = System.getProperty("user.home");

	public Display display = null;
	public Shell shell = null;
	public GC mainGC = null;
	
	public String userEMail = "";
	public String userCode = "";

	// Colors
	public Color whiteC;
	public Color blackC;
	public Color grayC;
	public Color blueC;
	public Color redC;
	public Color yellowC;
	public Color mainBckC;
	public Color logoDarkC;
	public Color logoLightC;
	public Color playerC;
	public Color playerBarC;
	public Color selectedC;

	public Font initialFont;
	public Font largeFont;
	public Font kaiFont;
	public Font butFont;
	public Font viewerFont;
	
	public Image kaiIcon = null;
	public Image kaiButIcon = null;

	// Log
	public LogoPanel logoPanel = null;

	//EM 24/08/2008 : store mixers indexes for valid stored mixers
	public ArrayList mixers = new ArrayList();
	public ArrayList mixersIndex = new ArrayList();

	// Players
	public DJPlayer playerPre = null;
	public DJPlayer player1 = null;
	public DJPlayer player2 = null;

	//EM 06/07/2008 : stop-after button
	KaiButton stopAfterB;
	boolean stopAfter = false;
	
	//EM 28/04/2009
	KaiButton showSoundProfileB;
	boolean showSoundProfile = false;


	public int soundCard1 = -1;
	public int soundCard2 = -1;
	public int soundCardPre = -1;
	public int soundCardJava = -1;

	Combo soundCard1C;
	Combo soundCard2C;
	Combo soundCardPreC;

	KaiButton autoMixB;

	boolean autoMix = true;
	boolean timeToNextDone = false;

	// Data
	public Db db;

	Id3Analyzer analyzer;

	// Interface groups
	public SearchManager managerSearch;
	public CategoryManager managerCategories = null;
	PlayManager managerPlay;
	Composite searchPanel;

	//EM 02/11/2008
	String msgUpdate = null;
	
	KaiEditor kaiEditor = null;
	
	KaiDJ() {
	}

	/**
	 * 
	 */
	void initDbList() {
		if (db == null) {
			db = new Db(this);
			db.initDb();
		}
		setMsg("Loading db..");
		Vector aAll = new Vector();
		managerSearch.pl.setPl(aAll);
		db.getAll(aAll);
		managerSearch.pl.listTC.needRedraw();
		setMsg("");
	}

	/**
	 * @param aPath
	 */
	void scanDir(final String aPath) {
		Thread aTh = new Thread(new Runnable() {
			public void run() {
				try {
					analyzer.stopScan = true;
					db.scanDir(aPath);
					//EM 13/09/2008
					db.cleanDoubles();
					initDbList();
					analyzer.scan();
					setMsg("");
					managerSearch.pl.listTC.needRedraw();
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
		});
		aTh.setPriority(Thread.MIN_PRIORITY);
		aTh.start();
	}

	/**
	 * 
	 */
	void paint() {
		if(KaiDJ._DEBUG_PAINT) {
			System.out.println("KAIDJ.paint()");
		}
		synchronized (mainGC) {
			Rectangle aRect = shell.getBounds();
			mainGC = new GC(shell);
			mainGC.setClipping(0, 0, aRect.width, aRect.height);
			mainGC.setBackground(mainBckC);
			mainGC.fillRectangle(0, 0, aRect.width, aRect.height);
			if (playerPre != null) {
				playerPre.needRedraw();
			}
			if (player1 != null) {
				player1.needRedraw();
			}
			if (player2 != null) {
				player2.needRedraw();
			}
			if (managerSearch != null) {
				managerSearch.pl.listTC.needRedraw();
			}
			if (managerPlay != null) {
				managerPlay.pl.listTC.needRedraw();
			}
			mainGC.dispose();
		}
	}

	/**
	 * @param aMsg
	 */
	public void setMsg(final String aMsg) {
		msgUpdate = aMsg;
	}

	/**
	 * 
	 */
	void createInterface() {
		// final jDJ aThis = this;
		display = new Display();
		shell = new Shell(display, SWT.SHELL_TRIM);
		shell.open();
		mainGC = new GC(shell);
		shell.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent arg0) {
				paint();
			}
		});
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		shell.setLayout(gridLayout);
		setMsg("");
		InputStream aIS = getClass().getResourceAsStream("img/Icon.png");
		kaiIcon = new Image(display, aIS);
		aIS = getClass().getResourceAsStream("img/playerBck.png");
		shell.setImage(kaiIcon);
		// Colors
		whiteC = display.getSystemColor(SWT.COLOR_WHITE);
		blackC = display.getSystemColor(SWT.COLOR_BLACK);
		grayC = display.getSystemColor(SWT.COLOR_GRAY);
		blueC = display.getSystemColor(SWT.COLOR_BLUE);
		redC = display.getSystemColor(SWT.COLOR_RED);
		//EM 18/05/2008
		yellowC = display.getSystemColor(SWT.COLOR_YELLOW);
		mainBckC = new Color(display, 0, 0, 100);
		logoDarkC = new Color(display, 121,44,125);
		logoLightC = new Color(display, 226,185,228);
		playerC = logoLightC;
		playerBarC = new Color(display, 33, 59, 255);
		selectedC =  new Color(display, 121, 44, 125);

		// Fonts
		initialFont = mainGC.getFont();
		
		FontData[] fontData = initialFont.getFontData();
		for (int i = 0; i < fontData.length; i++) {
			fontData[i].setHeight(22);
		}
		largeFont = new Font(display, fontData);

		fontData = initialFont.getFontData();
		for (int i = 0; i < fontData.length; i++) {
			fontData[i].setHeight(12);
		}
		kaiFont = new Font(display, fontData);

		fontData = initialFont.getFontData();
		for (int i = 0; i < fontData.length; i++) {
			fontData[i].setHeight(14);
		}
		butFont = new Font(display, fontData);

		fontData = initialFont.getFontData();
		for (int i = 0; i < fontData.length; i++) {
			fontData[i].setHeight(36);
		}
		viewerFont = new Font(display, fontData);

		// Panels
		// Composite aC;
		GridData aGD;
		// Image aImg;
		// ##Top##
		createPlayers();
		// ##Bottom##
		Composite aBottomC = new Composite(shell, SWT.NULL);
		// aBottomC.setBackground(mainBckC);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		aBottomC.setLayout(gridLayout);
		aGD = new GridData(GridData.FILL_BOTH);
		aBottomC.setLayoutData(aGD);
		// Buttons col
		createButtons(aBottomC);
		// Lists cols
		Composite aCentralC = new Composite(aBottomC, SWT.NULL);
		aCentralC.setBackground(mainBckC);
		aGD = new GridData(GridData.FILL_BOTH);
		aCentralC.setLayoutData(aGD);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		aCentralC.setLayout(gridLayout);

		// Search panel
		searchPanel = new Composite(aCentralC, SWT.NONE);
		aGD = new GridData(GridData.FILL_BOTH);
		searchPanel.setLayoutData(aGD);
		GridLayout aGL = new GridLayout();
		aGL.numColumns = 1;
		aGL.marginWidth = 0;
		aGL.marginHeight = 0;
		aGL.horizontalSpacing = 0;
		aGL.verticalSpacing = 0;
		searchPanel.setLayout(aGL);

		managerSearch = new SearchManager(this, searchPanel, SWT.NULL);

		// Play panel
		managerPlay = new PlayManager(this, aCentralC, SWT.NULL);

		// Add listeners
		createListeners();
		
		aIS = getClass().getResourceAsStream("img2/kaiIcon.png");
		kaiButIcon = new Image(display, aIS);

		shell.pack();
		Rectangle aR = display.getBounds();
		if(aR.width > 1920) {
			aR.width = 1920;
		}
		if(aR.height > 1080) {
			aR.height = 1080;
		}
		aR.width -= 20;
		aR.height /=2;
		if(KaiDJ._SIZE_FOR_SCREENSHOTS) {
			aR.width = KaiDJ._SIZE_FOR_SCREENSHOTS_W;
			aR.height = KaiDJ._SIZE_FOR_SCREENSHOTS_H;
		}
		shell.setBounds(aR);
	}

	/**
	 * 
	 */
	void createPlayers() {
		GridData aGD;
		GridLayout gridLayout;
		
		Composite aBlank;
		GridData aGDblank;
		
		Composite aInterC;
		GridData aGDinter;
		
		// Players
		Composite aPlayersC = new Composite(shell, SWT.NULL);
		aPlayersC.setBackground(mainBckC);
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aPlayersC.setLayoutData(aGD);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 5;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.makeColumnsEqualWidth = false;
		aPlayersC.setLayout(gridLayout);

		// Pre
		aGD = new GridData();
		aGD.widthHint = DJPlayer._WIDTHINT;
		aGD.heightHint = DJPlayer._HEIGHTINT;
		playerPre = new DJPlayer(this, aPlayersC, SWT.NULL, 2,false);
		playerPre.setLayoutData(aGD);
		// Logo
		logoPanel = new LogoPanel(this, aPlayersC, SWT.BORDER);
		logoPanel.setBackground(mainBckC);
		GridData aGDLogo = new GridData(GridData.FILL_BOTH);
		aGDLogo.verticalSpan = 2;
		logoPanel.setLayoutData(aGDLogo);

		// P1
		player1 = new DJPlayer(this, aPlayersC, SWT.NULL, 0,true);
		player1.setLayoutData(aGD);
		
		//EM 06/07/2008 : stop-after button
		aInterC = new Composite(aPlayersC, SWT.NULL);
		aInterC.setBackground(this.mainBckC);
		GridData aGDcc = new GridData(GridData.FILL_VERTICAL);
		aGDcc.verticalSpan = 2;
		aInterC.setLayoutData(aGDcc);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		aInterC.setLayout(gridLayout);
		
//		aBlank = new Composite(aInterC, SWT.NULL);
//		aBlank.setBackground(this.mainBckC);
//		aGDblank = new GridData(GridData.FILL);
//		aBlank.setLayoutData(aGDblank);

		//EM 06/07/2008 : automix now here !
		// Auto mix
		autoMixB = new KaiButton(this, aInterC, SWT.TOGGLE);
		autoMixB.setBackground(this.mainBckC);
		autoMixB.setSelection(autoMix);
		autoMixB.setToolTipText("Auto-mix ON/OFF");
		autoMixB.setImagePath("img2/mixBut.png");
		autoMixB.setText("Mix");
		aGDinter = new GridData(GridData.FILL_HORIZONTAL);
		aGDinter.heightHint = KaiButton._HEIGHT;
		aGDinter.widthHint = KaiButton._WIDTH;
		autoMixB.setLayoutData(aGDinter);
		autoMixB.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				autoMix = autoMixB.getSelection();
			}
		});

		stopAfterB = new KaiButton(this, aInterC, SWT.TOGGLE);
		stopAfterB.setBackground(this.mainBckC);
		stopAfterB.setSelection(stopAfter);
		stopAfterB.setToolTipText("Stop after this song ON/OFF");
		stopAfterB.setImagePath("img2/stopAftBut.png");
		stopAfterB.setText("Stop");
		aGDinter = new GridData(GridData.FILL_HORIZONTAL);
		aGDinter.heightHint = KaiButton._HEIGHT;
		aGDinter.widthHint = KaiButton._WIDTH;
		stopAfterB.setLayoutData(aGDinter);
		stopAfterB.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				stopAfter = stopAfterB.getSelection();
			}
		});

		showSoundProfileB = new KaiButton(this, aInterC, SWT.TOGGLE);
		showSoundProfileB.setBackground(this.mainBckC);
		showSoundProfileB.setSelection(showSoundProfile);
		showSoundProfileB.setToolTipText("Show sound profile ON/OFF");
		showSoundProfileB.setImagePath("img2/showProfBut.png");
		showSoundProfileB.setText("Profile");
		aGDinter = new GridData(GridData.FILL_HORIZONTAL);
		aGDinter.heightHint = KaiButton._HEIGHT;
		aGDinter.widthHint = KaiButton._WIDTH;
		showSoundProfileB.setLayoutData(aGDinter);
		showSoundProfileB.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				showSoundProfile = showSoundProfileB.getSelection();
			}
		});

//Bad position with this one ??
//		aBlank = new Composite(aInterC, SWT.NULL);
//		aBlank.setBackground(this.mainBckC);
//		aGDblank = new GridData(GridData.FILL_HORIZONTAL);
//		aGDblank.heightHint = JDJButton._HEIGHT;
//		aBlank.setLayoutData(aGDblank);


		// P2
		player2 = new DJPlayer(this, aPlayersC, SWT.NULL, 0,true);
		player2.setLayoutData(aGD);

		// Get all sound cards
		Mixer.Info[] mInfos = AudioSystem.getMixerInfo();
		if (mInfos != null) {
			for (int i = 0; i < mInfos.length; i++) {
				Line.Info lineInfo = new Line.Info(SourceDataLine.class);
				Mixer mixer = AudioSystem.getMixer(mInfos[i]);
				if (mixer.isLineSupported(lineInfo)) {
					System.out.println("MIXER added : " + mInfos[i].getName());
					mixers.add(mInfos[i].getName());
					mixersIndex.add(new Integer(i));
				}
				else{
					System.out.println("MIXER not added : " + mInfos[i].getName());
				}
			}
		}
		// Selectors
		GridData aGDSel = new GridData(GridData.CENTER);
		aGDSel.widthHint = DJPlayer._WIDTHINT;
		final KaiDJ aThis = this;

		soundCardPreC = new Combo(aPlayersC, SWT.NULL);
		soundCardPreC.setBackground(this.mainBckC);
		soundCardPreC.setForeground(this.playerBarC);
		for (int c = 0; c < mixers.size(); c++) {
			//EM 03/06/2008 : search for Java sound card
			String aSCName = (String) mixers.get(c);
			soundCardPreC.add(aSCName);
			if(aSCName.toLowerCase().indexOf("java") >= 0
					|| aSCName.toLowerCase().indexOf("default") >= 0
					|| aSCName.toLowerCase().indexOf("primary") >= 0){
				soundCardJava = ((Integer)mixersIndex.get(c)).intValue();
			}
		}
		soundCardPreC.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				//EM 24/08/2008 : store mixers indexes for valid stored mixers
				playerPre.setDestCard(soundCardPre = ((Integer)mixersIndex.get(soundCardPreC.getSelectionIndex())).intValue());
				ConfigLoader.save(aThis, configPath);
			}
		});
		soundCardPreC.setLayoutData(aGDSel);

		soundCard1C = new Combo(aPlayersC, SWT.NULL);
		soundCard1C.setBackground(this.mainBckC);
		soundCard1C.setForeground(this.playerBarC);
		for (int c = 0; c < mixers.size(); c++) {
			soundCard1C.add((String) mixers.get(c));
		}
		soundCard1C.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				//EM 24/08/2008 : store mixers indexes for valid stored mixers
				player1.setDestCard(soundCard1 = ((Integer)mixersIndex.get(soundCard1C.getSelectionIndex())).intValue());
				ConfigLoader.save(aThis, configPath);
			}
		});
		soundCard1C.setLayoutData(aGDSel);

		soundCard2C = new Combo(aPlayersC, SWT.NULL);
		soundCard2C.setBackground(this.mainBckC);
		soundCard2C.setForeground(this.playerBarC);
		for (int c = 0; c < mixers.size(); c++) {
			soundCard2C.add((String) mixers.get(c));
		}
		soundCard2C.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				//EM 24/08/2008 : store mixers indexes for valid stored mixers
				player2.setDestCard(soundCard2 = ((Integer)mixersIndex.get(soundCard2C.getSelectionIndex())).intValue());
				ConfigLoader.save(aThis, configPath);
			}
		});
		soundCard2C.setLayoutData(aGDSel);
	}

	/**
	 * @param aParentC
	 */
	void createButtons(Composite aParentC) {
		//https://icons.getbootstrap.com/
		GridData aGD;
		GridLayout gridLayout;
		Composite aBlank;
		KaiButton aBut;
		// Create buttons panel
		Composite aButtonsC = new Composite(aParentC, SWT.NULL);
		aButtonsC.setBackground(this.mainBckC);
		aGD = new GridData(GridData.FILL_VERTICAL);
		aButtonsC.setLayoutData(aGD);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginRight = 0;// gridLayout.marginWidth;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		aButtonsC.setLayout(gridLayout);

		// Blank panel
		aBlank = new Composite(aButtonsC, SWT.NULL);
		aBlank.setBackground(this.mainBckC);
		aGD = new GridData(GridData.FILL_BOTH);
		aBlank.setLayoutData(aGD);
		// Open/Save
		aBut = new KaiButton(this, aButtonsC, SWT.NULL);
		aBut.setBackground(this.mainBckC);
		aBut.setToolTipText("Clear playlist");
		aBut.setImagePath("img2/newPLBut.png");
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		aBut.setLayoutData(aGD);
		aBut.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				managerPlay.pl.setPl(new Vector());
				managerPlay.pl.listTC.needRedraw();
			}
		});
		aBut = new KaiButton(this, aButtonsC, SWT.NULL);
		aBut.setBackground(this.mainBckC);
		aBut.setToolTipText("Open playlist");
		aBut.setImagePath("img2/openPLBut.png");
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		aBut.setLayoutData(aGD);
		aBut.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				FileDialog aFD = new FileDialog(shell, SWT.OPEN);
				aFD.setFilterExtensions(new String[] { "*.jdj;*.m3u;*.m3u8" });
				System.out.println("Current path : " + currentPLPath);
				aFD.setFilterPath(currentPLPath);
				String aPath = aFD.open();
				if (aPath != null) {
					System.out.println("Open : " + aPath);
					currentPLPath = aPath;
					managerPlay.load(aPath, true);
					managerPlay.pl.listTC.needRedraw();
				}
			}
		});
		aBut = new KaiButton(this, aButtonsC, SWT.NULL);
		aBut.setBackground(this.mainBckC);
		aBut.setToolTipText("Save playlist");
		aBut.setImagePath("img2/savePLBut.png");
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		aBut.setLayoutData(aGD);
		aBut.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				FileDialog aFD = new FileDialog(shell, SWT.SAVE);
				aFD.setFilterExtensions(new String[] { "*.jdj;*.m3u;*.m3u8" });
				System.out.println("Current path : " + currentPLPath);
				aFD.setFilterPath(currentPLPath);
				String aPath = aFD.open();
				if (aPath != null) {
					System.out.println("Save : " + aPath);
					currentPLPath = aPath;
					managerPlay.pl.save(aPath);
				}
			}
		});
		// Blank panel
		aBlank = new Composite(aButtonsC, SWT.NULL);
		aBlank.setBackground(this.mainBckC);
		aGD = new GridData(GridData.FILL_BOTH);
		aBlank.setLayoutData(aGD);
		// karaok-AI
		KaiButton aKai = new KaiButton(this, aButtonsC, SWT.NULL);
		aKai.setBackground(this.mainBckC);
		aKai.setToolTipText("Open karaok-AI (select a song first)");
		aKai.setImagePath("img2/kaiBut.png");
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		aKai.setLayoutData(aGD);
		final KaiDJ aThis = this;
		aKai.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				SongDescr aSong = managerSearch.pl.getSelected();
				aSong = new SongDescr(aSong);//Enforce update
				if(aSong == null) {
					return;
				}
				if(kaiEditor == null || kaiEditor.shell.isDisposed()) {
					kaiEditor = new KaiEditor(aThis);
				}
				kaiEditor.load(aSong);
			}
		});
		KaiButton aSearchKai = new KaiButton(this, aButtonsC, SWT.NULL);
		aSearchKai.setBackground(this.mainBckC);
		aSearchKai.setToolTipText("Search songs with karaok-AI vocals and lyrics extractions");
		aSearchKai.setImagePath("img2/searchKaiBut.png");
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		aSearchKai.setLayoutData(aGD);
		aSearchKai.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				display.asyncExec(new Runnable() {
					public void run() {
						managerSearch.searchText.setText("kAI");
					}
				});
			}
		});
		// Blank panel
		aBlank = new Composite(aButtonsC, SWT.NULL);
		aBlank.setBackground(this.mainBckC);
		aGD = new GridData(GridData.FILL_BOTH);
		aBlank.setLayoutData(aGD);
		// Categories
		KaiButton aAddRemove = new KaiButton(this, aButtonsC, SWT.NULL);
		aAddRemove.setBackground(this.mainBckC);
		aAddRemove.setToolTipText("Show/hide categories");
		aAddRemove.setImagePath("img2/catBut.png");
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		aAddRemove.setLayoutData(aGD);
		aAddRemove.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				if (managerCategories != null && !managerCategories.isDisposed()) {
					managerCategories.dispose();// .setVisible(false);
					managerCategories = null;
					searchPanel.layout();
				} else {
					managerCategories = new CategoryManager(aThis, searchPanel, SWT.NULL);
					searchPanel.layout();
				}
			}
		});
		aBut = new KaiButton(this, aButtonsC, SWT.NULL);
		aBut.setBackground(this.mainBckC);
		aBut.setToolTipText("Add playlist to category");
		aBut.setImagePath("img2/openCatBut.png");
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		aBut.setLayoutData(aGD);
		aBut.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				if (managerCategories == null || managerCategories.isDisposed()) {
					managerCategories = new CategoryManager(aThis, searchPanel, SWT.NULL);
					searchPanel.layout();
				}
				FileDialog aFD = new FileDialog(shell, SWT.OPEN);
				aFD.setFilterExtensions(new String[] { "*.jdj;*.m3u;*.m3u8" });
				System.out.println("Current path : " + currentPLPath);
				aFD.setFilterPath(currentPLPath);
				String aPath = aFD.open();
				if (aPath != null) {
					// Check cat
					String aCat = new File(aPath).getName();
					if (aCat.lastIndexOf(".") > 0) {
						aCat = aCat.substring(0, aCat.lastIndexOf("."));
					}
					managerCategories.checkCategory(aCat);
					// Open
					System.out.println("Open : " + aPath);
					currentPLPath = aPath;
					managerCategories.load(aPath, false);
					managerCategories.pl.listTC.needRedraw();
				}
			}
		});
		aBut = new KaiButton(this, aButtonsC, SWT.NULL);
		aBut.setBackground(this.mainBckC);
		aBut.setToolTipText("Save category as playlist");
		aBut.setImagePath("img2/saveCatBut.png");
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		aBut.setLayoutData(aGD);
		aBut.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				if (managerCategories == null || managerCategories.isDisposed()) {
					return;
				}
				FileDialog aFD = new FileDialog(shell, SWT.SAVE);
				aFD.setFilterExtensions(new String[] { "*.jdj;*.m3u;*.m3u8" });
				System.out.println("Current path : " + currentPLPath);
				aFD.setFilterPath(currentPLPath);
				aFD.setFileName(managerCategories.currentCategory);
				String aPath = aFD.open();
				if (aPath != null) {
					System.out.println("Save : " + aPath);
					currentPLPath = aPath;
					managerCategories.pl.save(aPath);
				}
			}
		});
		// Blank panel
		aBlank = new Composite(aButtonsC, SWT.NULL);
		aBlank.setBackground(this.mainBckC);
		aGD = new GridData(GridData.FILL_BOTH);
		aBlank.setLayoutData(aGD);
		// Bd buttons
		aBut = new KaiButton(this, aButtonsC, SWT.NULL);
		aBut.setBackground(this.mainBckC);
		aBut.setToolTipText("Add folder to Db");
		aBut.setImagePath("img2/addDbBut.png");
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		aBut.setLayoutData(aGD);
		aBut.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				// display.asyncExec(new Runnable() {
				// public void run() {
				DirectoryDialog aDD = new DirectoryDialog(shell, SWT.NULL);
				String aPath = aDD.open();
				scanDir(aPath);
				initDbList();
				// }
				// });
			}
		});
		aBut = new KaiButton(this, aButtonsC, SWT.NULL);
		aBut.setBackground(this.mainBckC);
		aBut.setToolTipText("Erase Db");
		aBut.setImagePath("img2/eraseDbBut.png");
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		aBut.setLayoutData(aGD);
		aBut.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				analyzer.stopScan = true;
				db.delAll();
				initDbList();
				analyzer.scan();
			}
		});
		// Blank panel
		aBlank = new Composite(aButtonsC, SWT.NULL);
		aBlank.setBackground(this.mainBckC);
		aGD = new GridData(GridData.FILL_BOTH);
		aBlank.setLayoutData(aGD);
		// Web site
		aBut = new KaiButton(this, aButtonsC, SWT.NULL);
		aBut.setBackground(this.mainBckC);
		aBut.setToolTipText("karaok-AI web site:\n- Download updates\n- Documentation\n- Forum");
		aBut.setImagePath("img2/webSiteBut.png");
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aGD.heightHint = KaiButton._HEIGHT;
		aGD.widthHint = KaiButton._WIDTH;
		aBut.setLayoutData(aGD);
		aBut.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				BrowserControl.displayURL("http://karaok-AI.com");
			}
		});
	}

	/**
	 * 
	 */
	void createListeners() {
		// Some mouse event are handled here to involve several objects

		player1.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent aEvt) {
			}

			public void mouseDown(MouseEvent aEvt) {
				if (player1.click(aEvt.x, aEvt.y) > 0) {
					// Done
					return;
				}

				// No need to stay in the SWT thread
				Thread aTh = new Thread(new Runnable() {
					public void run() {
						if (player1.playState == 1) {
							// Now playing, toggle
							player1.togglePlay();
						} else if (player1.playState < 0) {
							// Nothing is waiting to play, try to get something new
							timeToNext(player1);
						} else {
							player1.fadeIn();
							player2.fadeOut();
						}
					}
				}, "TMP (PLAYER-1 MOUSE DOWN)");
				aTh.start();
			}

			public void mouseUp(MouseEvent arg0) {
			}
		});
		player2.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent aEvt) {
			}

			public void mouseDown(MouseEvent aEvt) {
				if (player2.click(aEvt.x, aEvt.y) > 0) {
					// Done
					return;
				}
				
				// No need to stay in the SWT thread
				Thread aTh = new Thread(new Runnable() {
					public void run() {
						if (player2.playState == 1) {
							// Now playing, toggle
							player2.togglePlay();
						} else if (player2.playState < 0) {
							// Nothing is waiting to play, try to get something new
							timeToNext(player2);
						} else {
							player2.fadeIn();
							player1.fadeOut();
						}
					}
				}, "TMP (PLAYER-2 MOUSE DOWN)");
				aTh.start();
			}

			public void mouseUp(MouseEvent arg0) {
			}
		});
		playerPre.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent aEvt) {
			}

			public void mouseDown(MouseEvent aEvt) {
				if (playerPre.click(aEvt.x, aEvt.y) > 0) {
					// Done
					return;
				}
				
				// No need to stay in the SWT thread
				Thread aTh = new Thread(new Runnable() {
					public void run() {
						playerPre.togglePlay();
					}
				}, "TMP (PLAYER-PRE TOGGLE PLAY)");
				aTh.start();
			}

			public void mouseUp(MouseEvent arg0) {
			}
		});
	}
	
	//EM 24/08/2008 
	public int mixerIndexToComboIndex(int aIndex){
		for(int i = 0;i < mixersIndex.size();i++){
			if(((Integer)mixersIndex.get(i)).intValue() == aIndex){
				return i;
			}
		}
		return -1;
	}

	/**
	 * 
	 */
	void start() {
		try {
			// Create interface
			createInterface();
			// Load config
			ConfigLoader.load(this, configPath);
			display.asyncExec(new Runnable() {
				public void run() {
					//EM 03/06/2008 : default sound card = Java
					int aIndex;
					aIndex = (soundCard1 >= 0) ? soundCard1 : ((soundCardJava >= 0) ? soundCardJava : 0);
					soundCard1C.select(mixerIndexToComboIndex(aIndex));
					player1.setDestCard(aIndex);
					aIndex = (soundCard2 >= 0) ? soundCard2 : ((soundCardJava >= 0) ? soundCardJava : 0);
					soundCard2C.select(mixerIndexToComboIndex(aIndex));
					player2.setDestCard(aIndex);
					aIndex = (soundCardPre >= 0) ? soundCardPre : ((soundCardJava >= 0) ? soundCardJava : 0);
					soundCardPreC.select(mixerIndexToComboIndex(aIndex));
					playerPre.setDestCard(aIndex);
				}
			});

			// Load data
			final KaiDJ aThis = this;
			Thread aTh = new Thread(new Runnable() {
				public void run() {
					display.syncExec(new Runnable() {
						public void run() {
							shell.setCursor(new Cursor(display, SWT.CURSOR_WAIT));
						}
					});
					long aTime = new Date().getTime();
					// init db
					initDbList();
					System.out.println("DB loaded in : " + DJPlayer.formatTimeMs(new Date().getTime() - aTime));
					//EM 13/09/2008
					aTime = new Date().getTime();
					db.cleanDoubles();
					System.out.println("Db cleaned in : " + DJPlayer.formatTimeMs(new Date().getTime() - aTime));
					// Try to load last pl
					aTime = new Date().getTime();
					managerPlay.load(kaiDir+File.separatorChar+"last.jdj", true);
					managerPlay.pl.listTC.needRedraw();
					System.out.println("Last PL loaded in : " + DJPlayer.formatTimeMs(new Date().getTime() - aTime));
					// Start analyzer
					analyzer = new Id3Analyzer(aThis, db);
					display.syncExec(new Runnable() {
						public void run() {
							shell.setCursor(null);
						}
					});
					//Start with something (first launch?)
					if(managerSearch.pl.getPl().size() <= 0 && new File("demo").exists()) {
						try {
							scanDir(new File("demo").getCanonicalPath());
						} catch (Exception e) {
							e.printStackTrace(System.err);
						}
					}

				}
			},
			//EM 02/11/2008
			"LOADING DATA");
			aTh.start();
			
			//EM 02/11/2008 : async message update (display.asyncExec() can be a bit time consuming)
			Thread aMsgUpdateTh = new Thread(new Runnable(){
				public void run() {
					while(true){
						try{
							Thread.sleep(100);
						}
						catch(Throwable t){
							//??
						}
						if (display == null || display.isDisposed()) {
							continue;
						}
						if(msgUpdate != null){
							final String aMsg = msgUpdate;//Avoid a possible change in the async exec
							display.asyncExec(new Runnable() {
								public void run() {
									if (!shell.isDisposed()) {
										shell.setText("karaok-AI " + _VERSION + " " + aMsg);
										msgUpdate = null;
									}
								}
							});
						}
					}
				}
			},"MSG Update");
			aMsgUpdateTh.setPriority(Thread.MIN_PRIORITY);
			aMsgUpdateTh.start();
			// Run main interface loop

			// If main interface thread is of low priority, this can cause playing troubles.. need to be reactive !
			// Be time respectuous
			// Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			try {
				while (!shell.isDisposed()) {
					try {
						if (!display.readAndDispatch()) {
							display.sleep();
						}
					} catch (Throwable t) {
						// Not handled ?
						System.err.println("Not handled error .. ");
						t.printStackTrace(System.err);
					}
					// System.out.println("Main SWT event loop");
				}
			} catch (Throwable t) {
				t.printStackTrace(System.err);
			}
			managerPlay.pl.save(kaiDir+"/last.jdj");
			display.dispose();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
		System.exit(0);
	}

	/**
	 * @param aDescr
	 */
	public void updatedDescr(SongDescr aDescr) {
		managerSearch.pl.updatedDescr(aDescr);
		managerPlay.pl.updatedDescr(aDescr);
	}

	/**
	 * @param aPath
	 */
	public void loadPre(String aPath) {
		playerPre.load(aPath);
	}

	/**
	 * @param aEndingPlayer
	 *            the player that ends its play and ask for a mix. Can be null if the call is made outside of a player
	 */
	public void timeToNext(DJPlayer aEndingPlayer) {
		if (aEndingPlayer == playerPre) {
			// Don't mix on pre-listener
			return;
		}
		// Avoid multiple mix
		if (timeToNextDone) {
			// Already done
			return;
		}
		synchronized (this) {// Avoid several launching before playState is set
			// Avoid multiple mix
			timeToNextDone = true;

			//EM 06/07/2008 : stop-after 
			if ((stopAfter && player1.playState <= 0 && player2.playState <= 0) //
					|| (!stopAfter && managerSearch.randomAllFlag && (player1.playState <= 0 || player2.playState <= 0))) {
				managerPlay.addSong(managerSearch.pl.getRandomPlaying());
			}
			//EM 06/07/2008 : stop-after 
			if (!stopAfter && player1.playState <= 0) {
				SongDescr aDescr = managerPlay.pl.nextPlayable();
				if (aDescr != null) {
					player1.load(aDescr.path);
					player1.setGain(autoMix ? 0 : 0.8);
					player1.play();
					player1.fadeIn();
					player2.fadeOut();
				}
			} 
			//EM 06/07/2008 : stop-after 
			else if (!stopAfter && player2.playState <= 0) {
				SongDescr aDescr = managerPlay.pl.nextPlayable();
				if (aDescr != null) {
					player2.load(aDescr.path);
					player2.setGain(autoMix ? 0 : 0.8);
					player2.play();
					player2.fadeIn();
					player1.fadeOut();
				}
			}

			// Can mix again
			timeToNextDone = false;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Java version : " + System.getProperty("java.version"));
		System.out.println("JRE location : " + System.getProperty("java.home"));
		System.out.println("SWT version : " + SWT.getVersion());
		for(String aA : args) {
			if("SCR".equalsIgnoreCase(aA)) {
				KaiDJ._SIZE_FOR_SCREENSHOTS = true;
			}
		}
		new KaiDJ().start();
	}
}
