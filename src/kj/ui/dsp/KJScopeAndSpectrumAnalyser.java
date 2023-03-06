/*
 * Created on Dec 18, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package kj.ui.dsp;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

import kj.dsp.KJDigitalSignalProcessor;
import kj.dsp.KJFFT;

/**
 * @author Kris Fudalewski
 *
 * Using the KJDigitalSignalProcessor package, this component can display a realtime
 * scope or spectrum analyser.
 */
public class KJScopeAndSpectrumAnalyser extends Component implements KJDigitalSignalProcessor {

	public static final int   DISPLAY_MODE_SCOPE             = 0;
	public static final int   DISPLAY_MODE_SPECTRUM_ANALYSER = 1;
	public static final int   DISPLAY_MODE_VU_METER          = 2;
	
	public static final int   DEFAULT_WIDTH  = 256;
	public static final int   DEFAULT_HEIGHT = 128;
	
	public static final int   DEFAULT_SPECTRUM_ANALYSER_FFT_SAMPLE_SIZE = 512;
	public static final int   DEFAULT_SPECTRUM_ANALYSER_BAND_COUNT      = 20;
	public static final float DEFAULT_SPECTRUM_ANALYSER_DECAY           = 0.03f;

	public static final Color DEFAULT_BACKGROUND_COLOR = new Color( 0,   0,   128 );	
	public static final Color DEFAULT_SCOPE_COLOR      = new Color( 255, 192, 0 );
	
	public static final float DEFAULT_VU_METER_DECAY           = 0.02f;
	
	private Image bi;

	private int displayMode = DISPLAY_MODE_SCOPE;
	
	private Color   scopeColor             = DEFAULT_SCOPE_COLOR; 
	private Color[] spectrumAnalyserColors = getDefaultSpectrumAnalyserColors();
	
	private int width;
	private int height;
	private int height_2;
	
	// -- Spectrum analyser varibles.
	private KJFFT   fft; 
	private float[] old_FFT;
	private int     saFFTSampleSize;  
	private int     saBands;   
	private float   saColorScale;   
	private float   saMultiplier;
	
	private float saDecay = DEFAULT_SPECTRUM_ANALYSER_DECAY; 
	private float sad;
	
	// -- VU Meter
	private float oldLeft;
	private float oldRight;
//	private float vuAverage;
//	private float vuSamples;

	private float vuDecay = DEFAULT_VU_METER_DECAY; 
	private float vuColorScale;   
	
	// -- FPS calulations.
	private long    lfu = 0;
	private	int     fc  = 0;
	private int     fps;
	private boolean showFPS = false;
	
//	private Runnable PAINT_SYNCHRONIZER = new AWTPaintSynchronizer();
	
	public KJScopeAndSpectrumAnalyser() {
		initialize();
	}
	
	private void computeColorScale() {
		saColorScale = ( (float)spectrumAnalyserColors.length / height ) * 2.0f;
		vuColorScale = ( (float)spectrumAnalyserColors.length / ( width - 32 ) ) * 2.0f;
	}

	private void computeSAMultiplier() {
		saMultiplier = ( saFFTSampleSize / 2 ) / saBands;
	}

	private void drawScope( Graphics pGrp, float[] pSample ) {
		
		pGrp.setColor( scopeColor );
    	
    	int wLas = (int)( pSample[ 0 ] * (float)height_2 ) + height_2;
    	int wSt = 2;
    	
       	for ( int a = wSt, c = 0; c < width; a += wSt, c++ ) {
    		
    		int wAs = (int)( pSample[ a ] * (float)height_2 ) + height_2;
    		
    		pGrp.drawLine( 
    		    c, 
				wLas, 
				c + 1, 
				wAs );
    		
    		wLas = wAs;
    		
    	}

	}
	
