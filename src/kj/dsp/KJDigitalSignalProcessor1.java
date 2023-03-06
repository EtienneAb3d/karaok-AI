package kj.dsp;

//EM 14/11/2008 : externalization of inner class for more efficiency 
//(possibly not a big gain, but much more fluent in JOP profiler)
public final class KJDigitalSignalProcessor1 implements Runnable {
	
	KJDigitalSignalProcessingAudioDataConsumer dspadc;
		
		boolean process = true;
		
		long lfp = 0;
		
		int frameSize;
		
		KJDigitalSignalProcessor1(KJDigitalSignalProcessingAudioDataConsumer aDspadc){
			dspadc = aDspadc;
			frameSize = dspadc.sourceDataLine.getFormat().getFrameSize();
		}
		
		private int calculateSamplePosition() {
			
			synchronized( dspadc.readWriteLock ) {
				
				long wFp = dspadc.sourceDataLine.getLongFramePosition();
				long wNfp = lfp;
				
				lfp = wFp;
				
				int wSdp = (int)( (long)( wNfp * frameSize ) - (long)( dspadc.audioDataBuffer.length * dspadc.offset ) );
				
//				KJJukeBox.getDSPDialog().setOutputPositionInfo( 
//					wFp, 
//					wFp - wNfp, 
//					wSdp );
	
				return wSdp;
				
			}
			
		}
		
		private void processSamples( int pPosition ) {
			
			int c = pPosition;
			
			if ( dspadc.channelMode == dspadc.CHANNEL_MODE_MONO && dspadc.sampleType == dspadc.SAMPLE_TYPE_EIGHT_BIT ) {
				
				for( int a = 0; a < dspadc.sampleSize; a++, c++ ) { 
				
					if ( c >= dspadc.audioDataBuffer.length ) {
						dspadc.offset++; 
						c = ( c - dspadc.audioDataBuffer.length );
					}
					
					dspadc.left[ a ]  = (float)( (int)dspadc.audioDataBuffer[ c ] / 128.0f );
					dspadc.right[ a ] = dspadc.left[ a ];
					
				}
				
			} else if ( dspadc.channelMode == dspadc.CHANNEL_MODE_STEREO && dspadc.sampleType == dspadc.SAMPLE_TYPE_EIGHT_BIT ) {
			
				for( int a = 0; a < dspadc.sampleSize; a++, c += 2 ) { 
					
					if ( c >= dspadc.audioDataBuffer.length ) {
						dspadc.offset++; 
						c = ( c - dspadc.audioDataBuffer.length );
					}
					
					dspadc.left[ a ]  = (float)( (int)dspadc.audioDataBuffer[ c ] / 128.0f );
					dspadc.right[ a ] = (float)( (int)dspadc.audioDataBuffer[ c + 1 ] / 128.0f );
					
				}
				
			} else if ( dspadc.channelMode == dspadc.CHANNEL_MODE_MONO && dspadc.sampleType == dspadc.SAMPLE_TYPE_SIXTEEN_BIT ) {
			
				for( int a = 0; a < dspadc.sampleSize; a++, c += 2 ) { 
					
					if ( c >= dspadc.audioDataBuffer.length ) {
						dspadc.offset++; 
						c = ( c - dspadc.audioDataBuffer.length );
					}
					
					dspadc.left[ a ]  = (float)( ( (int)dspadc.audioDataBuffer[ c + 1 ] << 8 ) + dspadc.audioDataBuffer[ c ] ) / 32767.0f;;
					dspadc.right[ a ] = dspadc.left[ a ];
					
				}
				
			} else if ( dspadc.channelMode == dspadc.CHANNEL_MODE_STEREO && dspadc.sampleType == dspadc.SAMPLE_TYPE_SIXTEEN_BIT ) {
			
				for( int a = 0; a < dspadc.sampleSize; a++, c += 4 ) { 
					
					if ( c >= dspadc.audioDataBuffer.length ) {
						dspadc.offset++; 
						c = ( c - dspadc.audioDataBuffer.length );
					}
					
					dspadc.left[ a ]  = (float)( ( (int)dspadc.audioDataBuffer[ c + 1 ] << 8 ) + dspadc.audioDataBuffer[ c ] ) / 32767.0f;
					dspadc.right[ a ] = (float)( ( (int)dspadc.audioDataBuffer[ c + 3 ] << 8 ) + dspadc.audioDataBuffer[ c + 2 ] ) / 32767.0f;
					
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
					for( int a = 0; a < dspadc.dsps.size(); a++ ) {
						
						// -- Calculate the frame rate ratio hint. This value can be used by 
						//    animated DSP's to fast forward animation frames to make up for
						//    inconsistencies with the frame rate.
						float wFrr = (float)dspadc.fpsAsNS / (float)dspadc.desiredFpsAsNS; 
						
						try {
							( (KJDigitalSignalProcessor)dspadc.dsps.get( a ) ).process( dspadc.left, dspadc.right, wFrr );
						} catch( Exception pEx ) {
							System.err.println( "-- DSP Exception: " );
							pEx.printStackTrace();
						}
					}
					
//					KJJukeBox.getDSPDialog().setDSPInformation( 
//						String.valueOf( 1000.0f / ( (float)( wEtn - wStn ) / 1000000.0f ) ) ); 
					
	//				System.out.println( 1000.0f / ( (float)( wEtn - wStn ) / 1000000.0f ) );
					
					long wDelay = dspadc.fpsAsNS - ( System.nanoTime() - wStn );
					
					// -- No DSP registered? Put the the DSP thread to sleep. 
					if ( dspadc.dsps.isEmpty() ) {
						wDelay = 1000000000; // -- 1 second.
					}  
						
					if ( wDelay > 0 ) {
					
						try {
							Thread.sleep( wDelay / 1000000, (int)wDelay % 1000000 );
						} catch ( Exception pEx ) {
							// TODO Auto-generated catch block
						}
	
						// -- Adjust FPS until we meet the "desired FPS".
						if ( dspadc.fpsAsNS > dspadc.desiredFpsAsNS ) {
							dspadc.fpsAsNS -= wDelay;
						} else {
							dspadc.fpsAsNS = dspadc.desiredFpsAsNS;
						}
						
					} else {
						
						// -- Reduce FPS because we cannot keep up with the "desired FPS".
						dspadc.fpsAsNS += -wDelay;
						
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
