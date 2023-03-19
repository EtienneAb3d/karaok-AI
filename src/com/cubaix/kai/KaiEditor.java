package com.cubaix.kai;

import java.io.File;
import java.util.Arrays;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.cubaix.kaiDJ.DJPlayer;
import com.cubaix.kaiDJ.KaiDJ;
import com.cubaix.kaiDJ.db.SongDescr;
import com.cubaix.kaiDJ.xml.ConfigLoader;

public class KaiEditor {
	static String[] languages = new String[] {
			"AR","BE","BG","BS","CS","DA","DE"
			,"EL","EN","ES","ET","FA","FI","FR"
			,"HE","HR","HU","IS","IT","JA"
			,"KM","LT","LV","MT","NL","NO","PL","PT"
			,"RO","RU","SK","SL","SV","SQ","SR"
			,"TA","TL","TR","UK","ZH"};
	static {
		Arrays.sort(languages);
	}
	protected KaiDJ parentKDJ;
	
	public Shell shell = null;
	public GC mainGC = null;
	
	LogoPanel logoPanel = null;
	
	KaiPlayer playerVocals = null;
	KaiPlayer playerDrums = null;
	KaiPlayer playerBass = null;
	KaiPlayer playerOther = null;

	Combo soundCardVocalsC = null;
	Combo soundCardDrumsC = null;
	Combo soundCardBassC = null;
	Combo soundCardOtherC = null;
	
	SongDescr song = null;
	String songLng = "??";
	Text editor = null;
	String editorLastContent = null;
	
	KaiViewer viewer = null;
	
	public KaiEditor(KaiDJ aParentKDJ) {
		parentKDJ = aParentKDJ;
		start();
	}
	