	private void drawSpectrumAnalyser( Graphics pGrp, float[] pSample, float pFrrh ) {
		
    	float   c = 0;
    	float[] wFFT = fft.calculate( pSample );
    	
    	float wSadfrr = ( saDecay * pFrrh );
    	float wBw = ( (float)width / (float)saBands ); 
    	
    	for ( int a = 0, bd = 0; bd < saBands; a += saMultiplier, bd++ ) {
    		
    		float wFs = 0;
    		
    		// -- Average out nearest bands.
    		for( int b = 0; b < saMultiplier; b++ ) {
    			wFs += wFFT[ a + b ];
    		}
    		
    		// -- Log filter.
    		wFs = ( wFs * (float)Math.log( bd + 2 ) );  
    		
    		if ( wFs > 1.0f ) { 
    			wFs = 1.0f; 
    		}
    		
    		// -- Compute SA decay...
    		if ( wFs >= ( old_FFT[ a ] - wSadfrr ) ) {
    			
    			old_FFT[ a ] = wFs;
    			
    		} else {
    			
    			old_FFT[ a ] -= wSadfrr;
    			
    			if ( old_FFT[ a ] < 0 ) {
    				old_FFT[ a ] = 0;
    			}
    			
    			wFs = old_FFT[ a ];
    			
    		}
    		
    		drawSpectrumAnalyserBar( 
    			pGrp, 
				(int)c, 
				height, 
				(int)wBw - 1, 
				(int)( wFs * height ) );
    		
    		c += wBw;
    		
    	}
    	
	}
	
	private void drawVUMeter( Graphics pGrp, float[] pLeft, float[] pRight, float pFrrh ) {
	
		float wLeft  = 0.0f;
		float wRight = 0.0f;
		
    	float wSadfrr = ( vuDecay * pFrrh );
		
		for( int a = 0; a < pLeft.length; a++ ) {
			
			wLeft  += Math.abs( pLeft[ a ] );
			wRight += Math.abs( pRight[ a ] );
			
		}
		
		wLeft  = ( ( wLeft * 2.0f ) / (float)pLeft.length );
		wRight = ( ( wRight * 2.0f ) / (float)pRight.length );
		
		if ( wLeft > 1.0f ) {
			wLeft = 1.0f;
		}
		
		if ( wRight > 1.0f ) {
			wRight = 1.0f;
		}
		
//		vuAverage += ( ( wLeft + wRight ) / 2.0f ); 
//		vuSamples++;
//		
//		if ( vuSamples > 128 ) {
//			vuSamples /= 2.0f;
//			vuAverage /= 2.0f;
//		}
		
		if ( wLeft >= ( oldLeft - wSadfrr ) ) {
			oldLeft = wLeft;
		} else {

			oldLeft -= wSadfrr;
			
			if ( oldLeft < 0 ) {
				oldLeft = 0;
			}
			
		}
		
		if ( wRight >= ( oldRight - wSadfrr ) ) {
			oldRight = wRight;
		} else {
			
			oldRight -= wSadfrr;
			
			if ( oldRight < 0 ) {
				oldRight = 0;
			}
			
		}
		
		int wHeight = ( height >> 1 ) - 24;
		
		drawVolumeMeterBar( pGrp, 16, 16,           (int)( oldLeft  * (float)( width - 32 ) ), wHeight );
//		drawVolumeMeterBar( pGrp, 16, wHeight + 22, (int)( ( vuAverage / vuSamples ) * (float)( width - 32 ) ), 4 );
		drawVolumeMeterBar( pGrp, 16, wHeight + 32, (int)( oldRight * (float)( width - 32 ) ), wHeight );

		
		
//		pGrp.fillRect( 16, 16, (int)( oldLeft  * (float)( width - 32 ) ), wHeight );
//		pGrp.fillRect( 16, 64, (int)( oldRight * (float)( width - 32 ) ), wHeight );
		
	}
	
	private void drawSpectrumAnalyserBar( Graphics pGraphics, int pX, int pY, int pWidth, int pHeight ) {
		
		float c = 0;
		
		for( int a = pY; a >= pY - pHeight; a -= 2 ) {
			
			c += saColorScale;
			
			if ( c < 256.0f ) {
				pGraphics.setColor( spectrumAnalyserColors[ (int)c ] );
			}
			
			pGraphics.fillRect( 
				pX, 
				a, 
				pWidth, 
				1 );
			
		}
		
	}

