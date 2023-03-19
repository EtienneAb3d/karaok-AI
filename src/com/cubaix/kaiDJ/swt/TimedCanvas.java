package com.cubaix.kaiDJ.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.cubaix.kaiDJ.KaiDJ;

public class TimedCanvas extends Canvas {
	protected KaiDJ parentKDJ;
	private Thread paintThread = null;
	private boolean needRedraw = true;
	//EM 02/11/2008
	private boolean needRedrawSlow = true;
	
	private String toolTip = null;
	
	//EM 12/11/2008
	protected int refreshRate = 100;
	
	public TimedCanvas(KaiDJ aParentKDJ, Composite parent, int style) {
		super(parent, style);
		parentKDJ = aParentKDJ;
		this.addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(MouseEvent arg0) {
				parentKDJ.logoPanel.help = toolTip;
				parentKDJ.logoPanel.needRedraw();
			}

			public void mouseExit(MouseEvent arg0) {
				parentKDJ.logoPanel.help = null;
				parentKDJ.logoPanel.needRedraw();
			}

			public void mouseHover(MouseEvent arg0) {
			}
		});
	}

	/**
	 * 
	 */
	public void needRedraw() {
		needRedraw = true;
		checkRedrawThread();
	}

	//EM 02/11/2008
	public void needRedrawSlow() {
		needRedrawSlow = true;
		checkRedrawThread();
	}

	//EM 02/11/2008 : needRedrawSlow
	public void checkRedrawThread(){
		if (paintThread == null || !paintThread.isAlive()) {
			// Create a new paint thread
			final TimedCanvas aThis = this;
			paintThread = new Thread(new Runnable() {
				public void run() {
					//EM 02/11/2008 : needRedrawSlow
					long aCount = 0;

					while (true) {
						try {
							// Be time respectuous
							Thread.sleep(refreshRate);
							aCount++;
							// Search for a possible change in interface
							if (aThis.paintThread != Thread.currentThread() || aThis.isDisposed()) {
								// Something has changed ??
								break;
							}
							// Is there a request to draw ?
							if (needRedraw //
									//EM 02/11/2008 : needRedrawSlow
									|| (needRedrawSlow && (aCount%10) == 0)) {
								// Immediatly take request into account
								needRedraw = false;
								needRedrawSlow = false;
								aCount = 0;
								
								if (parentKDJ.display == null || parentKDJ.display.isDisposed()) {
									// Wait until it is available
									continue;
								}
								// Use SWT async
								parentKDJ.display.asyncExec(new Runnable() {
									public void run() {
										try {
											paintTimed();
										} 
										catch (Throwable t) {
											t.printStackTrace(System.err);
										}
									}
								});
							}
						} catch (Throwable t) {
							System.err.println("Display thread error (shutdown ?) : " + t);
						}
					}
				}
			}, "Display daemon {" + this.getClass().getName() + "}");
			paintThread.setPriority(Thread.MIN_PRIORITY);
			paintThread.start();
		}
	}

	@Override
	public void drawBackground(GC gc, int x, int y, int width, int height, int offsetX, int offsetY) {
	}

	/**
	 * 
	 */
	protected void paintTimed() {
	}

	public String getToolTip() {
		return toolTip;
	}

	public void setToolTip(String toolTip) {
		this.toolTip = toolTip;
	}
}