	void createInterface() {
		// final jDJ aThis = this;
		shell = new Shell(parentKDJ.display, SWT.SHELL_TRIM);
		shell.setText("karaok-AI " + parentKDJ._VERSION);
		shell.open();
		mainGC = new GC(shell);
		shell.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent arg0) {
				paint();
			}
		});
		shell.setImage(parentKDJ.kaiIcon);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		shell.setLayout(gridLayout);
		
		createPlayers();
		
		editor = new Text(shell,SWT.MULTI|SWT.V_SCROLL|SWT.H_SCROLL);
		GridData aGD = new GridData(GridData.FILL_BOTH);
		editor.setLayoutData(aGD);
		editor.setBackground(parentKDJ.playerC);
		editor.setForeground(parentKDJ.blackC);
		editor.setCursor(new Cursor(parentKDJ.display,SWT.CURSOR_UPARROW));
		editor.setFont(parentKDJ.kaiFont);
		
		viewer = new KaiViewer(this); 
		
		createForm();
		
		createListeners();
		
		shell.pack();
		Rectangle aR = parentKDJ.display.getBounds();
		if(aR.width > 1920) {
			aR.width = 1920;
		}
		if(aR.height > 1080) {
			aR.height = 1080;
		}
		aR.width /=2;
		aR.height /=2;
		aR.y += aR.height;
		aR.width -= 20;
		if(aR.y+aR.height > parentKDJ.display.getBounds().height - 40) {
			aR.y = parentKDJ.display.getBounds().height-aR.height - 40;
		}
		shell.setBounds(aR);
	}

	void createPlayers() {
		GridData aGD;
		GridLayout gridLayout;
		
		Composite aBlank;
		GridData aGDblank;
		
		Composite aInterC;
		GridData aGDinter;
		
		// Players
		Composite aPlayersC = new Composite(shell, SWT.NULL);
		aPlayersC.setBackground(parentKDJ.mainBckC);
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aPlayersC.setLayoutData(aGD);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 5;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.makeColumnsEqualWidth = false;
		aPlayersC.setLayout(gridLayout);

		// Vocals
		aGD = new GridData();
		aGD.widthHint = DJPlayer._WIDTHINT;
		aGD.heightHint = DJPlayer._HEIGHTINT;
		playerVocals = new KaiPlayer(parentKDJ, aPlayersC, SWT.NULL, parentKDJ.soundCardJava);
		playerVocals.setLayoutData(aGD);

		logoPanel = new LogoPanel(this,aPlayersC, SWT.FILL);
		GridData aPanelGD = new GridData(GridData.FILL_BOTH);
		aPanelGD.verticalSpan = 2;
		logoPanel.setLayoutData(aPanelGD);
		logoPanel.setBackground(parentKDJ.mainBckC);
		
		// Drums
		playerDrums = new KaiPlayer(parentKDJ, aPlayersC, SWT.NULL, parentKDJ.soundCardJava);
		playerDrums.setLayoutData(aGD);
		
		// Bass
		playerBass = new KaiPlayer(parentKDJ, aPlayersC, SWT.NULL, parentKDJ.soundCardJava);
		playerBass.setLayoutData(aGD);

		// Other
		playerOther = new KaiPlayer(parentKDJ, aPlayersC, SWT.NULL, parentKDJ.soundCardJava);
		playerOther.setLayoutData(aGD);

		// Get all sound cards
		Mixer.Info[] mInfos = AudioSystem.getMixerInfo();
		if (mInfos != null) {
			for (int i = 0; i < mInfos.length; i++) {
				Line.Info lineInfo = new Line.Info(SourceDataLine.class);
				Mixer mixer = AudioSystem.getMixer(mInfos[i]);
				if (mixer.isLineSupported(lineInfo)) {
					//EM 24/08/2008 : trace
					System.out.println("MIXER added : " + mInfos[i].getName());
					parentKDJ.mixers.add(mInfos[i].getName());
					//EM 24/08/2008
					parentKDJ.mixersIndex.add(new Integer(i));
				}
				else{
					//EM 24/08/2008 : trace
					System.out.println("MIXER not added : " + mInfos[i].getName());
				}
			}
		}
		// Selectors
		GridData aGDSel = new GridData(GridData.CENTER);
		aGDSel.widthHint = DJPlayer._WIDTHINT;

		soundCardVocalsC = new Combo(aPlayersC, SWT.NULL);
		soundCardVocalsC.setBackground(parentKDJ.mainBckC);
		soundCardVocalsC.setForeground(parentKDJ.playerBarC);
		for (int c = 0; c < parentKDJ.mixers.size(); c++) {
			String aSCName = (String) parentKDJ.mixers.get(c);
			soundCardVocalsC.add(aSCName);
		}
		soundCardVocalsC.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				playerVocals.setDestCard(((Integer)parentKDJ.mixersIndex.get(soundCardVocalsC.getSelectionIndex())).intValue());
			}
		});
		soundCardVocalsC.setLayoutData(aGDSel);

		soundCardDrumsC = new Combo(aPlayersC, SWT.NULL);
		soundCardDrumsC.setBackground(parentKDJ.mainBckC);
		soundCardDrumsC.setForeground(parentKDJ.playerBarC);
		for (int c = 0; c < parentKDJ.mixers.size(); c++) {
			String aSCName = (String) parentKDJ.mixers.get(c);
			soundCardDrumsC.add(aSCName);
		}
		soundCardDrumsC.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				playerDrums.setDestCard(((Integer)parentKDJ.mixersIndex.get(soundCardDrumsC.getSelectionIndex())).intValue());
			}
		});
		soundCardDrumsC.setLayoutData(aGDSel);

		soundCardBassC = new Combo(aPlayersC, SWT.NULL);
		soundCardBassC.setBackground(parentKDJ.mainBckC);
		soundCardBassC.setForeground(parentKDJ.playerBarC);
		for (int c = 0; c < parentKDJ.mixers.size(); c++) {
			String aSCName = (String) parentKDJ.mixers.get(c);
			soundCardBassC.add(aSCName);
		}
		soundCardBassC.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				playerBass.setDestCard(((Integer)parentKDJ.mixersIndex.get(soundCardBassC.getSelectionIndex())).intValue());
			}
		});
		soundCardBassC.setLayoutData(aGDSel);
		
		soundCardOtherC = new Combo(aPlayersC, SWT.NULL);
		soundCardOtherC.setBackground(parentKDJ.mainBckC);
		soundCardOtherC.setForeground(parentKDJ.playerBarC);
		for (int c = 0; c < parentKDJ.mixers.size(); c++) {
			String aSCName = (String) parentKDJ.mixers.get(c);
			soundCardOtherC.add(aSCName);
		}
		soundCardOtherC.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				playerOther.setDestCard(((Integer)parentKDJ.mixersIndex.get(soundCardOtherC.getSelectionIndex())).intValue());
			}
		});
		soundCardOtherC.setLayoutData(aGDSel);
	}

	void createForm() {
		GridData aLabelGD;
		GridData aTextGD;
		GridLayout gridLayout;

		Composite aFormC = new Composite(shell, SWT.NULL);
		aFormC.setBackground(parentKDJ.mainBckC);
		GridData aGD = new GridData(GridData.FILL_HORIZONTAL);
		aFormC.setLayoutData(aGD);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 8;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.makeColumnsEqualWidth = false;
		aFormC.setLayout(gridLayout);
		
		CLabel aEmailL = new CLabel(aFormC, SWT.CENTER);
		aLabelGD = new GridData(SWT.NONE, SWT.FILL, false, true);
		aLabelGD.heightHint = 30;
		aEmailL.setLayoutData(aLabelGD);
		aEmailL.setText("e-mail:");
		aEmailL.setBackground(parentKDJ.logoLightC);
		aEmailL.setForeground(parentKDJ.blackC);
		final Text aEmailT = new Text(aFormC, SWT.None);
		aTextGD = new GridData(SWT.FILL, SWT.FILL, true, true);
		aTextGD.widthHint = 60;
		aTextGD.heightHint = 30;
		aEmailT.setLayoutData(aTextGD);
		aEmailT.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent aE) {
				parentKDJ.userEMail = aEmailT.getText();
				ConfigLoader.save(parentKDJ, parentKDJ.configPath);
			}
			@Override
			public void keyPressed(KeyEvent aE) {
			}
		});
		aEmailT.setText(parentKDJ.userEMail);
		Button aGetCodeB = new Button(aFormC, SWT.NONE);