	private void drawVolumeMeterBar( Graphics pGraphics, int pX, int pY, int pWidth, int pHeight ) {
		
		float c = 0;
		
		for( int a = pX; a <= pX + pWidth; a += 2 ) {
			
			c += vuColorScale;
			
			if ( c < 256.0f ) {
				pGraphics.setColor( spectrumAnalyserColors[ (int)c ] );
			}
			
			pGraphics.fillRect( 
				a, 
				pY, 
				1, 
				pHeight );
			
		}
		
	}
	
	private synchronized Image getDoubleBuffer() {
		
		if ( bi == null || ( bi.getWidth( null ) != getSize().width || bi.getHeight( null ) != getSize().height ) ) {
		
			width    = getSize().width;
			height   = getSize().height;
			height_2 = height >> 1;
			
			computeColorScale();
			
			bi = getGraphicsConfiguration().createCompatibleVolatileImage( 
				width,
				height );
			
		}
		
		return bi;
		
	}
	
	public static Color[] getDefaultSpectrumAnalyserColors() {
		
		Color[] wColors = new Color[ 256 ];
		
		for( int a = 0; a < 128; a++ ) {
			wColors[ a ] = new Color( 0, ( a >> 1 ) + 192, 0 );
		}
		
		for( int a = 0; a < 64; a++ ) {
			wColors[ a + 128 ] = new Color( a << 2, 255, 0 );
		}

		for( int a = 0; a < 64; a++ ) {
			wColors[ a + 192 ] = new Color( 255, 255 - ( a << 2 ), 0 );
		}
		
		return wColors;
		
	}
	
	/**
	 * @return Returns the current display mode, DISPLAY_MODE_SCOPE or DISPLAY_MODE_SPECTRUM_ANALYSER.
	 */
	public int getDisplayMode() {
		return displayMode;
	}
	
	/**
	 * @return Returns the current number of bands displayed by the spectrum analyser.  
	 */
	public int getSpectrumAnalyserBandCount() {
		return saBands;
	}
	
	/**
	 * @return Returns the decay rate of the spectrum analyser's bands. 
	 */
	public float getSpectrumAnalyserDecay() {
		return saDecay;
	}
	
	
	/**
	 * @return Returns the color the scope is rendered in.
	 */
	public Color getScopeColor() {
		return scopeColor;
	}
	
	/**
	 * @return Returns the color scale used to render the spectrum analyser bars.
	 */
	public Color[] getSpectrumAnalyserColors() {
		return spectrumAnalyserColors;
	}
	
	private void initialize() {
		
		setSize( DEFAULT_WIDTH, DEFAULT_HEIGHT );

		setBackground( DEFAULT_BACKGROUND_COLOR );
		
		prepareDisplayToggleListener();

		setSpectrumAnalyserBandCount( DEFAULT_SPECTRUM_ANALYSER_BAND_COUNT );
		setSpectrumAnalyserFFTSampleSize( DEFAULT_SPECTRUM_ANALYSER_FFT_SAMPLE_SIZE );

	}
	
	/**
	 * @return Returns 'true' if "Frames Per Second" are being calculated and displayed.  
	 */
	public boolean isShowingFPS() {
		return showFPS;
	}
	
	public void paint( Graphics pGraphics ) {
		pGraphics.drawImage( getDoubleBuffer(), 0, 0, null );
	}
	
	private void prepareDisplayToggleListener() {
		
		setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
		
		addMouseListener(
			new MouseAdapter() {
				public void mouseClicked( MouseEvent pEvent ) {
					
					if ( pEvent.getButton() == MouseEvent.BUTTON1 ) {
					
						if ( displayMode + 1 > 2 ) {
							displayMode = 0;
						} else { 
							displayMode++;
						}
						
					}
					
				}
			}
	    );
		
	}
	
