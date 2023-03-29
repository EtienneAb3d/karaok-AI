package com.cubaix.kai;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.cubaix.kaiDJ.KaiDJ;

public class KaiViewer {
	protected KaiEditor parentKE = null;
	
	public Shell shell = null;
	public GC mainGC = null;
	
	KaiScreener screener = null;

	public KaiViewer(KaiEditor aParentKE) {
		parentKE = aParentKE;
		start();
	}
	
	void createInterface() {
		// final jDJ aThis = this;
		shell = new Shell(parentKE.parentKDJ.display, SWT.SHELL_TRIM);
		shell.setText("karaok-AI " + parentKE.parentKDJ._VERSION);
		shell.open();
		mainGC = new GC(shell);
		shell.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent arg0) {
				paint();
			}
		});
		shell.setImage(parentKE.parentKDJ.kaiIcon);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		shell.setLayout(gridLayout);
		
		screener = new KaiScreener(this, shell, SWT.FILL); 
		GridData aGD = new GridData(GridData.FILL_BOTH);
		screener.setLayoutData(aGD);
		screener.setBackground(parentKE.parentKDJ.playerC);
		screener.setForeground(parentKE.parentKDJ.blackC);
		screener.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent arg0) {
				shell.setFullScreen(!shell.getFullScreen());
			}
			
			@Override
			public void mouseDown(MouseEvent arg0) {
			}
			
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
			}
		});
		
		shell.pack();
		// Size
		Rectangle aR = parentKE.parentKDJ.display.getBounds();
		if(aR.width > 1920) {
			aR.width = 1920;
		}
		if(aR.height > 1080) {
			aR.height = 1080;
		}
		aR.width /=2;
		aR.height /=2;
		aR.x += aR.width;
		aR.y += aR.height;
		aR.width -= 20;
		if(aR.y+aR.height > parentKE.parentKDJ.display.getBounds().height - 40) {
			aR.y = parentKE.parentKDJ.display.getBounds().height-aR.height - 40;
		}
		if(KaiDJ._SIZE_FOR_SCREENSHOTS) {
			aR.width = KaiDJ._SIZE_FOR_SCREENSHOTS_W;
			aR.height = KaiDJ._SIZE_FOR_SCREENSHOTS_H;
		}
		shell.setBounds(aR);
	}

	void paint() {
		if(KaiDJ._DEBUG_PAINT) {
			System.out.println("VIEWER.paint()");
		}
		synchronized (mainGC) {
			Rectangle aRect = shell.getBounds();
			mainGC = new GC(shell);
			mainGC.setClipping(0, 0, aRect.width, aRect.height);
			mainGC.setBackground(parentKE.parentKDJ.mainBckC);
			mainGC.fillRectangle(0, 0, aRect.width, aRect.height);
			mainGC.dispose();
			screener.needRedrawSlow();
		}
	}

	void start() {
		try {
			// Create interface
			createInterface();
		}
		catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
}
