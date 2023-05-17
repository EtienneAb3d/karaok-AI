package com.cubaix.kai;

import java.io.PrintStream;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;

import com.cubAIx.WhisperTimeSync.CubaixAlignerSimple;
import com.cubAIx.WhisperTimeSync.Pair;
import com.cubAIx.WhisperTimeSync.TokenizedSent;
import com.cubAIx.WhisperTimeSync.TokenizerSimple;
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
	private Long Tend = (long) -1;
	
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
	
	//Some final values to paint the timeline
	private final int scaleBarHeight = 40;
	private final int chunkHeight = 66;
	private final int firstLine = scaleBarHeight+chunkHeight;
	private final int secondLine = scaleBarHeight+ chunkHeight*2;
	
	private Point editorSelection = null;
	
	//Menu
	private final Rectangle previousButton = new Rectangle(0,240-21,20,20); //240 height of the timeline
	private final Rectangle nextButton = new Rectangle(21,240-21,20,20);
	private final Rectangle musicButton = new Rectangle(42,240-21,20,20);
	
	private boolean previousButIsClicked = false;
	private boolean nextButIsClicked = false;
	private boolean musicButIsClicked = false;
	
	private final int[] previousTriangle = new int[] {4,240-11, 14,240-16, 14,240-6}; 
	private final int[] nextTriangle = new int[] {21+14,240-11, 21+4,240-16, 21+4,240-6};

	public KaiTimeLine(KaiEditor parentKE, Composite parent, int style) {
		super(parentKE.parentKDJ, parent, style);
		this.parentKE = parentKE;
		refreshRate = 50;
		
		maintimeStamp = new Rectangle(0, firstLine, 0, chunkHeight);
		createListeners();
	}
	
	public void timestampClickHandler(Long timeMS, Long timeEndMS, Point editorSelection ) {
		this.currentKaiIdx = song.kaiSrt.newGetChunkIdx(timeMS, timeEndMS);
		this.editorSelection = editorSelection;
		needRedraw("recalculateZ");
	}

	public void calculate() {
		song = parentKE.song;
		Long duréeMoyenneChunk;
		
		//Cheking if the song is available for karaok-ai
		if(song.kaiSrt != null) {
			
			TstartChunk = song.kaiSrt.chunks.get(currentKaiIdx).getStartTime();
			TendChunk = song.kaiSrt.chunks.get(currentKaiIdx).getEndTime();
			
			duréeMoyenneChunk = (TendChunk-TstartChunk) / 2; //duration in ms
			Z = (long) 0; // ms.pxl
			
			//calculating for an appropriate Z
			do {
				Z += 10;
				
				Talpha = (long) (timeLineBounds.width / 2);
				
				Tstart = (long) ((TstartChunk + duréeMoyenneChunk) - Talpha * Z); // ms
				if(Tstart < 0) Tstart = (long) 0;
				
				xMainChunk  = (TstartChunk / Z)-(Tstart / Z);
				widthMainChunk = ((TendChunk / Z)-(Tstart / Z)) - xMainChunk;
				
			} while (widthMainChunk > (timeLineBounds.width /3));
			
			maintimeStamp.x = xMainChunk.intValue();
			maintimeStamp.width = widthMainChunk.intValue();
			
		} else {
			
			Z = (long) 100;
			Tstart = (long) 0;
			
		}
	}

	private void createListeners() {
		final KaiTimeLine aThis = this;
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent aPE) {
				
				timeLineBounds = getClientArea();
				calculate();
				
				GC aPlGC = new GC(aThis);
				paintDbl(aPlGC);
				aPlGC.dispose();
				needRedraw();
			}
		});
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent arg0) {	
				if(previousButIsClicked || nextButIsClicked || musicButIsClicked) {
					
					menuEventhandler();
					
					previousButIsClicked = false;
					nextButIsClicked = false;
					
					needRedraw();
				}else if(mainChunkIsClicked || markOneIsClicked || markTwoIsClicked) {
					aThis.setCursor(new Cursor(parentKDJ.display,SWT.CURSOR_ARROW));
					
					try {
						editFile();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					mainChunkIsClicked = false;
					markOneIsClicked = false;
					markTwoIsClicked = false;
					
					needRedraw();
					xCurrentMousePos = -1;
				}
			}
			
			private void menuEventhandler() {
				if(previousButIsClicked) {
					if (currentKaiIdx>0) {
						currentKaiIdx--;
						needRedraw("recalculateZ");
					}
					parentKE.editor.setSelection(song.kaiSrt.chunks.get(currentKaiIdx).editorTimestampLine[0], song.kaiSrt.chunks.get(currentKaiIdx).editorTimestampLine[1]);
				
				}else if (nextButIsClicked){//nextBut
					if (currentKaiIdx<song.kaiSrt.chunks.size()-1) {
						currentKaiIdx++;
						needRedraw("recalculateZ");
					}
					parentKE.editor.setSelection(song.kaiSrt.chunks.get(currentKaiIdx).editorTimestampLine[0], song.kaiSrt.chunks.get(currentKaiIdx).editorTimestampLine[1]);
					
				}else if (musicButIsClicked && currentKaiIdx != -1){//musicBut
					parentKE.seek(song.kaiSrt.chunks.get(currentKaiIdx).getStartTime());
				}
			}

			@Override
			public void mouseDown(MouseEvent arg0) {
				if (song.kaiSrt != null && isSomethingIsClicked(arg0)) {
					xCurrentMousePos = arg0.x;
					needRedraw();
				}
			}
			
			private boolean isSomethingIsClicked(MouseEvent arg0) {
				if (
				// Checking if mouse coordinates are on the main chunk:
				arg0.x >= maintimeStamp.x && arg0.x <= maintimeStamp.x + maintimeStamp.width && 
				arg0.y >= firstLine && arg0.y <= secondLine) {
					aThis.setCursor(new Cursor(parentKDJ.display,SWT.CURSOR_SIZEALL));
					mainChunkIsClicked = true;
					return true;
				} else if (
						// Checking if mouse coordinates are on the second mark:
						arg0.x >= maintimeStamp.x+maintimeStamp.width && arg0.x <= maintimeStamp.x + maintimeStamp.width + 6 && 
						arg0.y >= firstLine+16 && arg0.y <= firstLine+16+34) {
					aThis.setCursor(new Cursor(parentKDJ.display,SWT.CURSOR_SIZEE));
					markTwoIsClicked = true;
					return true;
				} else if (
						// Checking if mouse coordinates are on the first mark:
						arg0.x >= maintimeStamp.x-6 && arg0.x <= maintimeStamp.x && 
						arg0.y >= firstLine+16 && arg0.y <= firstLine+16+34) {
					aThis.setCursor(new Cursor(parentKDJ.display,SWT.CURSOR_SIZEE));
					markOneIsClicked = true;
					return true;
				} else if (
						// Checking if mouse coordinates are on the previus button:
						arg0.x >= previousButton.x && arg0.x <= previousButton.x + previousButton.width && 
						arg0.y >= previousButton.y && arg0.y <= previousButton.y + previousButton.height) {
					previousButIsClicked = true;
					return true;
				} else if (
						// Checking if mouse coordinates are on the next button:
						arg0.x >= nextButton.x && arg0.x <= nextButton.x + nextButton.width && 
						arg0.y >= nextButton.y && arg0.y <= nextButton.y + nextButton.height) {
					nextButIsClicked = true;
					return true;
				} else if (
						// Checking if mouse coordinates are on the music button:
						arg0.x >= musicButton.x && arg0.x <= musicButton.x + musicButton.width && 
						arg0.y >= musicButton.y && arg0.y <= musicButton.y + musicButton.height) {
					musicButIsClicked = true;
					return true;
				} else {
					return false;
				}
			}
			
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
				
			}
		});
		
		/**
		 * Managing chunk resizing when clicking on it or on handles
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

	protected void editFile() throws Exception {
		String newLine = "";
		if (mainChunkIsClicked) {
			newLine = ChunkStr.getTimeMSToFormatTimestamp((Tstart/Z+maintimeStamp.x)*Z)+" --> "+ChunkStr.getTimeMSToFormatTimestamp((Tstart/Z+maintimeStamp.x+maintimeStamp.width)*Z);
			System.out.println(newLine);
		}
		if (markOneIsClicked) {
			newLine = ChunkStr.getTimeMSToFormatTimestamp((Tstart/Z+maintimeStamp.x)*Z)+" --> "+ChunkStr.getTimeMSToFormatTimestamp(song.kaiSrt.chunks.get(currentKaiIdx).getEndTime());
			System.out.println(newLine);
		}
		if (markTwoIsClicked) {
			newLine = ChunkStr.getTimeMSToFormatTimestamp(song.kaiSrt.chunks.get(currentKaiIdx).getStartTime())+" --> "+ChunkStr.getTimeMSToFormatTimestamp((Tstart/Z+maintimeStamp.x+maintimeStamp.width)*Z);
			System.out.println(newLine);
		}
		parentKE.editor.clearSelection();
		parentKE.editor.setSelection(song.kaiSrt.chunks.get(currentKaiIdx).editorTimestampLine[0], song.kaiSrt.chunks.get(currentKaiIdx).editorTimestampLine[1]);
		
		if(newLine != "") parentKE.editor.insert(newLine);
		parentKE.editor.clearSelection();
		
		song.kaiSrt.srt = parentKE.editor.getText().replaceAll("\r*\n","\n");
		song.kaiSrt.srt2Text();
		song.kaiSrt.resyncText2Srt();
		song.kaiSrt.save(song.path+".ksrt.edited");
		song.kaiSrt.setLinesChunk(parentKE.editor.getText());
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
				dblBufGC.drawPolygon(previousTriangle);
				dblBufGC.fillRectangle(nextButton);
				dblBufGC.drawPolygon(nextTriangle);
				
				dblBufGC.setBackground(parentKDJ.whiteC);
				dblBufGC.fillRectangle(musicButton);
				
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

	@Override
	public void needRedraw() {
		super.needRedraw();
	}
	
	public void needRedraw(String event) {
		
		switch (event) {
		case "editor":
			currentKaiIdx = 0;
			calculate();
			super.needRedraw();
			break;
		case "recalculateZ":
			calculate();
			super.needRedraw();
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + event);
		}
	}
}

