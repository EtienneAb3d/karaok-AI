package kj.dsp;

/**
 * @author Kris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class KJFFT {

    private float[] xre;
    private float[] xim;
    private float[] mag;
    
    private float[] fftSin;
    private float[] fftCos;
    private int[]   fftBr;
    
    private int ss, ss2, nu, nu1;
    
	/**
	 * @param The amount of the sample provided to the "calculate" method to use during
	 *        FFT calculations.
	 */
	public KJFFT( int pSampleSize ) {
        
		ss  = pSampleSize;
		ss2 = ss >> 1;
			
		xre = new float[ ss ];
        xim = new float[ ss ];
        mag = new float[ ss2 ];
		
        nu = (int)( Math.log( ss ) / Math.log( 2 ) );
        nu1 = nu - 1;
        
        prepareFFTTables();
        
	}

    private int bitrev( int j, int nu ) {

        int j1 = j;
        int j2;
        int k = 0;
        
        for( int i = 1; i <= nu; i++ ) {
            j2 = j1 >> 1;
            k  = ( k << 1 ) + j1 - ( j2 << 1 );
            j1 = j2;
        }
        
        return k;
        
    }

    
    /**
     * @param  pSample The sample to compute FFT values on.
     * @return         The results of the calculation, normalized between 0.0 and 1.0. 
     */
    public float[] calculate( float[] pSample ) {
        
        int n2  = ss2;
        int nu1 = nu - 1;
        
        int wAps = pSample.length / ss;
        
        // -- FIXME: This affects the calculation accuracy, because
        //           is compresses the digital signal. Looks nice on
        //           the spectrum analyser, as it chops off most of 
        //           sound we cannot hear anyway.
        for ( int a = 0, b = 0; a < pSample.length; a += wAps, b++ ) {
       		xre[ b ] = pSample[ a ];
        	xim[ b ] = 0.0f;
        }
        
        float tr, ti, c, s;
        int   k, kn2, x = 0;

        for ( int l = 1; l <= nu; l++ ) {
        	
            k = 0;
            
            while ( k < ss ) {
            	
                for ( int i = 1; i <= n2; i++ ) {
                	
                	// -- Tabled sin/cos
                    c = fftCos[ x ]; 
                    s = fftSin[ x ]; 
                    
                    kn2 = k + n2;
                    
                    tr = xre[ kn2 ] * c + xim[ kn2 ] * s;
                    ti = xim[ kn2 ] * c - xre[ kn2 ] * s;
                    
                    xre[ kn2 ] = xre[ k ] - tr;
                    xim[ kn2 ] = xim[ k ] - ti;
                    xre[ k ] += tr;
                    xim[ k ] += ti;
                    
                    k++; x++;
                    
                }
                
                k += n2;
                
            }
            
            nu1--;
            n2 >>= 1; 
            
        }
        
        int r;
        
        // -- Reorder output.
        for( k = 0; k < ss; k++ ) {
        	
        	// -- Use tabled BR values.
            r = fftBr[ k ]; 
            
            if ( r > k ) {
            	
                tr = xre[ k ];
                ti = xim[ k ];
                
                xre[ k ] = xre[ r ];
                xim[ k ] = xim[ r ];
                xre[ r ] = tr;
                xim[ r ] = ti;
                
            }
            
        }
        
        // -- Calculate magnitude.
        mag[ 0 ] = (float)( Math.sqrt( xre[ 0 ] * xre[ 0 ] + xim[ 0 ] * xim[ 0 ] ) ) / ss;
        
        for ( int i = 1; i < ss2; i++ ) {
            mag[ i ]= 2 * (float)( Math.sqrt( xre[ i ] * xre[ i ] + xim[ i ] * xim[ i ] ) ) / ss;
        }
        
        return mag;
        
    }
    
    private void prepareFFTTables() {
    	
        int n2 = ss2;
        int nu1 = nu - 1;
        
        // -- Allocate FFT SIN/COS tables.
    	fftSin = new float[ nu * n2 ];
    	fftCos = new float[ nu * n2 ];
    	
        float tr, ti, p, arg;
        int   k = 0, x = 0;

        // -- Prepare SIN/COS tables.
        for ( int l = 1; l <= nu; l++ ) {
        	
            while ( k < ss ) {
            	
                for ( int i = 1; i <= n2; i++ ) {
                	
                    p = bitrev( k >> nu1, nu );
                    
                    arg = 2 * (float)Math.PI * p / ss;

                    fftSin[ x ] = (float)Math.sin( arg );
                    fftCos[ x ] = (float)Math.cos( arg );
                    
                    k++;
                    x++;
                    
                }
                
                k += n2;
                
            }
            
            k = 0;
            
            nu1--;
            n2 >>= 1; 
            
        }
        
        // -- Prepare bitrev table.
        fftBr = new int[ ss ];
        
        for( k = 0; k < ss; k++ ) {
            fftBr[ k ] = bitrev( k, nu );
        }
        
    }
	
}
