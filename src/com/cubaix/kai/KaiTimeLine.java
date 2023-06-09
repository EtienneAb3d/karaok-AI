package com.cubaix.kai;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import com.cubaix.kaiDJ.KaiDJ;
import com.cubaix.kaiDJ.db.SongDescr;
import com.cubaix.kaiDJ.swt.TimedCanvas;


public class KaiTimeLine extends TimedCanvas {
	
	//Some final values to paint the time line
	private static final int SCALE_BAR_HEIGHT = 40;
	private static final int CHUNK_HEIGHT = 66;
	private static final int FIRST_LINE = SCALE_BAR_HEIGHT+CHUNK_HEIGHT;
	private static final int SECOND_LINE = SCALE_BAR_HEIGHT+ CHUNK_HEIGHT*2;
	
	private static final int MAX_SCALE_FACTOR = 60;
	private static final int MIN_SCALE_FACTOR = 10;
	private static final int INCREMENT_SCALE_FACTOR = 10;

	private KaiEditor parentKE = null;

	private Image dblBuf = null;
	private GC dblBufGC;
	
	private Long tAlpha;
	
	//ScaleBar Time in ms
	private Long tStart = (long) -1;
	
	private Long tStartChunk = (long) -1;
	private Long tEndChunk = (long) -1;
	
	private Long scaleFactor;// ms/px

	private int currentKaiIdx = 0;
	
	private SongDescr song;
	private Rectangle timeLineBounds;

	private Long xMainChunk = (long) -1;
	private Long widthMainChunk = (long) -1;

	private int xCurrentMousePos = -1;

	private boolean mainChunkIsClicked = false;
	private boolean markOneIsClicked = false;
	private boolean markTwoIsClicked = false;
	
	private Rectangle mainTimestamp;
	
	//Menu
	private final Rectangle previousButton = new Rectangle(0,240-31,30,30); //240 time line height
	private final Rectangle nextButton = new Rectangle(31,240-31,30,30);
	private final Rectangle minusScaleFactorButton = new Rectangle(62,240-31,30,30);
	private final Rectangle plusScaleFactorButton = new Rectangle(164,240-31,30,30);
	
	private boolean previousButIsClicked = false;
	private boolean nextButIsClicked = false;
	private boolean plusButIsClicked = false;
	private boolean minusButIsClicked = false;
	
	private final int[] previousTriangle = new int[] {6,240-15, 22,240-25, 22,240-5};
	private final int[] nextTriangle = new int[] {31+30-8,240-15, 31+7,240-25, 31+7,240-5};
	
	private int[] currentPlayTimeTriangle;
	Long aPlayerCurrentPosMS;
	
	private int indexToGo = -1;

	public KaiTimeLine(KaiEditor parentKE, Composite parent, int style) {
		super(parentKE.parentKDJ, parent, style);
		this.parentKE = parentKE;
		
		refreshRate = 50;
		scaleFactor = (long) 20;
		mainTimestamp = new Rectangle(0, FIRST_LINE, 0, CHUNK_HEIGHT);
		currentPlayTimeTriangle = new int[] {0,SCALE_BAR_HEIGHT-2 , -5,SCALE_BAR_HEIGHT-12, 5,SCALE_BAR_HEIGHT-12};
		aPlayerCurrentPosMS = parentKE.playerVocals.getPositionMs();
		
		createListeners();
	}
	
	public void timestampClickHandler(Long timeMS, Long timeEndMS) {
		this.currentKaiIdx = song.kaiSrt.newGetChunkIdx(timeMS, timeEndMS);
		needRedraw(2);
	}
	