//		aGetCodeB.setLayoutData(aFillVertGD);
//		aGetCodeB.setBackground(parentKDJ.logoLightC);
//		aGetCodeB.setForeground(parentKDJ.blackC);
		aGetCodeB.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent arg0) {
				getCode();
			}
			@Override
			public void mouseDown(MouseEvent arg0) {
			}
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
			}
		});
		aGetCodeB.setText("Get code");
		CLabel aCodeL = new CLabel(aFormC, SWT.CENTER);
		aCodeL.setLayoutData(aLabelGD);
		aCodeL.setBackground(parentKDJ.logoLightC);
		aCodeL.setForeground(parentKDJ.blackC);
		aCodeL.setText("Code:");
		final Text aCodeT = new Text(aFormC, SWT.None);
		aCodeT.setLayoutData(aTextGD);
		aCodeT.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent aE) {
				parentKDJ.userCode = aCodeT.getText();
				ConfigLoader.save(parentKDJ, parentKDJ.configPath);
			}
			@Override
			public void keyPressed(KeyEvent aE) {
			}
		});
		aCodeT.setText(parentKDJ.userCode);
		CLabel aLangL = new CLabel(aFormC, SWT.CENTER);
		aLangL.setLayoutData(aLabelGD);
		aLangL.setBackground(parentKDJ.logoLightC);
		aLangL.setForeground(parentKDJ.blackC);
		aLangL.setText("Lang:");
		final Combo aLngC = new Combo(aFormC, SWT.NULL);
		GridData aComboGD = new GridData(SWT.NONE, SWT.FILL, false, true);
		aComboGD.widthHint = 60;
		aComboGD.heightHint = 30;
		aLngC.setLayoutData(aComboGD);
		aLngC.add("??");
		for (String aL : languages) {
			aLngC.add(aL.toLowerCase());
		}
		aLngC.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent aE) {
			}

			public void widgetSelected(SelectionEvent aE) {
				songLng = languages[aLngC.getSelectionIndex()-1].toLowerCase();
			}
		});
		aLngC.select(0);
		Button aExtractB = new Button(aFormC, SWT.NONE);