	/* (non-Javadoc)
	 * @see kj.dsp.KJDigitalSignalProcessor#process(float[], float[], float)
	 */
	public synchronized void process( float[] pLeft, float[] pRight, float pFrameRateRatioHint ) {
    	
    	Graphics wGrp = getDoubleBuffer().getGraphics(); 

    	wGrp.setColor( getBackground() );
    	wGrp.fillRect( 0, 0, getSize().width, getSize().height );
    	
    	switch( displayMode ) {
    		
    		case DISPLAY_MODE_SCOPE:
    			drawScope( wGrp, stereoMerge( pLeft, pRight ) );
    			break;
    			
    		case DISPLAY_MODE_SPECTRUM_ANALYSER:
    			drawSpectrumAnalyser( wGrp, stereoMerge( pLeft, pRight ), pFrameRateRatioHint );
    			break;

    		case DISPLAY_MODE_VU_METER:
    			drawVUMeter( wGrp, pLeft, pRight, pFrameRateRatioHint );
    			break;
    			
    	}
    	
    	// -- Show FPS if necessary.
    	if ( showFPS ) {
    	
	    	// -- Calculate FPS.
			if ( System.currentTimeMillis() >= lfu + 1000 ) {
				
				lfu = System.currentTimeMillis();
				fps = fc;
				fc = 0;
				
			}
	
			fc++;
	    	
	    	wGrp.setColor( Color.yellow );
	    	wGrp.drawString( "FPS: " + fps + " (FRRH: " + pFrameRateRatioHint + ")", 0, height - 1 );
	    	
    	}
    	
		getGraphics().drawImage( getDoubleBuffer(), 0, 0, null );
    	
//    	repaint();
    	
//    	try {
//			EventQueue.invokeLater( new AWTPaintSynchronizer() );
//		} catch ( Exception pEx ) {
//			// -- Ignore exception.
//			pEx.printStackTrace();
//		} 
    	
    }
	
	/**
	 * Sets the current display mode.
	 * 
	 * @param pMode Must be either DISPLAY_MODE_SCOPE or DISPLAY_MODE_SPECTRUM_ANALYSER.
	 */
	public synchronized void setDisplayMode( int pMode ) {
		displayMode = pMode;
	}
	
	/**
	 * Sets the color of the scope.
	 * 
	 * @param pColor 
	 */
	public synchronized void setScopeColor( Color pColor ) {
		scopeColor = pColor;
	}
	
	/**
	 * When 'true' is passed as a parameter, will overlay the "Frames Per Seconds"
	 * achieved by the component.
	 * 
	 * @param pState
	 */
	public synchronized void setShowFPS( boolean pState ) {
		showFPS = pState;
	}
	
	/**
	 * Sets the numbers of bands rendered by the spectrum analyser. 
	 * 
	 * @param pCount Cannot be more than half the "FFT sample size".
	 */
	public synchronized void setSpectrumAnalyserBandCount( int pCount ) {
		saBands = pCount;
		computeSAMultiplier();
	}
	
	/**
	 * Sets the spectrum analyser band decay rate.
	 * 
	 * @param pDecay Must be a number between 0.0 and 1.0 exclusive.
	 */
	public synchronized void setSpectrumAnalyserDecay( float pDecay ) {
		saDecay = pDecay;
	}
	
	/**
	 * Sets the spectrum analyser color scale.
	 * 
	 * @param pColors Any amount of colors may be used. Must not be null.
	 */
	public synchronized void setSpectrumAnalyserColors( Color[] pColors ) {
		spectrumAnalyserColors = pColors;
		computeColorScale();
	}
	
	/**
	 * Sets the FFT sample size to be just for calculating the spectrum analyser 
	 * values. The default is 512. 
	 * 
	 * @param pSize Cannot be more than the size of the sample provided by the DSP.  
	 */
	public synchronized void setSpectrumAnalyserFFTSampleSize( int pSize ) {
		
		saFFTSampleSize = pSize;
		
		fft = new KJFFT( saFFTSampleSize );
		old_FFT = new float[ saFFTSampleSize ]; 
		
		computeSAMultiplier();
		
	}
	
	private float[] stereoMerge( float[] pLeft, float[] pRight ) {
		
		for( int a = 0; a < pLeft.length; a++ ) {
			pLeft[ a ] = ( pLeft[ a ] + pRight[ a ] ) / 2.0f;
		}
		
		return pLeft;
		
	}
	
	public void update( Graphics pGraphics ) {
		// -- Prevent AWT from clearing background.
		paint( pGraphics );
	}
	
//	private class AWTPaintSynchronizer implements Runnable {
//		
//		public void run() {
//			repaint();
//		}
//		
//	}
	
}
