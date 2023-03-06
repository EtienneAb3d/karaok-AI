/*
 * Created on Nov 30, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package kj.dsp;

import java.util.ArrayList;

import javax.sound.sampled.SourceDataLine;

import com.cubaix.kaiDJ.KaiDJ;

import kj.audio.KJAudioDataConsumer;
//import kj.ui.KJJukeBox;

/**
 * @author Kris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public final class KJDigitalSignalProcessingAudioDataConsumer implements KJAudioDataConsumer {
	//EM 29/04/2009 : parentJDJ
	protected KaiDJ parentJDJ;

	private static final int DEFAULT_SAMPLE_SIZE = 2048;
	private static final int DEFAULT_FPS = 70;
	
	public static final int SAMPLE_TYPE_EIGHT_BIT   = 1;
	public static final int SAMPLE_TYPE_SIXTEEN_BIT = 2;
	public static final int CHANNEL_MODE_MONO = 1;
	public static final int CHANNEL_MODE_STEREO = 2;
	
	//EM 14/11/2008 : public
	public Object readWriteLock = new Object();
	
	//EM 14/11/2008 : public
	public SourceDataLine sourceDataLine;

	//EM 14/11/2008 : public
	public int  sampleSize;
	
	//EM 14/11/2008 : public
	public long fpsAsNS;
	public long desiredFpsAsNS;
	
	//EM 14/11/2008 : public
	public byte[]  audioDataBuffer;

	//EM 14/11/2008 : public
	public float[] left;
	public float[] right;
	
	private int     position;
	//EM 14/11/2008 : public
	public long    offset;
	
	//EM 14/11/2008 : externalization of inner class for more efficiency
	//(possibly not a big gain, but much more fluent in JOP profiler)
	//private SignalProcessor signalProcessor;
	private KJDigitalSignalProcessor1 signalProcessor;
	
	//EM 14/11/2008 : public
	public ArrayList dsps = new ArrayList();
	
	//EM 14/11/2008 : public
	public int sampleType;
	//EM 14/11/2008 : public
	public int channelMode;
	
	/**
	 * Default constructor creates a DSPAC with DEFAULT_SAMPLE_SIZE and DEFAULT_FPS as
	 * parameters.
	 */
	//EM 29/04/2009 : parentJDJ
	public KJDigitalSignalProcessingAudioDataConsumer(KaiDJ aParentJDJ) {
		this( DEFAULT_SAMPLE_SIZE, DEFAULT_FPS );
		
		//EM 29/04/2009
		parentJDJ = aParentJDJ;
	}
	
	/**
	 * @param pSampleSize The sample size to extract from audio data sent to the SourceDataLine.
	 * @param pFramePerSecond The desired refresh rate per second of registered DSP's.  
	 */
	public KJDigitalSignalProcessingAudioDataConsumer( int pSampleSize, int pFramesPerSecond ) {
		this( pSampleSize, pFramesPerSecond, SAMPLE_TYPE_SIXTEEN_BIT, CHANNEL_MODE_STEREO );
	}
	
	/**
	 * @param pSampleSize The sample size to extract from audio data sent to the SourceDataLine.
	 * @param pFramePerSecond The desired refresh rate per second of registered DSP's.  
	 * @param pSampleType The sample type SAMPLE_TYPE_EIGHT_BIT or SAMPLE_TYPE_SIXTEEN_BIT.  
	 * @param pFramePerSecond The channel mode CHANNEL_MODE_MONO or CHANNEL_MODE_STEREO.  
	 */
	public KJDigitalSignalProcessingAudioDataConsumer( int pSampleSize, int pFramesPerSecond, int pSampleType, int pChannelMode ) { 
	
		sampleSize     = pSampleSize;
		desiredFpsAsNS = 1000000000L / (long)pFramesPerSecond;
		fpsAsNS        = desiredFpsAsNS;	
		
		setSampleType( pSampleType );
		setChannelMode( pChannelMode );
	}
	
	/**
	 * Adds a DSP to the DSPAC and forwards any audio data to it at the specific
	 * frame rate.
	 * 
	 * @param A class implementing the KJDigitalSignalProcessor interface. 
	 */
	public void add( KJDigitalSignalProcessor pSignalProcessor ) {
		dsps.add( pSignalProcessor );
	}

	/**
	 * Removes the specified DSP from this DSPAC if it exists.
	 * 
	 * @param A class implementing the KJDigitalSignalProcessor interface. 
	 */
	public void remove( KJDigitalSignalProcessor pSignalProcessor ) {
		dsps.remove( pSignalProcessor );
	}
	
	public void setChannelMode( int pChannelMode ) {
		channelMode = pChannelMode;
	}
	
	public void setSampleType( int pSampleType ) {
		sampleType = pSampleType;
	}
	
	/**
	 * Start monitoring the specified SourceDataLine.
	 * 
	 * @param pSdl A SourceDataLine.
	 */
	public synchronized void start( SourceDataLine pSdl ) {
		
		// -- Stop processing previous source data line.
		if ( signalProcessor != null ) {
			stop();
		}
		
		if ( signalProcessor == null ) {
		
//			System.out.println( "ADBS: " + pSdl.getBufferSize() );
			
			sourceDataLine = pSdl;
			
			// -- Allocate double the memory than the SDL to prevent
			//    buffer overlapping.
			audioDataBuffer = new byte[ pSdl.getBufferSize() << 1 ];
			
			left  = new float[ sampleSize ];			
			right = new float[ sampleSize ];			

			position = 0;
			offset = 0;
			
			//EM 14/11/2008 : externalization of inner class for more efficiency
			signalProcessor = new /*SignalProcessor();/*/KJDigitalSignalProcessor1(this);
			
			Thread aTh = new Thread( signalProcessor ,"DSP SignalProcessor");
			aTh.start();
		}
		
	}
	
	/**
	 * Stop monitoring the currect SourceDataLine.
	 */
	public synchronized void stop() {
		
		if ( signalProcessor != null ) {
			
			signalProcessor.stop();
			signalProcessor = null;
			
			audioDataBuffer = null;
			sourceDataLine = null;
			
		}
		
	}
	
	private void storeAudioData( byte[] pAudioData, int pOffset, int pLength ) {
		
		synchronized( readWriteLock ) {
		
			if ( audioDataBuffer == null ) {
				return;
			}
			
			int wOverrun = 0;
			
			if ( position + pLength > audioDataBuffer.length - 1 ) {
				
				wOverrun = ( position + pLength ) - audioDataBuffer.length;
				pLength = audioDataBuffer.length - position;
				
			}
			
			System.arraycopy( pAudioData, pOffset, audioDataBuffer, position, pLength );
			
			if ( wOverrun > 0 ) {
				
				System.arraycopy( pAudioData, pOffset + pLength, audioDataBuffer, 0, wOverrun );
				position = wOverrun;
				
			} else {
				position += pLength;
			}
			
//			KJJukeBox.getDSPDialog().setDSPBufferInfo( 
//				position, 
//				pOffset, 
//				pLength, 
//				audioDataBuffer.length );
			
		}
		
	}
	
	/* (non-Javadoc)
	 * @see kj.audio.KJAudioDataConsumer#writeAudioData(byte[])
	 */
	public void writeAudioData( byte[] pAudioData ) {
		storeAudioData( pAudioData, 0, pAudioData.length );
	}

	/* (non-Javadoc)
	 * @see kj.audio.KJAudioDataConsumer#writeAudioData(byte[], int, int)
	 */
	public void writeAudioData( byte[] pAudioData, int pOffset, int pLength ) {
		storeAudioData( pAudioData, pOffset, pLength );
	}
	
	private class SignalProcessor implements Runnable {
		
		boolean process = true;
		
		long lfp = 0;
		
		int frameSize;
		
		public SignalProcessor() {
			frameSize = sourceDataLine.getFormat().getFrameSize();
		}
		
		private int calculateSamplePosition() {
			
			synchronized( readWriteLock ) {
				
				long wFp = sourceDataLine.getLongFramePosition();
				long wNfp = lfp;
				
				lfp = wFp;
				
				int wSdp = (int)( (long)( wNfp * frameSize ) - (long)( audioDataBuffer.length * offset ) );
				
//				KJJukeBox.getDSPDialog().setOutputPositionInfo( 
//					wFp, 
//					wFp - wNfp, 
//					wSdp );
	
				return wSdp;
				
			}
			
		}
		
		private void processSamples( int pPosition ) {
			
			int c = pPosition;
			
			if ( channelMode == CHANNEL_MODE_MONO && sampleType == SAMPLE_TYPE_EIGHT_BIT ) {
				
				for( int a = 0; a < sampleSize; a++, c++ ) { 
				
					if ( c >= audioDataBuffer.length ) {
						offset++; 
						c = ( c - audioDataBuffer.length );
					}
					
					left[ a ]  = (float)( (int)audioDataBuffer[ c ] / 128.0f );
					right[ a ] = left[ a ];
					
				}
				
			} else if ( channelMode == CHANNEL_MODE_STEREO && sampleType == SAMPLE_TYPE_EIGHT_BIT ) {
			
				for( int a = 0; a < sampleSize; a++, c += 2 ) { 
					
					if ( c >= audioDataBuffer.length ) {
						offset++; 
						c = ( c - audioDataBuffer.length );
					}
					
					left[ a ]  = (float)( (int)audioDataBuffer[ c ] / 128.0f );
					right[ a ] = (float)( (int)audioDataBuffer[ c + 1 ] / 128.0f );
					
				}
				
			} else if ( channelMode == CHANNEL_MODE_MONO && sampleType == SAMPLE_TYPE_SIXTEEN_BIT ) {
			
				for( int a = 0; a < sampleSize; a++, c += 2 ) { 
					
					if ( c >= audioDataBuffer.length ) {
						offset++; 
						c = ( c - audioDataBuffer.length );
					}
					
					left[ a ]  = (float)( ( (int)audioDataBuffer[ c + 1 ] << 8 ) + audioDataBuffer[ c ] ) / 32767.0f;;
					right[ a ] = left[ a ];
					
				}
				
			} else if ( channelMode == CHANNEL_MODE_STEREO && sampleType == SAMPLE_TYPE_SIXTEEN_BIT ) {
			
				for( int a = 0; a < sampleSize; a++, c += 4 ) { 
					
					if ( c >= audioDataBuffer.length ) {
						offset++; 
						c = ( c - audioDataBuffer.length );
					}
					
					left[ a ]  = (float)( ( (int)audioDataBuffer[ c + 1 ] << 8 ) + audioDataBuffer[ c ] ) / 32767.0f;
					right[ a ] = (float)( ( (int)audioDataBuffer[ c + 3 ] << 8 ) + audioDataBuffer[ c + 2 ] ) / 32767.0f;
					
				}
				
			}

		}
		
		public void run() {
			
			while( process ) {
				
				try {
				
					long wStn = System.nanoTime();
					
					int wSdp = calculateSamplePosition();
					
					if ( wSdp > 0 ) {
						processSamples( wSdp );
					} 
					
					// -- Dispatch sample data to digtal signal processors.
					for( int a = 0; a < dsps.size(); a++ ) {
						
						// -- Calculate the frame rate ratio hint. This value can be used by 
						//    animated DSP's to fast forward animation frames to make up for
						//    inconsistencies with the frame rate.
						float wFrr = (float)fpsAsNS / (float)desiredFpsAsNS; 
						
						try {
							( (KJDigitalSignalProcessor)dsps.get( a ) ).process( left, right, wFrr );
						} catch( Exception pEx ) {
							System.err.println( "-- DSP Exception: " );
							pEx.printStackTrace();
						}
					}
					
//					KJJukeBox.getDSPDialog().setDSPInformation( 
//						String.valueOf( 1000.0f / ( (float)( wEtn - wStn ) / 1000000.0f ) ) ); 
					
	//				System.out.println( 1000.0f / ( (float)( wEtn - wStn ) / 1000000.0f ) );
					
					long wDelay = fpsAsNS - ( System.nanoTime() - wStn );
					
					// -- No DSP registered? Put the the DSP thread to sleep. 
					if ( dsps.isEmpty() ) {
						wDelay = 1000000000; // -- 1 second.
					}  
						
					if ( wDelay > 0 ) {
					
						try {
							Thread.sleep( wDelay / 1000000, (int)wDelay % 1000000 );
						} catch ( Exception pEx ) {
							// TODO Auto-generated catch block
						}
	
						// -- Adjust FPS until we meet the "desired FPS".
						if ( fpsAsNS > desiredFpsAsNS ) {
							fpsAsNS -= wDelay;
						} else {
							fpsAsNS = desiredFpsAsNS;
						}
						
					} else {
						
						// -- Reduce FPS because we cannot keep up with the "desired FPS".
						fpsAsNS += -wDelay;
						
						// -- Keep thread from hogging CPU.
						try {
							Thread.sleep( 10 );
						} catch ( InterruptedException pEx ) {
							// TODO Auto-generated catch block
						}
						
					}
					
				} catch( Exception pEx ) {
					System.err.println( "- DSP Exception: " );
					pEx.printStackTrace();
				}
				
			}
			
		}
		
		public void stop() {
			process = false;
		}
		
	}

}
