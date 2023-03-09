package com.cubaix.kaiDJ;

import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.cubaix.kaiDJ.db.SongDescr;
import com.cubaix.kaiDJ.swt.PlayListListener;

public class CategoryManager extends Composite {
	KaiDJ parentJDJ;
	PlayList pl;
	PlayListListener playListListener;
	Combo categoriesC;
//	Text categoryT;
	String currentCategory = "";
	public CategoryManager(KaiDJ aParentJDJ,Composite parent, int style) {
		super(parent, style | SWT.NO_BACKGROUND);
		parentJDJ = aParentJDJ;
		
        String os = System.getProperty("os.name");

		//Main panel
		GridLayout aGL = new GridLayout();
		aGL.horizontalSpacing = 0;
		aGL.verticalSpacing = 0;
		aGL.marginHeight = 0;
		aGL.marginWidth = 0;
		aGL.numColumns = 1;
		this.setLayout(aGL);
		GridData aGD = new GridData(GridData.FILL_BOTH);
		this.setLayoutData(aGD);

		//Buttons
		Composite aButPanel = new Composite(this,SWT.NULL);
		aGL = new GridLayout();
		aGL.horizontalSpacing = 0;
		aGL.verticalSpacing = 0;
		aGL.marginHeight = 0;
		aGL.marginWidth = 0;
		aGL.numColumns = 2;
		aButPanel.setLayout(aGL);
		aGD = new GridData(GridData.FILL_HORIZONTAL);
		aButPanel.setLayoutData(aGD);
		
		Label aLabel = new Label(aButPanel,SWT.NULL);
		aLabel.setText("Category name: ");
        if(!os.toLowerCase().startsWith("mac")){
        	//Mac seems to not support properly background colors here
        	aLabel.setBackground(parentJDJ.mainBckC);
        	aLabel.setForeground(parentJDJ.playerC);
        }
		aGD = new GridData(GridData.FILL_VERTICAL);
		aLabel.setLayoutData(aGD);
		
		categoriesC = new Combo(aButPanel,SWT.NULL);
		categoriesC.setToolTipText("Select/Enter a category");
        if(!os.toLowerCase().startsWith("mac")){
        	//Mac seems to not support properly background colors here
        	categoriesC.setBackground(parentJDJ.mainBckC);
        	categoriesC.setForeground(parentJDJ.whiteC);
        }
		aGD = new GridData(GridData.FILL_BOTH);
		categoriesC.setLayoutData(aGD);
        categoriesC.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
			public void widgetSelected(SelectionEvent evt) {
				currentCategory = categoriesC.getItem(categoriesC.getSelectionIndex());
				doCategory();
			}
        	
        });
        categoriesC.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {}
			public void keyReleased(KeyEvent arg0) {
				int aCharCode = arg0.character & 0xFF;
				if (aCharCode > 32 || arg0.keyCode == 8 || (arg0.keyCode >= 32 && arg0.keyCode < 65535)) {
					currentCategory = categoriesC.getText();
					doCategory();
				}
			}
		});