	//default Scale factor is 20
	public void calculateMainChunkBounds() {
		song = parentKE.song;
		//Checking for the first time if chunks are available (in case of an empty file at the start)
		if(song.kaiSrt == null || song.kaiSrt.chunks.size() == 0) currentKaiIdx = -1;
		
		//Cheking if the song is available for karaok-ai
		if(currentKaiIdx != -1) {
			Long aAverageChunkDuration;
			
			tStartChunk = song.kaiSrt.chunks.get(currentKaiIdx).getStartTime();
			tEndChunk = song.kaiSrt.chunks.get(currentKaiIdx).getEndTime();
			
			aAverageChunkDuration = (tEndChunk-tStartChunk) / 2; //duration in ms
			
			tAlpha = (long) (timeLineBounds.width / 2);
			
			tStart = (long) ((tStartChunk + aAverageChunkDuration) - tAlpha * scaleFactor); // ms
			if(tStart < 0) tStart = (long) 0;
			
			xMainChunk  = (tStartChunk / scaleFactor)-(tStart / scaleFactor);
			widthMainChunk = ((tEndChunk / scaleFactor)-(tStart / scaleFactor)) - xMainChunk;
			
			mainTimestamp.x = xMainChunk.intValue();
			mainTimestamp.width = widthMainChunk.intValue();
			
			int aPlayerCurrentPosPxl = (int) ((aPlayerCurrentPosMS / scaleFactor)-(tStart / scaleFactor));
			currentPlayTimeTriangle = new int[] {aPlayerCurrentPosPxl,SCALE_BAR_HEIGHT-2 , aPlayerCurrentPosPxl-5,SCALE_BAR_HEIGHT-12, aPlayerCurrentPosPxl+5,SCALE_BAR_HEIGHT-12};
			
		} else {
			tStart = (long) 0;
		}
	}

