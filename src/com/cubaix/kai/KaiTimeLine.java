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

	private KaiEditor parentKE = null;

	private Image dblBuf = null;
	private GC dblBufGC;
	
	private Long Talpha;
	
	//ScaleBar Time in ms
	private Long Tstart = (long) -1;
	
	private Long TstartChunk = (long) -1;
	private Long TendChunk = (long) -1;
	
	private Long Z;// ms/px

	private int currentKaiIdx = 0;
	
	private SongDescr song;
	private Rectangle timeLineBounds;

	private Long xMainChunk = (long) -1;
	private Long widthMainChunk = (long) -1;

	private int xCurrentMousePos = -1;

	private boolean mainChunkIsClicked = false;
	private boolean markOneIsClicked = false;
	private boolean markTwoIsClicked = false;
	
	private Rectangle maintimeStamp;
	
	//Some final values to paint the time line
	private final int scaleBarHeight = 40;
	private final int chunkHeight = 66;
	private final int firstLine = scaleBarHeight+chunkHeight;
	private final int secondLine = scaleBarHeight+ chunkHeight*2;
	
	//Menu
	private final Rectangle previousButton = new Rectangle(0,240-31,30,30); //240 height of the timeline
	private final Rectangle nextButton = new Rectangle(31,240-31,30,30);
	private final Rectangle minusScaleFactorButton = new Rectangle(62,240-31,30,30);
	private final Rectangle plusScaleFactorButton = new Rectangle(164,240-31,30,30);
	
	private boolean previousButIsClicked = false;
	private boolean nextButIsClicked = false;
	private boolean plusButIsClicked = false;
	private boolean minusButIsClicked = false;
	
	private final int[] previousTriangle = new int[] {6,240-15, 22,240-25, 22,240-5};
	private final int[] nextTriangle = new int[] {31+30-8,240-15, 31+7,240-25, 31+7,240-5};
	
	private int indexToGo = -1;
	
	private final int MAX_SCALE_FACTOR = 60;
	private final int MIN_SCALE_FACTOR = 10;
	private final int INCREMENT_SCALE_FACTOR = 10;

	public KaiTimeLine(KaiEditor parentKE, Composite parent, int style) {
		super(parentKE.parentKDJ, parent, style);
		this.parentKE = parentKE;
		refreshRate = 50;
		Z = (long) 20;
		maintimeStamp = new Rectangle(0, firstLine, 0, chunkHeight);
		
		createListeners();
	}
	
	public void timestampClickHandler(Long timeMS, Long timeEndMS) {
		this.currentKaiIdx = song.kaiSrt.newGetChunkIdx(timeMS, timeEndMS);
		needRedraw("recalculateZ");
	}
	
	//Scale factor default Z = 20
	public void calculateMainChunkBounds() {
		song = parentKE.song;
		//Checking for the first time if chunks are available (in case of an empty file at the start)
		if(song.kaiSrt == null || song.kaiSrt.chunks.size() == 0) currentKaiIdx = -1;
		
		//Cheking if the song is available for karaok-ai
		if(currentKaiIdx != -1) {
			Long duréeMoyenneChunk;
			
			TstartChunk = song.kaiSrt.chunks.get(currentKaiIdx).getStartTime();
			TendChunk = song.kaiSrt.chunks.get(currentKaiIdx).getEndTime();
			
			duréeMoyenneChunk = (TendChunk-TstartChunk) / 2; //duration in ms
			
			Talpha = (long) (timeLineBounds.width / 2);
			
			Tstart = (long) ((TstartChunk + duréeMoyenneChunk) - Talpha * Z); // ms
			if(Tstart < 0) Tstart = (long) 0;
			
			xMainChunk  = (TstartChunk / Z)-(Tstart / Z);
			widthMainChunk = ((TendChunk / Z)-(Tstart / Z)) - xMainChunk;
			
			maintimeStamp.x = xMainChunk.intValue();
			maintimeStamp.width = widthMainChunk.intValue();
			
		} else {
			Tstart = (long) 0;
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
			 * @param arg0
			 */
			@Override
			public void mouseDown(MouseEvent arg0) {
				if (song.kaiSrt != null && currentKaiIdx != -1 && isSomethingIsClicked(arg0)) {
					xCurrentMousePos = arg0.x;
					needRedraw();
				}
			}
			
			/**
			 * Looking if some dynamic content has been clicked and updating concerned boolean to activate MouseEventListener
			 * 
			 * @param MouseEvent arg0 
			 * @return boolean
			 */
			private boolean isSomethingIsClicked(MouseEvent arg0) {
				if (
				//Main Chunk
				arg0.x >= maintimeStamp.x && arg0.x <= maintimeStamp.x + maintimeStamp.width && 
				arg0.y >= firstLine && arg0.y <= secondLine) {
					aThis.setCursor(new Cursor(parentKDJ.display,SWT.CURSOR_SIZEALL));
					mainChunkIsClicked = true;
					return true;
				} else if (
						//Ending time handle
						arg0.x >= maintimeStamp.x+maintimeStamp.width && arg0.x <= maintimeStamp.x + maintimeStamp.width + 6 && 
						arg0.y >= firstLine+16 && arg0.y <= firstLine+16+34) {
					aThis.setCursor(new Cursor(parentKDJ.display,SWT.CURSOR_SIZEE));
					markTwoIsClicked = true;
					return true;
				} else if (
						//Starting time handle
						arg0.x >= maintimeStamp.x-6 && arg0.x <= maintimeStamp.x && 
						arg0.y >= firstLine+16 && arg0.y <= firstLine+16+34) {
					aThis.setCursor(new Cursor(parentKDJ.display,SWT.CURSOR_SIZEE));
					markOneIsClicked = true;
					return true;
				} else if (
						//Menu previous button
						arg0.x >= previousButton.x && arg0.x <= previousButton.x + previousButton.width && 
						arg0.y >= previousButton.y && arg0.y <= previousButton.y + previousButton.height) {
					previousButIsClicked = true;
					return true;
				} else if (
						//Menu next button:
						arg0.x >= nextButton.x && arg0.x <= nextButton.x + nextButton.width && 
						arg0.y >= nextButton.y && arg0.y <= nextButton.y + nextButton.height) {
					nextButIsClicked = true;
					return true;
				} else if (
						//Menu plus button:
						arg0.x >= plusScaleFactorButton.x && arg0.x <= plusScaleFactorButton.x + plusScaleFactorButton.width && 
						arg0.y >= plusScaleFactorButton.y && arg0.y <= plusScaleFactorButton.y + plusScaleFactorButton.height) {
					plusButIsClicked = true;
					return true;
				} else if (
						//Menu minus button:
						arg0.x >= minusScaleFactorButton.x && arg0.x <= minusScaleFactorButton.x + minusScaleFactorButton.width && 
						arg0.y >= minusScaleFactorButton.y && arg0.y <= minusScaleFactorButton.y + minusScaleFactorButton.height) {
					minusButIsClicked = true;
					return true;
				} else {
					return false;
				}
			}
			
			/**
			 * This function is at the end of all time line interactions.
			 * Applying changes when mouse is up and reseting booleans
			 * @param arg0
			 */
			@Override
			public void mouseUp(MouseEvent arg0) {	
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
					newLine = ChunkStr.getTimeMSToFormatTimestamp((Tstart/Z+maintimeStamp.x)*Z)+" --> "+ChunkStr.getTimeMSToFormatTimestamp((Tstart/Z+maintimeStamp.x+maintimeStamp.width)*Z);
				}
				if (markOneIsClicked) {
					newLine = ChunkStr.getTimeMSToFormatTimestamp((Tstart/Z+maintimeStamp.x)*Z)+" --> "+ChunkStr.getTimeMSToFormatTimestamp(song.kaiSrt.chunks.get(currentKaiIdx).getEndTime());
				}
				if (markTwoIsClicked) {
					newLine = ChunkStr.getTimeMSToFormatTimestamp(song.kaiSrt.chunks.get(currentKaiIdx).getStartTime())+" --> "+ChunkStr.getTimeMSToFormatTimestamp((Tstart/Z+maintimeStamp.x+maintimeStamp.width)*Z);
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
						needRedraw("recalculateZ");
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
						needRedraw("recalculateZ");
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
			public void mouseDoubleClick(MouseEvent arg0) {}
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
			public void mouseMove(MouseEvent arg0) {
				if(mainChunkIsClicked) {
					newPos = maintimeStamp.x + arg0.x-xCurrentMousePos;
					//restricting position to fit into windows limits
					maintimeStamp.x = (newPos < 0) ? 
							0 : (newPos + maintimeStamp.width > timeLineBounds.width-1) ?
									timeLineBounds.width-maintimeStamp.width-1 : newPos;
					needRedraw();
					xCurrentMousePos = arg0.x;
				}
				
				if(markTwoIsClicked) {
					newPos = maintimeStamp.width + arg0.x-xCurrentMousePos;
					
					maintimeStamp.width = (newPos < 0) ?
							0 : (maintimeStamp.x + newPos > timeLineBounds.width-1) ?
									timeLineBounds.width - maintimeStamp.x-1 : newPos;
					needRedraw();
					xCurrentMousePos = arg0.x;
				}
				
				if(markOneIsClicked) {
					newPos = maintimeStamp.x + (arg0.x-xCurrentMousePos);
					newSize = maintimeStamp.width - (arg0.x-xCurrentMousePos);
					
					if(newPos >= 0 && newSize >= 0) {
						maintimeStamp.x = newPos;
						maintimeStamp.width = newSize;
					}
					needRedraw();
					xCurrentMousePos = arg0.x;
				}
			}
		});
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
			
			dblBufGC.setForeground(parentKDJ.whiteC);
			dblBufGC.setBackground(parentKDJ.mainBckC);
			dblBufGC.setFont(parentKDJ.kaiFont);
			
			//Drawing scaleBar
			dblBufGC.fillRectangle(-1, -1, timeLineBounds.width+1, scaleBarHeight);
			dblBufGC.drawRectangle(-1, -1, timeLineBounds.width+1, scaleBarHeight);
			dblBufGC.setClipping(-1, -1, timeLineBounds.width+1, scaleBarHeight);
			
			for (int i = 0; i < timeLineBounds.width; i++) {
				if (i % 25 == 0) dblBufGC.drawLine(i, scaleBarHeight, i, 35);
				if (i % 100 == 0) {
					dblBufGC.drawLine(i, scaleBarHeight, i, 25);
					dblBufGC.drawText(ChunkStr.getTimeFormatFromMs(Tstart + i*Z), i, 0);
				}
			}
			
			//Drawing under scale bar content
			dblBufGC.setClipping(0, scaleBarHeight, timeLineBounds.width+1, 200);
			
			//Background
			dblBufGC.setForeground(parentKDJ.blackC);
			dblBufGC.setBackground(parentKDJ.secondBckC);
			dblBufGC.fillRectangle(0, scaleBarHeight, timeLineBounds.width, chunkHeight);
			dblBufGC.setBackground(parentKDJ.mainBckC);
			dblBufGC.fillRectangle(0, firstLine, timeLineBounds.width, chunkHeight);
			dblBufGC.setBackground(parentKDJ.secondBckC);
			dblBufGC.fillRectangle(0, secondLine, timeLineBounds.width, chunkHeight+2);
			
			dblBufGC.setClipping(timeLineBounds);
			
			if(song.kaiSrt != null && currentKaiIdx != -1) {
				
				dblBufGC.setBackground(parentKDJ.logoLightC);
				dblBufGC.setForeground(parentKDJ.blackC);
				dblBufGC.setFont(parentKDJ.initialFont);
				
				//Checking to draw the chunk before
				if (currentKaiIdx > 0) {
					int i = currentKaiIdx-1;
					int xChunkBefore = (int) ((song.kaiSrt.chunks.get(i).getStartTime() / Z)-(Tstart / Z));
					int widthChunkBefore = (int) ((song.kaiSrt.chunks.get(i).getEndTime() / Z)-(Tstart / Z)) - xChunkBefore;
					
					Rectangle chunkBefore = new Rectangle(xChunkBefore, scaleBarHeight , widthChunkBefore, chunkHeight);
					dblBufGC.setClipping(chunkBefore);
					dblBufGC.fillRectangle(chunkBefore);
					dblBufGC.drawText(song.kaiSrt.chunks.get(i).getText(), chunkBefore.x+1, chunkBefore.y);
				}
				
				//Checking to draw the chunk after
				if (currentKaiIdx < song.kaiSrt.chunks.size()-1) {
					int i = currentKaiIdx+1;
					int xChunkAfter = (int) ((song.kaiSrt.chunks.get(i).getStartTime() / Z)-(Tstart / Z));
					int widthChunkAfter = (int) ((song.kaiSrt.chunks.get(i).getEndTime() / Z)-(Tstart / Z)) - xChunkAfter;
					
					Rectangle chunkBefore = new Rectangle(xChunkAfter, secondLine , widthChunkAfter, chunkHeight);
					dblBufGC.setClipping(chunkBefore);
					dblBufGC.fillRectangle(chunkBefore);
					dblBufGC.drawText(song.kaiSrt.chunks.get(i).getText(), chunkBefore.x+1, chunkBefore.y);
				}
				
				//Drawing main chunk rectangle
				dblBufGC.setClipping(timeLineBounds);
				dblBufGC.setForeground(parentKDJ.redC);
				
				//Red Hook
				dblBufGC.drawRectangle(maintimeStamp.x - 6, firstLine+16, 6, 34);
				dblBufGC.setClipping(maintimeStamp.x + maintimeStamp.width, firstLine, 50, maintimeStamp.height);
				dblBufGC.drawRectangle(maintimeStamp.x+ maintimeStamp.width-6, firstLine+16, 12, 34);

				dblBufGC.setBackground(parentKDJ.logoDarkC);
				dblBufGC.setForeground(parentKDJ.whiteC);
				dblBufGC.setClipping(maintimeStamp);
				dblBufGC.fillRectangle(maintimeStamp);
				dblBufGC.drawText(song.kaiSrt.chunks.get(currentKaiIdx).getText(), maintimeStamp.x+1, maintimeStamp.y);
				
				//Drawing red limits
				dblBufGC.setClipping(timeLineBounds);
				dblBufGC.setForeground(parentKDJ.redC);
				dblBufGC.drawLine(maintimeStamp.x, secondLine-1, maintimeStamp.x, scaleBarHeight);
				dblBufGC.drawLine(maintimeStamp.x + maintimeStamp.width, secondLine-1, maintimeStamp.x + maintimeStamp.width, scaleBarHeight);
				
				dblBufGC.setForeground(parentKDJ.whiteC);
				dblBufGC.setBackground(parentKDJ.mainBckC);
				if (markTwoIsClicked || mainChunkIsClicked) dblBufGC.drawText(ChunkStr.getTimeFormatFromMs((Tstart/Z+maintimeStamp.x+maintimeStamp.width)*Z),maintimeStamp.x+maintimeStamp.width + 10, firstLine+5 );
				if (markOneIsClicked || mainChunkIsClicked)	dblBufGC.drawText(ChunkStr.getTimeFormatFromMs((Tstart/Z+maintimeStamp.x)*Z),maintimeStamp.x-57, firstLine+5 );
				
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
	 * Different type of events before a new redraw
	 * @param event
	 */
	public void needRedraw(String event) {
		switch (event) {
		// redrawing after key press event on the text editor
		case "editor":
			checkForCurrentChunkIndex();
			break;
		// recalculating scale factor before redrawing
		case "recalculateZ":
			break;
		case "init":
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
		return Z;
	}

	public void setZ(Long z) {
		Z = z;
		needRedraw("recalculateZ");
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

