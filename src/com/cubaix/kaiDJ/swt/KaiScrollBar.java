package com.cubaix.kaiDJ.swt;

import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.cubaix.kaiDJ.KaiDJ;

public class KaiScrollBar extends Canvas implements MouseWheelListener {
	KaiDJ parentKDJ = null;
	int style = SWT.VERTICAL;
	Composite parent = null;
	
	Vector<SelectionListener> selectionListener = new Vector<SelectionListener>();
	int selection = 0;
	int maximum = 100;
	int thumb = 10;
	int increment = 1;
	int pageIncrement = 10;
	
	Point mouseLastPos = null;
	Point mouseLastDrag = null;
	
	Color thumbC = null;
	Color incrementC = null;
	Color pageIncrementC = null;
	Color borderC = null;
	
	public KaiScrollBar(KaiDJ aParentKDJ,Composite aParent, int aStyle) {
		super(aParent, SWT.None);
		parentKDJ = aParentKDJ;
		style = aStyle;
		parent = aParent;
		
		thumbC = parentKDJ.logoDarkC;
		incrementC = parentKDJ.logoDarkC;
		pageIncrementC = parentKDJ.mainBckC;
		borderC = parentKDJ.logoLightC;
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent aE) {
				paint(aE.gc);
			}
		});
		final KaiScrollBar aMe = this;
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent aE) {
				aMe.mouseScrolled(aE);
			}
		});
		addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent aME) {
				mouseLastDrag = null;
				if(mouseLastPos == null) {
					return;
				}
				if(mouseLastPos.x == aME.x && mouseLastPos.y == aME.y) {
					Rectangle aRect = getBounds();
					if((style & SWT.VERTICAL) > 0) {
						double aScreenRange = aRect.height*(maximum-thumb)/(double)maximum;
						selection = (int)(maximum * (aME.y - aRect.y) / aScreenRange);
						if(selection < 0) {
							selection = 0;
						}
						if(selection > maximum) {
							selection = maximum;
						}
						for(SelectionListener aL : selectionListener) {
							aL.widgetSelected(null);
						}
					}
					else {
						double aScreenRange = aRect.width*(maximum-thumb)/(double)maximum;
						selection = (int)(maximum * (aME.x - aRect.x) / aScreenRange);
						if(selection < 0) {
							selection = 0;
						}
						if(selection > maximum) {
							selection = maximum;
						}
						for(SelectionListener aL : selectionListener) {
							aL.widgetSelected(null);
						}
					}
					redraw();
				}
				mouseLastPos = null;
			}
			@Override
			public void mouseDown(MouseEvent aME) {
				mouseLastPos = new Point(aME.x, aME.y);
			}
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
			}
		});
		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent aME) {
				if(mouseLastPos != null) {
					mouseLastDrag = mouseLastPos;
					mouseLastPos = null;
				}
				if(mouseLastDrag == null) {
					return;
				}

				Rectangle aRect = getBounds();
				if((style & SWT.VERTICAL) > 0) {
					double aScreenRange = aRect.height*(maximum-thumb)/(double)maximum;
					int aDelta = (int)(maximum * (aME.y - mouseLastDrag.y) / aScreenRange);
					if(Math.abs(aDelta) <= 2) {
						//Do nothing on too small move
						return;
					}
					selection += aDelta;
					if(selection < 0) {
						selection = 0;
					}
					if(selection > maximum) {
						selection = maximum;
					}
					for(SelectionListener aL : selectionListener) {
						aL.widgetSelected(null);
					}
				}
				else {
					double aScreenRange = aRect.width*(maximum-thumb)/(double)maximum;
					int aDelta = (int)(maximum * (aME.x - mouseLastDrag.x) / aScreenRange);
					if(Math.abs(aDelta) <= 2) {
						//Do nothing on too small move
						return;
					}
					selection += aDelta;
					if(selection < 0) {
						selection = 0;
					}
					if(selection > maximum) {
						selection = maximum;
					}
					for(SelectionListener aL : selectionListener) {
						aL.widgetSelected(null);
					}
				}
				redraw();
				mouseLastDrag.x = aME.x;
				mouseLastDrag.y = aME.y;
			}
		});
	}
	
	void paint(GC aGC) {
		if(KaiDJ._DEBUG_PAINT) {
			System.out.println("KaiScrollBar.paint()");
		}
		Rectangle aRect = getBounds();
		aGC.setBackground(pageIncrementC);
		aGC.fillRectangle(0,0,aRect.width,aRect.height);
		aGC.setForeground(borderC);

		aGC.setBackground(thumbC);
		int aFreeRange = maximum-thumb;
		
		if((style & SWT.VERTICAL) > 0) {
			aGC.fillRectangle(aRect.width/2,0,1,aRect.height);
			if(aFreeRange <= 0) {
				aGC.fillRectangle(2,0,aRect.width - 3,aRect.height);
			}
			else {
				int aPos = (int)(aRect.height * (selection / (double)maximum) * (aFreeRange / (double)maximum));
				int aThumb = (aRect.height * thumb) / maximum;
				if(aThumb <= 0) {
					aThumb = 2;
				}
				aGC.fillRectangle(2,aPos, aRect.width - 3, aThumb);
			}
		}
		else {
			aGC.fillRectangle(0,aRect.height/2,aRect.width,1);
			if(aFreeRange <= 0) {
				aGC.fillRectangle(0,2,aRect.width,aRect.height-2);
			}
			else {
				int aPos = (int)(aRect.width * (selection / (double)maximum) * (aFreeRange / (double)maximum));
				int aThumb = (aRect.width * thumb) / maximum;
				if(aThumb <= 0) {
					aThumb = 2;
				}
				aGC.fillRectangle(aPos,2,aThumb,aRect.height - 3);
			}
		}
	}
	
	public void setThumbColor(Color aC) {
		thumbC = aC;
	}
	public void setIncrementColor(Color aC) {
		pageIncrementC = aC;
	}
	public void setPageIncrementColor(Color aC) {
		pageIncrementC = aC;
	}
	
	public void addSelectionListener(SelectionListener aSelectionListener) {
		selectionListener.add(aSelectionListener);
	}
	
	public int getSelection() {
		return selection;
	}
	public void setSelection(int aS) {
		selection = aS;
	}
	public int getMaximum() {
		return maximum;
	}
	public void setMaximum(int aM) {
		maximum = aM;
	}
	public int getThumb() {
		return thumb;
	}
	public void setThumb(int aT) {
		thumb = aT;
	}
	public int getIncrement() {
		return increment;
	}
	public void setIncrement(int aI) {
		increment = aI;
	}
	public int getPageIncrement() {
		return pageIncrement;
	}
	public void setPageIncrement(int aI) {
		pageIncrement = aI;
	}

	@Override
	public void mouseScrolled(MouseEvent aME) {
		selection -= (thumb * aME.count)/8;
		if(selection < 0) {
			selection = 0;
		}
		if(selection > maximum) {
			selection = maximum;
		}
		for(SelectionListener aL : selectionListener) {
			aL.widgetSelected(null);
		}
		redraw();
	}
	
}