	private void createListeners() {
		final KaiTimeLine aThis = this;
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent aPE) {
				
				timeLineBounds = getClientArea();
				calculateMainChunkBounds();
				
				GC aPlGC = new GC(aThis);
				paintDbl(aPlGC);
				aPlGC.dispose();
				needRedraw();
			}
		});
		addMouseListener(new MouseListener() {
			/**
			 * This function is at the beginning of all time line interactions.
			 * It mainly trigger a function to check if the click event coordinates are on an interactive element.
			 * Save the current mouse position and redraw the time line.
			 * @param aME
			 */
			@Override
			public void mouseDown(MouseEvent aME) {
				if (song.kaiSrt != null && currentKaiIdx != -1 && isSomethingIsClicked(aME)) {
					xCurrentMousePos = aME.x;
					needRedraw();
				}
			}
			
			/**
			 * Looking if some dynamic content has been clicked and updating concerned boolean to activate MouseEventListener
			 * 
			 * @param MouseEvent aME 
			 * @return boolean
			 */
			private boolean isSomethingIsClicked(MouseEvent aME) {
				if (
				//Main Chunk
				aME.x >= mainTimestamp.x && aME.x <= mainTimestamp.x + mainTimestamp.width && 
				aME.y >= FIRST_LINE && aME.y <= SECOND_LINE) {
					aThis.setCursor(new Cursor(parentKDJ.display,SWT.CURSOR_SIZEALL));
					mainChunkIsClicked = true;
					return true;
				} else if (
						//Ending time handle
						aME.x >= mainTimestamp.x+mainTimestamp.width && aME.x <= mainTimestamp.x + mainTimestamp.width + 6 && 
						aME.y >= FIRST_LINE+16 && aME.y <= FIRST_LINE+16+34) {
					aThis.setCursor(new Cursor(parentKDJ.display,SWT.CURSOR_SIZEE));
					markTwoIsClicked = true;
					return true;
				} else if (
						//Starting time handle
						aME.x >= mainTimestamp.x-6 && aME.x <= mainTimestamp.x && 
						aME.y >= FIRST_LINE+16 && aME.y <= FIRST_LINE+16+34) {
					aThis.setCursor(new Cursor(parentKDJ.display,SWT.CURSOR_SIZEE));
					markOneIsClicked = true;
					return true;
				} else if (
						//Menu previous button
						aME.x >= previousButton.x && aME.x <= previousButton.x + previousButton.width && 
						aME.y >= previousButton.y && aME.y <= previousButton.y + previousButton.height) {
					previousButIsClicked = true;
					return true;
				} else if (
						//Menu next button:
						aME.x >= nextButton.x && aME.x <= nextButton.x + nextButton.width && 
						aME.y >= nextButton.y && aME.y <= nextButton.y + nextButton.height) {
					nextButIsClicked = true;
					return true;
				} else if (
						//Menu plus button:
						aME.x >= plusScaleFactorButton.x && aME.x <= plusScaleFactorButton.x + plusScaleFactorButton.width && 
						aME.y >= plusScaleFactorButton.y && aME.y <= plusScaleFactorButton.y + plusScaleFactorButton.height) {
					plusButIsClicked = true;
					return true;
				} else if (
						//Menu minus button:
						aME.x >= minusScaleFactorButton.x && aME.x <= minusScaleFactorButton.x + minusScaleFactorButton.width && 
						aME.y >= minusScaleFactorButton.y && aME.y <= minusScaleFactorButton.y + minusScaleFactorButton.height) {
					minusButIsClicked = true;
					return true;
				} else {
					return false;
				}
			}
			
			/**
			 * This function is at the end of all time line interactions.
			 * Applying changes when mouse is up and reseting booleans
			 * @param aME
			 */
			@Override
			public void mouseUp(MouseEvent aME) {	
				if(previousButIsClicked || nextButIsClicked || plusButIsClicked || minusButIsClicked) {
					
					menuEventhandler();
					
					needRedraw();
				}else if(mainChunkIsClicked || markOneIsClicked || markTwoIsClicked) {
					aThis.setCursor(new Cursor(parentKDJ.display,SWT.CURSOR_ARROW));
					
					try {
						editFile();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					mainChunkIsClicked = false;
					markOneIsClicked = false;
					markTwoIsClicked = false;
					
					needRedraw();
					xCurrentMousePos = -1;
				}
			}
			/**
			 * Modifying text editor content before saving modification on file (happens when a chunk is modified in the time line)
			 * @throws Exception
			 */
			protected void editFile() throws Exception {
				String newLine = "";
				if (mainChunkIsClicked) {
					newLine = ChunkStr.getTimeMSToFormatTimestamp((tStart/scaleFactor+mainTimestamp.x)*scaleFactor)+" --> "+ChunkStr.getTimeMSToFormatTimestamp((tStart/scaleFactor+mainTimestamp.x+mainTimestamp.width)*scaleFactor);
				}
				if (markOneIsClicked) {
					newLine = ChunkStr.getTimeMSToFormatTimestamp((tStart/scaleFactor+mainTimestamp.x)*scaleFactor)+" --> "+ChunkStr.getTimeMSToFormatTimestamp(song.kaiSrt.chunks.get(currentKaiIdx).getEndTime());
				}
				if (markTwoIsClicked) {
					newLine = ChunkStr.getTimeMSToFormatTimestamp(song.kaiSrt.chunks.get(currentKaiIdx).getStartTime())+" --> "+ChunkStr.getTimeMSToFormatTimestamp((tStart/scaleFactor+mainTimestamp.x+mainTimestamp.width)*scaleFactor);
				}
				
				parentKE.editor.setSelection(song.kaiSrt.chunks.get(currentKaiIdx).editorTimestampLine[0], song.kaiSrt.chunks.get(currentKaiIdx).editorTimestampLine[1]);
				
				if(newLine != "") parentKE.editor.insert(newLine);
				
				String aContent = parentKE.editor.getText().replaceAll("\r*\n","\n");
				
				parentKE.editorContentHistory.add(aContent);
				int aVPos = parentKE.editor.getVerticalBar().getSelection();
				int aHPos = parentKE.editor.getHorizontalBar().getSelection();
				parentKE.editorHVPosHistory.add(new int[] {aVPos,aHPos});
				
				song.kaiSrt.srt = aContent;
				song.kaiSrt.srt2Text();
				song.kaiSrt.resyncText2Srt();
				song.kaiSrt.save(song.path+".ksrt.edited");
				song.kaiSrt.setLinesChunk(parentKE.editor.getText());
			}
			/**
			 * updating chunk index to display and music players
			 */
			private void menuEventhandler() {
				if(previousButIsClicked) {
					if (currentKaiIdx>0) {
						currentKaiIdx--;
						if(parentKE.currentlySeeking) {
							enterInQueue();
						} else {
							parentKE.seek(song.kaiSrt.chunks.get(currentKaiIdx).getStartTime());
						}
						needRedraw(2);
					}
					parentKE.editor.setSelection(song.kaiSrt.chunks.get(currentKaiIdx).editorTimestampLine[0], song.kaiSrt.chunks.get(currentKaiIdx).editorTimestampLine[1]);
					previousButIsClicked = false;
				}else if (nextButIsClicked){//nextBut
					if (currentKaiIdx<song.kaiSrt.chunks.size()-1) {
						currentKaiIdx++;
						if(parentKE.currentlySeeking) {
							enterInQueue();
						} else {
							parentKE.seek(song.kaiSrt.chunks.get(currentKaiIdx).getStartTime());
						}
						needRedraw(2);
					}
					parentKE.editor.setSelection(song.kaiSrt.chunks.get(currentKaiIdx).editorTimestampLine[0], song.kaiSrt.chunks.get(currentKaiIdx).editorTimestampLine[1]);
					nextButIsClicked = false;
				} else if (plusButIsClicked) {
					increaseZ();
					plusButIsClicked = false;
				} else if (minusButIsClicked) {
					decreaseZ();
					minusButIsClicked = false;
				}
			}
			/**
			 * This is done in case Threads players are currently seeking another time.
			 * It save the index to go when the players will be available.
			 */
			private void enterInQueue() {
				indexToGo = currentKaiIdx;
			}
			
			@Override
			public void mouseDoubleClick(MouseEvent aME) {}
		});
		/**
		 * This thread is listening indxToGo value and updating players time when players will be available (to avoid any desynchronisation).
		 */
		Thread aSeekTrackTh = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!aThis.isDisposed()) {
					try {
						if(indexToGo != -1 && !parentKE.currentlySeeking) {
							System.out.println("seeking chunk : " + indexToGo);
							parentKE.seek(song.kaiSrt.chunks.get(indexToGo).getStartTime());
							indexToGo = -1;
						}
						Thread.sleep(5);
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
			}
		});
		aSeekTrackTh.start();
		
		/**
		 * Listening mouse moves only when the chunk or handles are clicked
		 * and manage new chunk position
		 * 
		 * Constraints :
		 * 	- Chunk cannot be moved out of the time line bounds
		 *  - Starting time handle cannot be moved after the Ending time handle (and vice versa)
		 */
		addMouseMoveListener(new MouseMoveListener() {
			private int newPos;
			private int newSize;
			
			@Override
			public void mouseMove(MouseEvent aME) {
				if(mainChunkIsClicked) {
					newPos = mainTimestamp.x + aME.x-xCurrentMousePos;
					//restricting position to fit into windows limits
					mainTimestamp.x = (newPos < 0) ? 
							0 : (newPos + mainTimestamp.width > timeLineBounds.width-1) ?
									timeLineBounds.width-mainTimestamp.width-1 : newPos;
					needRedraw();
					xCurrentMousePos = aME.x;
				}
				
				if(markTwoIsClicked) {
					newPos = mainTimestamp.width + aME.x-xCurrentMousePos;
					
					mainTimestamp.width = (newPos < 0) ?
							0 : (mainTimestamp.x + newPos > timeLineBounds.width-1) ?
									timeLineBounds.width - mainTimestamp.x-1 : newPos;
					needRedraw();
					xCurrentMousePos = aME.x;
				}
				
				if(markOneIsClicked) {
					newPos = mainTimestamp.x + (aME.x-xCurrentMousePos);
					newSize = mainTimestamp.width - (aME.x-xCurrentMousePos);
					
					if(newPos >= 0 && newSize >= 0) {
						mainTimestamp.x = newPos;
						mainTimestamp.width = newSize;
					}
					needRedraw();
					xCurrentMousePos = aME.x;
				}
			}
		});
		
		//Dynamic time line 
		Thread aTrackTh = new Thread(new Runnable() {
			@Override
			public void run() {
				//voir si le temps a changer, avec le stockage du temps précedant
				while(!aThis.isDisposed()) {
					if(aPlayerCurrentPosMS != parentKE.playerVocals.getPositionMs()) {
						int aPlayerCurrentPosPxl = (int) ((aPlayerCurrentPosMS / scaleFactor)-(tStart / scaleFactor));
						currentPlayTimeTriangle = new int[] {aPlayerCurrentPosPxl,SCALE_BAR_HEIGHT-2 , aPlayerCurrentPosPxl-5,SCALE_BAR_HEIGHT-12, aPlayerCurrentPosPxl+5,SCALE_BAR_HEIGHT-12};
						aPlayerCurrentPosMS = parentKE.playerVocals.getPositionMs();
						needRedraw();
					}
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		aTrackTh.start();
	}

	@Override
	protected void paintTimed() {
		
		if (KaiDJ._DEBUG_PAINT) {
			System.out.println("KAITIMELINE.paintTimed()");
		}
		try {
			Rectangle aRectDbl = null;
			if (dblBuf != null) {
				aRectDbl = dblBuf.getBounds();
			}
			
			// Windows expanding
			if (dblBuf == null || timeLineBounds.width != aRectDbl.width || timeLineBounds.height != aRectDbl.height) {
				if (dblBuf != null) {
					dblBuf.dispose();
					dblBuf = null;
					dblBufGC.dispose();
				}
				dblBuf = new Image(parentKDJ.display, timeLineBounds);
				dblBufGC = new GC(dblBuf);
			}
			
			dblBufGC.setClipping(timeLineBounds);
			dblBufGC.setForeground(parentKDJ.whiteC);
			dblBufGC.setBackground(parentKDJ.mainBckC);
			dblBufGC.setFont(parentKDJ.kaiFont);
			
			//Drawing scaleBar
			dblBufGC.fillRectangle(-1, -1, timeLineBounds.width+1, SCALE_BAR_HEIGHT);
			dblBufGC.drawRectangle(-1, -1, timeLineBounds.width+1, SCALE_BAR_HEIGHT);
			dblBufGC.setClipping(-1, -1, timeLineBounds.width+1, SCALE_BAR_HEIGHT);
			
			for (int i = 0; i < timeLineBounds.width; i++) {
				if (i % 25 == 0) dblBufGC.drawLine(i, SCALE_BAR_HEIGHT, i, 35);
				if (i % 100 == 0) {
					dblBufGC.drawLine(i, SCALE_BAR_HEIGHT, i, 25);
					dblBufGC.drawText(ChunkStr.getTimeFormatFromMs(tStart + i*scaleFactor), i, 0);
				}
			}
			
			//Drawing under scale bar content
			dblBufGC.setClipping(0, SCALE_BAR_HEIGHT, timeLineBounds.width+1, 200);
			
			//Background
			dblBufGC.setForeground(parentKDJ.blackC);
			dblBufGC.setBackground(parentKDJ.secondBckC);
			dblBufGC.fillRectangle(0, SCALE_BAR_HEIGHT, timeLineBounds.width, CHUNK_HEIGHT);
			dblBufGC.setBackground(parentKDJ.mainBckC);
			dblBufGC.fillRectangle(0, FIRST_LINE, timeLineBounds.width, CHUNK_HEIGHT);
			dblBufGC.setBackground(parentKDJ.secondBckC);
			dblBufGC.fillRectangle(0, SECOND_LINE, timeLineBounds.width, CHUNK_HEIGHT+2);
			
			dblBufGC.setClipping(timeLineBounds);
			
			if(song.kaiSrt != null && currentKaiIdx >= 0) {
				
				dblBufGC.setBackground(parentKDJ.logoLightC);
				dblBufGC.setForeground(parentKDJ.blackC);
				dblBufGC.setFont(parentKDJ.initialFont);
				
				//Checking to draw the chunk before
				if (currentKaiIdx > 0) {
					int i = currentKaiIdx-1;
					int xChunkBefore = (int) ((song.kaiSrt.chunks.get(i).getStartTime() / scaleFactor)-(tStart / scaleFactor));
					int widthChunkBefore = (int) ((song.kaiSrt.chunks.get(i).getEndTime() / scaleFactor)-(tStart / scaleFactor)) - xChunkBefore;
					Rectangle chunkBefore = new Rectangle(xChunkBefore, SCALE_BAR_HEIGHT , widthChunkBefore, CHUNK_HEIGHT);
					
					dblBufGC.setClipping(chunkBefore);
					dblBufGC.fillRectangle(chunkBefore);
					
					if(chunkBefore.x < -20) {
						dblBufGC.drawText(song.kaiSrt.chunks.get(i).getText(), -20, chunkBefore.y);
					} else {
						dblBufGC.drawText(song.kaiSrt.chunks.get(i).getText(), chunkBefore.x+1, chunkBefore.y);
					}
				}
				
				//Checking to draw the chunk after
				if (currentKaiIdx < song.kaiSrt.chunks.size()-1) {
					int i = currentKaiIdx+1;
					int xChunkAfter = (int) ((song.kaiSrt.chunks.get(i).getStartTime() / scaleFactor)-(tStart / scaleFactor));
					int widthChunkAfter = (int) ((song.kaiSrt.chunks.get(i).getEndTime() / scaleFactor)-(tStart / scaleFactor)) - xChunkAfter;
					
					Rectangle chunkBefore = new Rectangle(xChunkAfter, SECOND_LINE , widthChunkAfter, CHUNK_HEIGHT);
					dblBufGC.setClipping(chunkBefore);
					dblBufGC.fillRectangle(chunkBefore);
					dblBufGC.drawText(song.kaiSrt.chunks.get(i).getText(), chunkBefore.x+1, chunkBefore.y);
				}
				
				//Drawing main chunk rectangle
				dblBufGC.setClipping(timeLineBounds);
				dblBufGC.setForeground(parentKDJ.whiteC);
				
				//Red Hook
				dblBufGC.drawRectangle(mainTimestamp.x - 6, FIRST_LINE+16, 6, 34);
				dblBufGC.setClipping(mainTimestamp.x + mainTimestamp.width, FIRST_LINE, 50, mainTimestamp.height);
				dblBufGC.drawRectangle(mainTimestamp.x+ mainTimestamp.width-6, FIRST_LINE+16, 12, 34);

				dblBufGC.setBackground(parentKDJ.logoDarkC);
				dblBufGC.setForeground(parentKDJ.whiteC);
				dblBufGC.setClipping(mainTimestamp);
				dblBufGC.fillRectangle(mainTimestamp);
				if(mainTimestamp.x < -10) {
					dblBufGC.drawText(song.kaiSrt.chunks.get(currentKaiIdx).getText(), -10, mainTimestamp.y);
				} else {
					dblBufGC.drawText(song.kaiSrt.chunks.get(currentKaiIdx).getText(), mainTimestamp.x+1, mainTimestamp.y);
				}
				
				//Drawing red limits
				dblBufGC.setClipping(timeLineBounds);
				dblBufGC.setForeground(parentKDJ.whiteC);
				dblBufGC.drawLine(mainTimestamp.x, SECOND_LINE-1, mainTimestamp.x, SCALE_BAR_HEIGHT);
				dblBufGC.drawLine(mainTimestamp.x + mainTimestamp.width, SECOND_LINE-1, mainTimestamp.x + mainTimestamp.width, SCALE_BAR_HEIGHT);
				
				//Current play time
				dblBufGC.setBackground(parentKDJ.redC);
				dblBufGC.setForeground(parentKDJ.redC);
				dblBufGC.fillPolygon(currentPlayTimeTriangle);
				dblBufGC.drawLine(currentPlayTimeTriangle[0], SCALE_BAR_HEIGHT, currentPlayTimeTriangle[0], timeLineBounds.height);
				
				dblBufGC.setForeground(parentKDJ.whiteC);
				dblBufGC.setBackground(parentKDJ.mainBckC);
				if (markTwoIsClicked || mainChunkIsClicked) dblBufGC.drawText(ChunkStr.getTimeFormatFromMs((tStart/scaleFactor+mainTimestamp.x+mainTimestamp.width)*scaleFactor),mainTimestamp.x+mainTimestamp.width + 10, FIRST_LINE+5 );
				if (markOneIsClicked || mainChunkIsClicked)	dblBufGC.drawText(ChunkStr.getTimeFormatFromMs((tStart/scaleFactor+mainTimestamp.x)*scaleFactor),mainTimestamp.x-57, FIRST_LINE+5 );
				
				//Menu
				dblBufGC.setForeground(parentKDJ.secondBckC);
				dblBufGC.setBackground(parentKDJ.grayC);
				//next and previous buttons
				dblBufGC.fillRectangle(previousButton);
				//dblBufGC.drawPolygon(previousTriangle);
				dblBufGC.fillRectangle(nextButton);
				//dblBufGC.drawPolygon(nextTriangle);
				
				//Drawing scale factor buttons
				dblBufGC.fillRectangle(minusScaleFactorButton);
				dblBufGC.fillRectangle(plusScaleFactorButton);
				// + and -
				dblBufGC.setBackground(parentKDJ.mainBckC);
				dblBufGC.fillRectangle(62+5,240-18,20,5);
				dblBufGC.fillRectangle(plusScaleFactorButton.x+5,240-18,20,5);
				dblBufGC.fillRectangle(plusScaleFactorButton.x+12,plusScaleFactorButton.y+5,5,20);
				
				dblBufGC.fillPolygon(nextTriangle);
				dblBufGC.fillPolygon(previousTriangle);
				
				//writing scale factor info
				dblBufGC.setClipping(93,209,70,30);
				dblBufGC.setBackground(parentKDJ.logoLightC);
				dblBufGC.drawText("Scale Factor :", 93, 208);
				dblBufGC.drawText(getZ()+" ms / px", 100, 223);
				
			}
			
			// Draw final image
			GC aPlGC = new GC(this);
			paintDbl(aPlGC);
			aPlGC.dispose();
			
		} catch (Throwable t) {
			System.err.println("Can't paint screen : " + t);
			t.printStackTrace(System.err);
		}
	}

	void paintDbl(GC aPlayerGC) {
		if (dblBuf == null) {
			return;
		}
		aPlayerGC.setClipping(timeLineBounds.x, timeLineBounds.y, timeLineBounds.width, timeLineBounds.height);
		aPlayerGC.setForeground(parentKDJ.blackC);
		aPlayerGC.drawImage(dblBuf, timeLineBounds.x, timeLineBounds.y);
	}
	
	
	/**
	 * Different type of events before a new redraw :
	 * - 1 for Editor modifications
	 * - 2 for a Scale factor modification
	 * - 3 for initialization
	 * @param int
	 */
	public void needRedraw(int event) {
		switch (event) {
		// redrawing after key press event on the text editor
		case 1:
			checkForCurrentChunkIndex();
			break;
		case 2:
			break;
		case 3:
			currentKaiIdx = 0;
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + event);
		}
		calculateMainChunkBounds();
		super.needRedraw();
	}
	
	/**
	 * This is done in case a time stamp is manually modified in the text editor
	 * It check if the current chunk index is still in bounds of the kaiStr chunk vector
	 */
	private void checkForCurrentChunkIndex() {
		int chunksSize = song.kaiSrt.chunks.size();
		if(chunksSize > 0) {
			if(currentKaiIdx > chunksSize-1) currentKaiIdx = chunksSize-1;
			if(currentKaiIdx < 0) currentKaiIdx = 0;
		} else {
			//Here no more chunks are available in kaiStr (no more time stamp in kaiStr files)
			currentKaiIdx = -1;
			
		}
	}
	
	/**
	 * Scale factor section
	 */

	public Long getZ() {
		return scaleFactor;
	}

	public void setZ(Long z) {
		scaleFactor = z;
		needRedraw(2);
	}
	
	private void increaseZ() {
		long z = getZ();
		if (z < MAX_SCALE_FACTOR) setZ(z+=INCREMENT_SCALE_FACTOR);
	}
	
	private void decreaseZ() {
		long z = getZ();
		if (z > MIN_SCALE_FACTOR) setZ(z-=INCREMENT_SCALE_FACTOR);
	}
}

