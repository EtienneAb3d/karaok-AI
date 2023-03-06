package com.cubaix.kaiDJ.xml;

public class XObject
{
    public static final int UNKNOWN = -1;
    public static final int WORD = 0;
    public static final int PUNCT = 1;
    public static final int TAG = 2;
    public static final int COMMENT = 3;
    public static final int SCRIPT = 3;

    public String text;
    public int kind = UNKNOWN;

    /**
     * 
     */
    public XObject()
    {
    }

    /**
     * @param aStr
     * @param aKind
     */
    public XObject(String aStr,int aKind)
    {
	text = aStr;
	kind = aKind;
	//	System.err.println("ABSgO " + aKind + " = " + text);
    }

    /**
     * @return
     */
    public XObject getClone(){
    	return new XObject(text,kind);
    }
}