//		categoryT = new Text(aButPanel, SWT.BORDER);
//		categoryT.setToolTipText("Enter a category");
//        if(!os.toLowerCase().startsWith("mac")){
//        	//Mac seems to not support properly background colors here
//        	categoryT.setBackground(parentJDJ.mainBckC);
//        	categoryT.setForeground(parentJDJ.whiteC);
//        }
//		aGD = new GridData(GridData.FILL_BOTH);
//		categoryT.setLayoutData(aGD);
//		categoryT.addKeyListener(new KeyListener() {
//			public void keyPressed(KeyEvent arg0) {}
//			public void keyReleased(KeyEvent arg0) {
//				int aCharCode = arg0.character & 0xFF;
//				if (aCharCode > 32 || arg0.keyCode == 8 || (arg0.keyCode >= 32 && arg0.keyCode < 65535)) {
//					currentCategory = categoryT.getText();
//					pl.getPl().clear();
//					parentJDJ.db.getCat(pl.getPl(),currentCategory);
//					pl.needRedraw();
//				}
//			}
//		});

		//PL
		pl = new PlayList(aParentJDJ, this, SWT.NONE);
		pl.listTC.setToolTip("Edit category:  \n"//
			+ "- Click to load a song on the pre-listen player\n"//
			+ "- Double-click to add a song at the end of the playlist\n"//
			+ "- Ctrl+Click to add a song after the current selection or the current playing song\n"//
			+ "- Suppr key to remove a song");
		aGD = new GridData(GridData.FILL_BOTH);
		pl.setLayoutData(aGD);
		pl.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent aEvt) {
				if (pl.isClickInTopBar(aEvt.x, aEvt.y)) {
					// Nothing to do
				} else {
					//Add song to playlist
					SongDescr aDescr = pl.getMp3Descr(aEvt.x, aEvt.y);
					if (aDescr != null) {
						parentJDJ.managerPlay.addSong(aDescr);
					}
				}
			}

			public void mouseDown(MouseEvent aEvt) {
				if(aEvt.stateMask == SWT.CTRL){
					//Add after
					if (pl.isClickInTopBar(aEvt.x, aEvt.y)) {
						// Nothing to do
					} else {
						SongDescr aDescr = pl.getMp3Descr(aEvt.x, aEvt.y);
						if (aDescr != null) {
							//Add song to playlist
							parentJDJ.managerPlay.addSongAfter(aDescr);
						}
					}
				}
			}

			public void mouseUp(MouseEvent arg0) {
			}
		});
		pl.addPlayListListener(playListListener = new PlayListListener(){
			public void removed(SongDescr aDescr) {
				parentJDJ.db.delInCat(aDescr,currentCategory);
				updateCategoriesList();
			}
			public void added(SongDescr aDescr) {
				parentJDJ.db.insertCat(aDescr,currentCategory);
				parentJDJ.db.commit();
				updateCategoriesList();
			}
		});
		
		//Init values
		updateCategoriesList();
	}
	
	

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose() {
		pl.removePlayListListener(playListListener);
		super.dispose();
	}

	/**
	 * @param aDefaultCat
	 */
	public void checkCategory(String aDefaultCat){
		if(currentCategory == null || currentCategory.length() <= 0){
			currentCategory = aDefaultCat;
			parentJDJ.display.syncExec(new Runnable(){
				public void run() {
					categoriesC.setText(currentCategory);
				}
				
			});
		}
	}

	/**
	 * @param aDescr
	 */
	public void addSong(SongDescr aDescr){
		checkCategory("Misc");
		parentJDJ.db.insertCat(aDescr,currentCategory);
		parentJDJ.db.commit();
		pl.getPl().add(aDescr);
		pl.listTC.needRedraw();
		updateCategoriesList();
	}
	
	/**
	 * @param aPath
	 * @param aClear
	 */
	public void load(String aPath, boolean aClear) {
		pl.load(aPath, aClear, 0);
	}

	/**
	 * 
	 */
	void updateCategoriesList(){
		final Vector aRes = parentJDJ.db.getAllCat();
		parentJDJ.display.syncExec(new Runnable(){
			public void run() {
				categoriesC.removeAll();
				for(int c = 0;c < aRes.size();c++){
					categoriesC.add((String)aRes.elementAt(c));
				}
				categoriesC.setText(currentCategory);
			}
		});
	}

	/**
	 * 
	 */
	void doCategory(){
		// Start main refresh thread
		Thread aThread = new Thread(new Runnable(){
			public void run() {
				pl.getPl().clear();
				parentJDJ.db.getCat(pl.getPl(),currentCategory);
				pl.listTC.needRedraw();
			}
		}, "Category request");
		aThread.setPriority(Thread.MIN_PRIORITY);
		aThread.start();
	}
	
}
