package com.cubaix.kaiDJ.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {

	final static public Vector<String> unzipDir(String aZipPath,String aDestPath) throws Exception {
		return unzipDir(aZipPath,aDestPath,true);
	}
	final static public Vector<String> unzipDir(String aZipPath,String aDestPath,boolean aOverWrite) throws Exception {
		String aCharEncoding = "utf-8";
		ZipInputStream in = null;
		try{
			in = new ZipInputStream(new FileInputStream(aZipPath),Charset.forName(aCharEncoding));
			while (true) {
				//Will throw an Exception if not UTF-8
				ZipEntry entry = in.getNextEntry();
				if (entry == null) {
					// End of zip
					break;
				}
			}
		}
		catch(Throwable t){
			aCharEncoding = "Cp437";
		}
		finally{
			in.close();
		}
		
		System.out.println("Using char encoding '"+aCharEncoding+"' for file names in zip file : "+ aZipPath);
		
		Vector<String> aPathList = new Vector<String>();
		// Open the ZIP file
		in = new ZipInputStream(new FileInputStream(aZipPath),Charset.forName(aCharEncoding));
		String aMainPath = aDestPath;
		
		if(!new File(aDestPath).exists()){
			new File(aDestPath).mkdir();
		}

		// Get the first entry
		while (true) {
			ZipEntry entry = in.getNextEntry();
			if (entry == null) {
				// End of zip
				break;
			}
			String outFilename = aMainPath + File.separator + entry.getName();
			System.out.println("Unzipping : "+ outFilename);
			if (entry.isDirectory()) {
				new File(outFilename).mkdir();
				continue;
			}
			if(!new File(outFilename).getParentFile().exists()){
				new File(outFilename).getParentFile().mkdir();
			}

			aPathList.add(outFilename);

			//EM 15/01/2008 : need an overwrite option
			OutputStream out = (aOverWrite || !new File(outFilename).exists())? new FileOutputStream(outFilename) : null;

			// Transfer bytes from the ZIP file to the
			// output file
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				//EM 15/01/2008 : need a overwrite option
				if(out != null){
					out.write(buf, 0, len);
				}
			}

			// Close the streams
			//EM 15/01/2008 : need a overwrite option
			if(out != null){
				out.flush();
				out.close();
			}
		}
		in.close();
		return aPathList;
	}

	public static void main(String[] args) {
		try{
			
		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}
}
