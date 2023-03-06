package com.cubaix.kaiDJ.xml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Vector;

public class XLoader {
	private static boolean _TRACE = false;

	String fileName = null;

	BufferedReader in = null;

	int parseState = 0;
	boolean newLineAsSpaces = true;

	String currentCharset = "iso-8859-1";
	boolean charsetChecked = false;

	Vector xObjects;

	String aBufString = null;

	int aBufStringPos = 0;

	/**
	 * 
	 */
	public XLoader() {
		// EM 06/06/2006 : char entities moved on XQualitifer
		// loadCharEntities();
	}

	public XLoader(String aCharSet) {
		// EM 06/06/2006 : char entities moved on XQualitifer
		// loadCharEntities();
		currentCharset = aCharSet;
	}

	/**
	 * @param aFileName
	 * @return
	 */
	public Vector load(String aFileName) {
		fileName = aFileName;
		if (!new File(aFileName).exists()) {
			// Can't load
			System.err.println("Can't load file '" + aFileName + "'");
			return xObjects = new Vector();
		}
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(aFileName), currentCharset));
			parse();
			in.close();
		} catch (Throwable t) {
			System.err.println("Error creating input access : "+ t);
		}

		return xObjects;
	}

	/**
	 * 
	 */
	void readNext() {
		String aLine;
		try {
			if (in == null || (aLine = in.readLine()) == null) {
				parseState = -1;
				return;
			}
			aBufStringPos = 0;
			aBufString = aLine + '\n';// .getBytes();
		} catch (Throwable t) {
			System.err.println("Error while reading input : "+ t);
		}
	}

	/*
	 * public Vector parse(String aStr) { aBufStringPos = 0; aBufString =
	 * aStr.getBytes(); return parse(); }
	 */

	/**
	 * @param aCharset
	 * @return
	 */
	String findEncoding(String aCharset) {
		if (Charset.availableCharsets().containsKey(aCharset)) {
			// Ok, direct value
			return aCharset;
		}
		// Assume values for bad formated entries
		String aNewCharSet = null;
		if (aCharset.indexOf("8859") >= 0) {
			aNewCharSet = "iso-8859-1";
		}
		if (aCharset.indexOf("utf") >= 0) {
			aNewCharSet = "utf-8";
		}
		if (aNewCharSet != null) {
			System.out.println("Warning, bad formatted charset '" + aCharset + "' think it is '" + aNewCharSet + "'");
			return aNewCharSet;
		}
		// Don't know ?? keep current
		System.out.println("Warning, can't recognize charset in '" + aCharset + "' keeping '" + currentCharset + "'");
		Iterator aIt = Charset.availableCharsets().keySet().iterator();
		System.out.println("Avalaible charset : ");
		while (aIt.hasNext()) {
			System.out.println((String) aIt.next());
		}
		return currentCharset;
	}

	/**
	 * @param aXTag
	 * @return
	 */
	boolean isGoodCharset(XTag aXTag) {
		String aContent;
		String aCharset = null;
		if (aXTag.tagName.equalsIgnoreCase("meta") && "content-type".equalsIgnoreCase(aXTag.getValue("http-equiv")) && (aContent = aXTag.getValue("content")) != null) {
			int aCharsetPos;
			aContent = aContent.toLowerCase();
			if ((aCharsetPos = aContent.indexOf("charset=")) >= 0) {
				aCharset = aContent.substring(aCharsetPos + "charset=".length());
			}
		} else if (aXTag.tagName.equalsIgnoreCase("?xml") && (aContent = aXTag.getValue("encoding")) != null) {
			aCharset = aContent.toLowerCase();
		}
		String aEncoding;
		if (aCharset != null && !(currentCharset.equalsIgnoreCase(aEncoding = findEncoding(aCharset)))) {
			System.out.println("Bad current charset definition '" + currentCharset + "' need to restart with '" + aEncoding + "'");
			charsetChecked = true;// Avoid loop on multi definitions
			currentCharset = aEncoding;
			return false;
		}
		return true;
	}

	/**
	 * @param aBuf
	 */
	void transUndefAs1252(char[] aBuf, int aSize) {
		for (int c = 0; c < aSize; c++) {
			switch (aBuf[c]) {
				case 0x80 :
					aBuf[c] = (char) 0x20AC;// #EURO SIGN
					break;
				/*
				 * case 0x81: return //#UNDEFINED;
				 */
				case 0x82 :
					aBuf[c] = (char) 0x201A;// #SINGLE LOW-9 QUOTATION MARK
					break;
				case 0x83 :
					aBuf[c] = (char) 0x0192;// #LATIN SMALL LETTER F WITH HOOK
					break;
				case 0x84 :
					aBuf[c] = (char) 0x201E;// #DOUBLE LOW-9 QUOTATION MARK
					break;
				case 0x85 :
					aBuf[c] = (char) 0x2026;// #HORIZONTAL ELLIPSIS
					break;
				case 0x86 :
					aBuf[c] = (char) 0x2020;// #DAGGER
					break;
				case 0x87 :
					aBuf[c] = (char) 0x2021;// #DOUBLE DAGGER
					break;
				case 0x88 :
					aBuf[c] = (char) 0x02C6;// #MODIFIER LETTER CIRCUMFLEX
											// ACCENT
					break;
				case 0x89 :
					aBuf[c] = (char) 0x2030;// #PER MILLE SIGN
					break;
				case 0x8A :
					aBuf[c] = (char) 0x0160;// #LATIN CAPITAL LETTER S WITH
											// CARON
					break;
				case 0x8B :
					aBuf[c] = (char) 0x2039;// #SINGLE LEFT-POINTING ANGLE
											// QUOTATION MARK
					break;
				case 0x8C :
					aBuf[c] = (char) 0x0152;// #LATIN CAPITAL LIGATURE OE
					break;
				/*
				 * case 0x8D: return //#UNDEFINED;
				 */
				case 0x8E :
					aBuf[c] = (char) 0x017D;// #LATIN CAPITAL LETTER Z WITH
											// CARON
					break;
				/*
				 * case 0x8F: return //#UNDEFINED;
				 */
				/*
				 * case 0x90: return //#UNDEFINED;
				 */
				case 0x91 :
					aBuf[c] = (char) 0x2018;// #LEFT SINGLE QUOTATION MARK
					break;
				case 0x92 :
					aBuf[c] = (char) 0x2019;// #RIGHT SINGLE QUOTATION MARK
					break;
				case 0x93 :
					aBuf[c] = (char) 0x201C;// #LEFT DOUBLE QUOTATION MARK
					break;
				case 0x94 :
					aBuf[c] = (char) 0x201D;// #RIGHT DOUBLE QUOTATION MARK
					break;
				case 0x95 :
					aBuf[c] = (char) 0x2022;// #BULLET
					break;
				case 0x96 :
					aBuf[c] = (char) 0x2013;// #EN DASH
					break;
				case 0x97 :
					aBuf[c] = (char) 0x2014;// #EM DASH
					break;
				case 0x98 :
					aBuf[c] = (char) 0x02DC;// #SMALL TILDE
					break;
				case 0x99 :
					aBuf[c] = (char) 0x2122;// #TRADE MARK SIGN
					break;
				case 0x9A :
					aBuf[c] = (char) 0x0161;// #LATIN SMALL LETTER S WITH CARON
					break;
				case 0x9B :
					aBuf[c] = (char) 0x203A;// #SINGLE RIGHT-POINTING ANGLE
											// QUOTATION MARK
					break;
				case 0x9C :
					aBuf[c] = (char) 0x0153;// #LATIN SMALL LIGATURE OE
					break;
				/*
				 * case 0x9D: return //#UNDEFINED;
				 */
				case 0x9E :
					aBuf[c] = (char) 0x017E;// #LATIN SMALL LETTER Z WITH CARON
					break;
				case 0x9F :
					aBuf[c] = (char) 0x0178;// #LATIN CAPITAL LETTER Y WITH
											// DIAERESIS
					break;
				default :
					break;
			}
		}
	}

	/**
	 * @return
	 */
	Vector parse() {
		xObjects = new Vector();
		String aStr = null;
		char[] aTmpBuf = new char[10000];
		int aTmpBufPos = 0;
		int aEntityLen = 0;
		int aImbrTag = 0;
		boolean aSpaceViewed = false;
		boolean aNormalize = false;
		int aLastLT = 0;
		try {
			parseState = 0;
			while (parseState >= 0) {
				if (aBufString == null || aBufStringPos == aBufString.length()) {
					readNext();
					continue;
				}
				switch (parseState) {
					case 6 : // In SCRIPT tag
						aTmpBuf[aTmpBufPos] = aBufString.charAt(aBufStringPos);
						if (aTmpBuf[aTmpBufPos] == '<') {
							aLastLT = aTmpBufPos;
						}
						aTmpBufPos++;
						aBufStringPos++;
						if (aLastLT >= 0 && new String(aTmpBuf, aLastLT, aTmpBufPos - aLastLT).toLowerCase().matches("<[ \t\n\r]*/[ \t\n\r]*script[ \t\n\r]*>")) {
							// Ok, end of script
							aStr = new String(aTmpBuf, 0, aLastLT);
							xObjects.add(new XObject(aStr, XObject.SCRIPT));
							xObjects.add(new XTag(new String(aTmpBuf, aLastLT, aTmpBufPos - aLastLT)));

							aTmpBufPos = 0;

							// To normal state
							parseState = 0;
							continue;
						}
						break;
					case 5 : // In comment tag
						aTmpBuf[aTmpBufPos] = aBufString.charAt(aBufStringPos);
						aTmpBufPos++;
						aBufStringPos++;
						if (new String(aTmpBuf, aTmpBufPos - 3, 3).equals("-->")) {
							// End of tag
							aStr = new String(aTmpBuf, 0, aTmpBufPos);// ,currentCharset);
							xObjects.add(new XObject(aStr, XObject.COMMENT));
							if (_TRACE) {
								System.err.println("Comment Tag:" + aStr);
							}
							aTmpBufPos = 0;

							// To normal state
							parseState = 0;
							continue;
						}
						break;
					case 4 : // In entity
						// Check for validity
						char aChar = aBufString.charAt(aBufStringPos);
						if (!(aChar == '&' || aChar == ';' || aChar == '#' || (aChar >= 'a' && aChar <= 'z') || (aChar >= 'A' && aChar <= 'Z') || (aChar >= '0' && aChar <= '9'))) {
							// No more in entity -> to normal state
							parseState = 0;
							continue;
						}
						aTmpBuf[aTmpBufPos] = aBufString.charAt(aBufStringPos);
						aTmpBufPos++;
						aBufStringPos++;
						aEntityLen++;
						if (aTmpBuf[aTmpBufPos - 1] == ';') {
							// End of entity
							String aEntity = new String(aTmpBuf, aTmpBufPos - aEntityLen, aEntityLen);
							String aTrans;
							// EM 06/06/2006 : char entities moved to XQualifier
							if ((aTrans = XQualifier.transcodeFromEntity(aEntity)) != null) {
								// System.out.println("Trans '" + aEntity + "'
								// -> '" + aTrans + "'");
								aTmpBufPos -= aEntityLen;
								for (int aTransPos = 0; aTransPos < aTrans.length(); aTransPos++) {
									aTmpBuf[aTmpBufPos] = aTrans.charAt(aTransPos);
									aTmpBufPos++;
								}
							}
							// aTmpBufPos -= aEntityLen;
							// byte[] aBytes;
							// aBytes = aStr.getBytes();
							// for(int i = 0;i < aBytes.length;i++)
							// {
							// aTmpBuf[aTmpBufPos] = aBytes[i];
							// aTmpBufPos++;
							// }

							// To normal state
							parseState = 0;
							continue;
						}
						break;
					case 3 : // In tag attribute
						aTmpBuf[aTmpBufPos] = aBufString.charAt(aBufStringPos);
						aTmpBufPos++;
						aBufStringPos++;
						if (aTmpBuf[aTmpBufPos - 1] == '\'') {
							// To tag state
							parseState = 1;
							continue;
						}
						break;
					case 2 : // In tag attribute
						aTmpBuf[aTmpBufPos] = aBufString.charAt(aBufStringPos);
						aTmpBufPos++;
						aBufStringPos++;
						if (aTmpBuf[aTmpBufPos - 1] == '\"') {
							// To tag state
							parseState = 1;
							continue;
						}
						break;
					case 1 : // In tag
						aTmpBuf[aTmpBufPos] = aBufString.charAt(aBufStringPos);
						aTmpBufPos++;
						aBufStringPos++;
						if (aTmpBufPos == 4 && new String(aTmpBuf, 0, 4).equals("<!--")) {
							// In comment tag
							parseState = 5;
							continue;
						}
						if (aTmpBuf[aTmpBufPos - 1] == '<') {
							// Imbricate
							aImbrTag++;
						} else if (aTmpBuf[aTmpBufPos - 1] == '>') {
							// Deimbricate
							aImbrTag--;
							if (aImbrTag <= 0) {
								// End of tag
								aStr = new String(aTmpBuf, 0, aTmpBufPos);// ,currentCharset);
								if (aStr.startsWith("<!")) {
									// Process tag, take it as comment for now
									xObjects.add(new XObject(aStr, XObject.COMMENT));
									if (_TRACE) {
										System.err.println("Comment Tag:" + aStr);
									}
								} else {
									XTag aXTag = new XTag(aStr);
									if (_TRACE) {
										System.err.println("Tag:" + aStr);
									}
									if (!charsetChecked) {
										// Search for charset..
										if (!isGoodCharset(aXTag)) {
											// Restart with new encoding !
											in.close();
											aBufString = null;
											return load(fileName);
										}
									}
									xObjects.add(aXTag);
									// Search for space normalisation
									if (!XQualifier.isInline(aXTag)) {
										// No more need of a space after a
										// structuration tag
										aSpaceViewed = true;
									}
									// Search for SCRIPT tag
									if (aXTag.tagName.equalsIgnoreCase("script")) {
										// In script
										parseState = 6;
										aTmpBufPos = 0;
										aLastLT = -1;
										continue;
									}
								}
								// System.err.println("Tag:" + aStr);
								aTmpBufPos = 0;

								// To normal state
								parseState = 0;
								continue;
							}
						} else if (aTmpBuf[aTmpBufPos - 1] == '\"') {
							// In attribute
							parseState = 2;
							continue;
						} else if (aTmpBuf[aTmpBufPos - 1] == '\'') {
							// In attribute
							parseState = 3;
							continue;
						}
						break;
					case 0 :
					default :

						if (newLineAsSpaces && (aBufString.charAt(aBufStringPos) == '\n' || aBufString.charAt(aBufStringPos) == '\r')) {
							if (!aSpaceViewed || !aNormalize) {
								if (aTmpBufPos > 0) {
									// EM 14/06/2006 : trans 1252 undef codes
									transUndefAs1252(aTmpBuf, aTmpBufPos);
									aStr = new String(aTmpBuf, 0, aTmpBufPos);// ,currentCharset);
									xObjects.add(new XObject(aStr, XObject.WORD));
									if (_TRACE) {
										System.err.println("Word:" + aStr);
									}
									aTmpBufPos = 0;
								}
								if(aNormalize){
									aStr = new String(" ");
								}
								else{
									aStr = aBufString.substring(aBufStringPos,aBufStringPos+1);
								}
								xObjects.add(new XObject(aStr, XObject.PUNCT));
								if (_TRACE) {
									System.err.println("Ponct:'" + aStr + "'");
								}
								aSpaceViewed = true;
							}
							aBufStringPos++;
							continue;
						} else if (aBufString.charAt(aBufStringPos) == '<') {
							if (aTmpBufPos > 0) {
								// EM 14/06/2006 : trans 1252 undef codes
								transUndefAs1252(aTmpBuf, aTmpBufPos);
								aStr = new String(aTmpBuf, 0, aTmpBufPos);// ,currentCharset);
								xObjects.add(new XObject(aStr, XObject.WORD));
								if (_TRACE) {
									System.err.println("Word:" + aStr);
								}
								aTmpBufPos = 0;
							}

							// In tag
							aImbrTag = 0;
							parseState = 1;
							continue;
						} else if (aBufString.charAt(aBufStringPos) == '&') {
							// In entity
							parseState = 4;
							aEntityLen = 0;
							// Suppose it's not a space equivalence
							aSpaceViewed = false;
							continue;
						} else if (XQualifier.isPonctChar(aBufString.charAt(aBufStringPos))) {
							// Ponct
							if (aTmpBufPos > 0) {
								// EM 14/06/2006 : trans 1252 undef codes
								transUndefAs1252(aTmpBuf, aTmpBufPos);
								aStr = new String(aTmpBuf, 0, aTmpBufPos);// ,currentCharset);
								xObjects.add(new XObject(aStr, XObject.WORD));
								if (_TRACE) {
									System.err.println("Word:" + aStr);
								}
								aTmpBufPos = 0;
							}
							if (aBufString.charAt(aBufStringPos) == ' ' || aBufString.charAt(aBufStringPos) == '\t') {
								if (aSpaceViewed && aNormalize) {
									// Delete
									aBufStringPos++;
									continue;
								}
								aSpaceViewed = true;
								// Norm
								if(aNormalize){
									aStr = new String(" ");
								}
								else{
									aStr = aBufString.substring(aBufStringPos,aBufStringPos+1);
								}
								xObjects.add(new XObject(aStr, XObject.PUNCT));
								if (_TRACE) {
									System.err.println("Ponct:'" + aStr + "'");
								}
								aBufStringPos++;
								continue;
							} else {
								aSpaceViewed = false;
							}
							// EM 14/06/2006 : trans 1252 undef codes
							aTmpBuf[aTmpBufPos] = aBufString.charAt(aBufStringPos);
							transUndefAs1252(aTmpBuf, aTmpBufPos + 1);
							aStr = new String(aTmpBuf, aTmpBufPos, aTmpBufPos + 1);// aBufString.substring(aBufStringPos,
																					// aBufStringPos
																					// +
																					// 1);//
																					// ,currentCharset);
							xObjects.add(new XObject(aStr, XObject.PUNCT));
							if (_TRACE) {
								System.err.println("Ponct:'" + aStr + "'");
							}
							aBufStringPos++;
							continue;
						}

						// In a word
						aTmpBuf[aTmpBufPos] = aBufString.charAt(aBufStringPos);
						aTmpBufPos++;
						aBufStringPos++;
						aSpaceViewed = false;
						break;
				}
			}
		} catch (Throwable t) {
			System.err.println("Warning : " + t);
			t.printStackTrace();
			return xObjects;
		}
		if (aTmpBufPos > 0) {
			// EM 14/06/2006 : trans 1252 undef codes
			transUndefAs1252(aTmpBuf, aTmpBufPos);
			aStr = new String(aTmpBuf, 0, aTmpBufPos);
			if (!aStr.matches("[ \t\n\r]+")) {
				xObjects.add(new XObject(aStr, XObject.WORD));
				System.err.println("Warning, soemthing left at the end of document : " + aStr);
			}
		}

		return xObjects;
	}

	/**
	 * Default saving with stored objects
	 * 
	 * @param aOutFileName
	 */
	public void save(String aOutFileName) {
		save(aOutFileName, xObjects);
	}

	/**
	 * Saving of new objects with loading settings (charset)
	 * 
	 * @param aOutFileName
	 */
	public void save(String aOutFileName, Vector aXObjects) {
		boolean aNeedEntities = !(currentCharset.toLowerCase().indexOf("utf") >= 0);
		String aStr;
		XObject aXO;
		try {
			BufferedWriter aOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(aOutFileName), currentCharset));
			for (int i = 0; i < aXObjects.size(); i++) {
				aXO = ((XObject) aXObjects.elementAt(i));
				aStr = aXO.text;
				switch (aXO.kind) {
					case XObject.COMMENT :
					case XObject.TAG :
						// Do not transcode !
						break;
					case XObject.PUNCT :
					case XObject.WORD :
						// EM 09/06/2006 : trouble correction for "&", "<" and
						// ">" transco
						int aPos = 0;
						while ((aPos = aStr.indexOf("&", aPos)) >= 0) {
							int aLen = XQualifier.entityLen(aStr, aPos);
							if (aLen > 0) {
								// Ok entity do not replace
								aPos++;
								continue;
							}
							// replace
							//EM 30/06/2006, small bug : bad recombination
							//EM 03/07/2006 : rewrited
							aStr = ((aPos > 0) ? aStr.substring(0, aPos) : "")
								+ "&amp;" 
								+ ((aPos < aStr.length() - 1) ? aStr.substring(aPos + 1) : "");//aLen);
							aPos++;
						}
						if (aStr.indexOf("<") >= 0) {
							aStr = aStr.replaceAll("<", "&lt;");
						}
						if (aStr.indexOf(">") >= 0) {
							aStr = aStr.replaceAll(">", "&gt;");
						}
						if (aNeedEntities) {
							// EM 06/06/2006 : char entities moved to XQualifier
							aStr = XQualifier.transcodeToEntities(aStr, charsetChecked);
						}
						break;
					case XObject.UNKNOWN :
					default :
						System.out.println("Warning, unknown XObject kind for : " + aStr);
				}
				aOut.write(aStr);
			}
			aOut.flush();
			aOut.close();
		} catch (Throwable t) {
			System.err.println("Can't write output file '" + aOutFileName + "'"+ t);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Reading " + args[0]);
		XLoader aXLoader = new XLoader();
		Vector aXObjects = aXLoader.load(args[0]);

		String aOutFileName = args[0];
		if (aOutFileName.lastIndexOf(".") >= 0) {
			aOutFileName = aOutFileName.substring(0, aOutFileName.lastIndexOf(".")) + ".TR" + aOutFileName.substring(aOutFileName.lastIndexOf("."));
		}
		System.out.println("Wrinting " + aOutFileName);
		aXLoader.save(aOutFileName);

		System.out.println("Dump file begin for " + aXObjects.size() + " objects");
		System.out.println("-----------");
		for (int i = 0; i < aXObjects.size(); i++) {
			System.out.print(((XObject) aXObjects.elementAt(i)).text);
		}
		System.out.println("-----------");
		System.out.println("Dump file end");
	}
}