//		aExtractB.setLayoutData(aFillVertGD);
//		aExtractB.setBackground(parentKDJ.logoLightC);
//		aExtractB.setForeground(parentKDJ.blackC);
		aExtractB.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent arg0) {
				extract();
			}
			@Override
			public void mouseDown(MouseEvent arg0) {
			}
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
			}
		});
		aExtractB.setText("Extract");
	}
	
	void paint() {
		if(KaiDJ._DEBUG_PAINT) {
			System.out.println("KAIDJ.paint()");
		}
		synchronized (mainGC) {
			Rectangle aRect = shell.getBounds();
			mainGC = new GC(shell);
			mainGC.setClipping(0, 0, aRect.width, aRect.height);
			mainGC.setBackground(parentKDJ.mainBckC);
			mainGC.fillRectangle(0, 0, aRect.width, aRect.height);
			mainGC.dispose();
			if(playerVocals != null) {
				playerVocals.needRedraw();
			}
			if(playerDrums != null) {
				playerDrums.needRedraw();
			}
			if(playerBass != null) {
				playerBass.needRedraw();
			}
			if(playerOther != null) {
				playerOther.needRedraw();
			}
		}
	}
	
	void clickSeek(int x,int y) {
		boolean aIsPlaying = playerVocals.playState == 1;
		if(aIsPlaying) {
			togglePlay();
		}
		final boolean[] aDone = new boolean[] {false,false,false,false}; 
		Thread aThVocals = new Thread(new Runnable() {
			@Override
			public void run() {
				playerVocals.click(x, y);
				aDone[0] = true;
			}
		});
		Thread aThDrums = new Thread(new Runnable() {
			@Override
			public void run() {
				playerDrums.click(x, y);
				aDone[1] = true;
			}
		});
		Thread aThBass = new Thread(new Runnable() {
			@Override
			public void run() {
				playerBass.click(x, y);
				aDone[2] = true;
			}
		});
		Thread aThOther = new Thread(new Runnable() {
			@Override
			public void run() {
				playerOther.click(x, y);
				aDone[3] = true;
			}
		});
		aThVocals.start();
		aThDrums.start();
		aThBass.start();
		aThOther.start();
		while(!(aDone[0] && aDone[1] && aDone[2] && aDone[3])){
			try {
				Thread.sleep(10);
			} catch (Exception e) {
			}
		}
		if(aIsPlaying) {
			togglePlay();
		}
	}
	
	void togglePlay() {
		final boolean[] aDone = new boolean[] {false,false,false,false}; 
		Thread aThVocals = new Thread(new Runnable() {
			@Override
			public void run() {
				playerVocals.togglePlay();
				aDone[0] = true;
			}
		});
		Thread aThDrums = new Thread(new Runnable() {
			@Override
			public void run() {
				playerDrums.togglePlay();
				aDone[1] = true;
			}
		});
		Thread aThBass = new Thread(new Runnable() {
			@Override
			public void run() {
				playerBass.togglePlay();
				aDone[2] = true;
			}
		});
		Thread aThOther = new Thread(new Runnable() {
			@Override
			public void run() {
				playerOther.togglePlay();
				aDone[3] = true;
			}
		});
		aThVocals.start();
		aThDrums.start();
		aThBass.start();
		aThOther.start();
		while(!(aDone[0] && aDone[1] && aDone[2] && aDone[3])){
			try {
				Thread.sleep(10);
			} catch (Exception e) {
			}
		}
	}
	
	void createListeners() {
		playerVocals.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent aEvt) {
			}

			public void mouseDown(MouseEvent aEvt) {
				int aClickWhat = playerVocals.clickWhat(aEvt.x, aEvt.y);
				if(aClickWhat == 1) {//Seek
					clickSeek(aEvt.x, aEvt.y);
				}
				else if(aClickWhat == 2) {//Gain
					playerVocals.click(aEvt.x, aEvt.y);
				}
				else {
					togglePlay();
				}
			}

			public void mouseUp(MouseEvent arg0) {
			}
		});
		playerDrums.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent aEvt) {
			}

			public void mouseDown(MouseEvent aEvt) {
				int aClickWhat = playerDrums.clickWhat(aEvt.x, aEvt.y);
				if(aClickWhat == 1) {//Seek
					clickSeek(aEvt.x, aEvt.y);
				}
				else if(aClickWhat == 2) {//Gain
					playerDrums.click(aEvt.x, aEvt.y);
				}
				else {
					togglePlay();
				}
			}

			public void mouseUp(MouseEvent arg0) {
			}
		});
		playerBass.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent aEvt) {
			}

			public void mouseDown(MouseEvent aEvt) {
				int aClickWhat = playerBass.clickWhat(aEvt.x, aEvt.y);
				if(aClickWhat == 1) {//Seek
					clickSeek(aEvt.x, aEvt.y);
				}
				else if(aClickWhat == 2) {//Gain
					playerBass.click(aEvt.x, aEvt.y);
				}
				else {
					togglePlay();
				}
			}

			public void mouseUp(MouseEvent arg0) {
			}
		});
		playerOther.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent aEvt) {
			}

			public void mouseDown(MouseEvent aEvt) {
				int aClickWhat = playerOther.clickWhat(aEvt.x, aEvt.y);
				if(aClickWhat == 1) {//Seek
					clickSeek(aEvt.x, aEvt.y);
				}
				else if(aClickWhat == 2) {//Gain
					playerOther.click(aEvt.x, aEvt.y);
				}
				else {
					togglePlay();
				}
			}

			public void mouseUp(MouseEvent arg0) {
			}
		});
		editor.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent arg0) {
				if(song == null) {
					return;
				}
				try {
					String aContent = editor.getText().replaceAll("\r*\n","\n");
					if(!aContent.equals(editorLastContent)) {
						editorLastContent = aContent;
						song.kaiSrt.srt = aContent;
						song.kaiSrt.srt2Text();
						song.kaiSrt.resyncText2Srt();
						song.kaiSrt.save(song.path+".ksrt.edited");
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
				viewer.screener.needRedraw();
				logoPanel.needRedraw();
			}
			
			@Override
			public void keyPressed(KeyEvent arg0) {
			}
		});
	}
	
	void start() {
		try {
			// Create interface
			createInterface();
			parentKDJ.display.asyncExec(new Runnable() {
				public void run() {
					soundCardVocalsC.select(parentKDJ.mixerIndexToComboIndex(parentKDJ.soundCardJava));
					playerVocals.setDestCard(parentKDJ.soundCardJava);
					soundCardDrumsC.select(parentKDJ.mixerIndexToComboIndex(parentKDJ.soundCardJava));
					playerDrums.setDestCard(parentKDJ.soundCardJava);
					soundCardBassC.select(parentKDJ.mixerIndexToComboIndex(parentKDJ.soundCardJava));
					playerBass.setDestCard(parentKDJ.soundCardJava);
					soundCardOtherC.select(parentKDJ.mixerIndexToComboIndex(parentKDJ.soundCardJava));
					playerOther.setDestCard(parentKDJ.soundCardJava);
				}
			});
		}
		catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
	
	void getCode() {
		Thread aTh = new Thread(new Runnable() {
			@Override
			public void run() {
				KaiClient aKaiClient = new KaiClient(parentKDJ);
				try {
					aKaiClient.getCode();
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
		});
		aTh.start();
	}
	
	void extract() {
		Thread aTh = new Thread(new Runnable() {
			@Override
			public void run() {
				if(parentKDJ.userCode == null || parentKDJ.userCode.trim().isEmpty()) {
					parentKDJ.display.asyncExec(new Runnable() {
						public void run() {
							editor.setText("You do not have a code. Please, ask for it:\n"
									+ "   a- Enter a valid e-mail address, \n"
									+ "   b- Click the 'Get code' button.\n"
									+ "   c- You will receive a code at your e-mail address.\n"
									+ "   d- Copy/paste this code in the 'Code' box.\n");
						}
					});
					return;
				}
				if(songLng == null || songLng.trim().isEmpty() || songLng.equals("??")) {
					parentKDJ.display.asyncExec(new Runnable() {
						public void run() {
							editor.setText("Please, select the language of your song.");
						}
					});
					return;
				}
				parentKDJ.display.asyncExec(new Runnable() {
					public void run() {
						editor.setText("Sending file to the vocals and lyrics extractor...");
					}
				});
				KaiClient aKaiClient = new KaiClient(parentKDJ);
				try {
					final String aRes = aKaiClient.extract(song.path,songLng);
					if(aRes == null) {
						parentKDJ.display.asyncExec(new Runnable() {
							public void run() {
								editor.setText("Unknown server error.");
							}
						});
						return;
					}
					if(aRes.startsWith("Error:")) {
						parentKDJ.display.asyncExec(new Runnable() {
							public void run() {
								editor.setText(aRes);
							}
						});
						return;
					}
					if(aRes.startsWith("TaskId:")) {
						aKaiClient.taskId = aRes.substring("TaskId:".length()).trim();
						parentKDJ.display.asyncExec(new Runnable() {
							public void run() {
								editor.setText("File uploaded.");
							}
						});
					}
					else {
						//Something wrong ?
						parentKDJ.display.asyncExec(new Runnable() {
							public void run() {
								editor.setText(aRes);
							}
						});
						return;
					}
					while(true) {
						try {
							Thread.sleep(5000);
						} catch (Exception e) {
						}
						String aState = aKaiClient.state();
						if(aState == null) {
							parentKDJ.display.asyncExec(new Runnable() {
								public void run() {
									editor.setText("Unknown server error.");
								}
							});
							break;
						}
						if(aState.startsWith("Error:")) {
							parentKDJ.display.asyncExec(new Runnable() {
								public void run() {
									editor.setText(aState);
								}
							});
							break;
						}
						if(aState.startsWith("Processing")) {
							parentKDJ.display.asyncExec(new Runnable() {
								public void run() {
									editor.setText("Processing...");
								}
							});
							continue;
						}
						if(aState.startsWith("Ready")) {
							parentKDJ.display.asyncExec(new Runnable() {
								public void run() {
									editor.setText("Downloading result...");
								}
							});
							aKaiClient.get();
							if(new File(song.path+".ksrt.edited").exists()) {
								new File(song.path+".ksrt.edited").renameTo(
										new File(song.path+".ksrt.edited."+System.currentTimeMillis()));
							}
							song = new SongDescr(song);
							load(song);
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
		});
		aTh.start();
	}
	
	public void load(SongDescr aSong) {
		song = aSong;

		playerVocals.setGain(0.1);
		playerDrums.setGain(0.8);
		playerBass.setGain(0.8);
		playerOther.setGain(0.8);
		
		if(!new File(song.path+".kai").exists()) {
			parentKDJ.display.asyncExec(new Runnable() {
				public void run() {
					if(parentKDJ.userCode == null || parentKDJ.userCode.trim().isEmpty()) {
						editor.setText("Vocals and lyrics need to be extracted:\n"
								+ "\n"
								+ "1) Ask for a code:\n"
								+ "   a- Enter a valid e-mail address, \n"
								+ "   b- Click the 'Get code' button.\n"
								+ "   c- You will receive a code at your e-mail address.\n"
								+ "   d- Copy/paste this code in the 'Code' box.\n"
								+ "\n"
								+ "2) Select the language of your song.\n"
								+ "\n"
								+ "3) Click on the 'Extract' button.\n"
								+ "\n"
								+ "When the extraction will be completed, the lyrics will appear here.");
					}
					else {
						editor.setText("Vocals and lyrics need to be extracted:\n"
								+ "\n"
								+ "1) Select the language of your song.\n"
								+ "\n"
								+ "2) Click on the 'Extract' button.\n"
								+ "\n"
								+ "When the extraction will be completed, the lyrics will appear here.");
					}
				}
			});
		}
		else {
			playerVocals.load(song,song.path+".vocals","VOCALS");
			playerDrums.load(song,song.path+".drums","DRUMS");
			playerBass.load(song,song.path+".bass","BASS");
			playerOther.load(song,song.path+".other","OTHER");
	
			//Start/Stop for a fully initialized state
			togglePlay();
			try {
				Thread.sleep(10);
			} catch (Exception e) {
			}
			togglePlay();
			clickSeek(playerVocals.progessBarR.x, playerVocals.progessBarR.y);
			
			parentKDJ.display.asyncExec(new Runnable() {
				public void run() {
					if(song == null || song.kaiSrt == null || song.kaiSrt.srt == null || song.kaiSrt.srt.isEmpty()) {
						editor.setText("Lyrics not available...");
					}
					else {
						editor.setText(song.kaiSrt.srt.replaceAll("\r*\n","\n"));
					}
				}
			});
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
