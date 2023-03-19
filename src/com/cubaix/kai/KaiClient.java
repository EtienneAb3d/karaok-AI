package com.cubaix.kai;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import com.cubaix.kaiDJ.KaiDJ;
import com.cubaix.kaiDJ.utils.FileUtils;

public class KaiClient {
	KaiDJ parentKDJ = null;
	String path = null;
	String taskId = null;
	
	public KaiClient(KaiDJ aParentKDJ) {
		parentKDJ = aParentKDJ;
	}
	
	XmlRpcClient getXmlRcpClient() throws Exception {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL("http://odyssee.neurospell.com:8991"));
		config.setEnabledForExtensions(false);
		config.setConnectionTimeout(60 * 1000);//1mn
		config.setReplyTimeout(10 * 60 * 60 * 1000);//10h
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);
		return client;
	}
	
	public void getCode() throws Exception {
		XmlRpcClient client = getXmlRcpClient();
		Object[] params = new Object[] {parentKDJ.userEMail};
        System.out.println("GET KAI CODE...");
		String aRep = (String)client.execute("Middleware.kaiCode",params);
		System.out.println("GetCode reply: "+aRep);
	}
	
	public String extract(String aPath,String aLng) throws Exception {
		path = aPath;
		XmlRpcClient client = getXmlRcpClient();
        File aFile = new File(aPath);
        System.out.println("LOADING: "+aFile.getCanonicalPath());
        FileInputStream fileInputStream;
        byte[] aBuf = null;
        try {
            fileInputStream = new FileInputStream(aFile);
            aBuf = new byte[(int) aFile.length()];
            fileInputStream.read(aBuf);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(ZipOutputStream zos = new ZipOutputStream(baos)) {

          /* File is not on the disk, test.txt indicates
             only the file name to be put into the zip */
          ZipEntry entry = new ZipEntry(aFile.getName()); 
          zos.putNextEntry(entry);
          zos.write(aBuf);
          zos.closeEntry();
        } 
        catch(IOException ioe) {
          ioe.printStackTrace();
        }
		Object[] params = new Object[] {parentKDJ.userEMail,parentKDJ.userCode,baos.toByteArray(),aLng,parentKDJ.userEMail.replaceAll("[@.]","_")};
        System.out.println("UPLOADING...");
        String aRes = (String)client.execute("Middleware.kai",params);
        System.out.println("Res: "+aRes);
        return aRes;
	}
	
	public String state() throws Exception {
		XmlRpcClient client = getXmlRcpClient();
        System.out.println("STATE: "+taskId);
		Object[] params = new Object[] {parentKDJ.userEMail,parentKDJ.userCode,taskId,parentKDJ.userEMail.replaceAll("[@.]","_")};
		String aRes = (String)client.execute("Middleware.kaiState",params);
        System.out.println("Res: "+aRes);
		return aRes;
	}
	
	public void get() throws Exception {
		XmlRpcClient client = getXmlRcpClient();
        System.out.println("GET: "+taskId);
		Object[] params = new Object[] {parentKDJ.userEMail,parentKDJ.userCode,taskId,parentKDJ.userEMail.replaceAll("[@.]","_")};
		byte[] aKai = (byte[])client.execute("Middleware.kaiGet",params);
		FileOutputStream aFOS = new FileOutputStream(path+".kai");
		aFOS.write(aKai);
		aFOS.flush();
		aFOS.close();
        System.out.println("SAVED");
        FileUtils.unzipDir(path+".kai", new File(path).getParentFile().getCanonicalPath(), true);
        System.out.println("UNZIPPED");
	}

	public static void main(String[] args) {
		try {
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

}
