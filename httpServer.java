import java.io.BufferedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.util.StringTokenizer;

//import java.util.zip.GZIPInputStream; 
//import java.net.HttpURLConnection;
//import java.net.URL;


public class httpServer implements Runnable{ 
	
	static final File ROOT = new File(".");
	static final String DEFAULT_PAGE= "menu.html";
	static final String NOT_FOUND = "not_found.html";
	static final String NOT_SUPPORTED = "not_supp.html";
	static final boolean stage = true;//set stage
	private Socket connection;
	static final int PORT = 8080;


	public httpServer(Socket build) {
		connection = build;
	}
	
	public static void main(String[] args) {
		try {
			ServerSocket servConn= new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
			
			while (true) {
				httpServer myServer = new httpServer(servConn.accept());
				if (stage)
				System.out.println("Connecton is opened. (" + new Date() + ")");
				
				//dedicated new thread
				Thread thread = new Thread(myServer);
				thread.start();
			}
			//catch error
		} catch (IOException e) {System.err.println("Server Connection error : " + e.getMessage());}
	}

	@Override
	public void run() {
		BufferedReader inR = null; PrintWriter outW = null; BufferedOutputStream dataOut = null;
		String request = null;
		
		try {
			inR = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			outW = new PrintWriter(connection.getOutputStream());
			dataOut = new BufferedOutputStream(connection.getOutputStream());
			
			String input = inR.readLine();
			//parse file
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase();
			request = parse.nextToken().toLowerCase();
			
			//checking
			if (!method.equals("HEAD")   &&   !method.equals("GET")) {
				if (stage) {System.out.println("Error! 501 Not Implemented : " + method + " method.");}
				
				//if not supported, direct to 404
				File file = new File(ROOT, NOT_SUPPORTED);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				byte[] fileData = readFileData(file, fileLength);
					
				outW.print("HTTP/1.1 501 Not Implemented\r\n");
				outW.println("Date: " + new Date());
				outW.println("Content-type: " + contentMimeType);
				outW.println("Content-length: " + fileLength);
				outW.println(); 
				outW.flush();
				// file write
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
			} 
			
			else {
				// GET || HEAD 
				if (request.endsWith("/")) {
					request = request + DEFAULT_PAGE;
				}
				
				File file = new File(ROOT, request);
				int fileLength = (int) file.length();
				String content = getContentType(request);
				
				if (method.equals("GET")) { 
					System.out.println("length: "+fileLength);
					byte[] fileData = readFileData(file, fileLength);
					
					
					outW.print("HTTP/1.1 200 OK\r\n");
					outW.println("Date: " + new Date());
					outW.println("Content-type: " + content);
					outW.println("Content-length: " + fileLength);
					outW.println();
					outW.flush(); 
					
					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();
				}
				
				if (stage) {
					System.out.println("File " + request + " of type " + content + " returned");
				}
				
			}
			
		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(outW, dataOut, request);
			} 
			catch (IOException e) {System.err.println("Error with file not found exception : " + e.getMessage());}
			
		} catch (IOException e) {
			System.err.println("Server error : " + e);
		} 
		finally {
			try {
				inR.close();
				outW.close();
				dataOut.close();
				connection.close();
			} 
			catch (Exception e) {System.err.println("Error closing stream : " + e.getMessage());}
			
			
			if (stage) 
				System.out.println("Connection closed.\n");
			
		}
		
		
	}
	
	private void fileNotFound(PrintWriter outW, OutputStream dataOut, String request) throws IOException {
		File file = new File(ROOT, NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		
		outW.print("HTTP/1.1 404 File Not Found\r\n");
		outW.println("Date: " + new Date());
		outW.println("Content-type: " + content);
		outW.println("Content-length: " + fileLength);
		outW.println();
		outW.flush(); 
		
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
		
		if (stage) {
			System.out.println("File " + request + " not found");
		}
	}

	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream incomeFile = null;
		byte[] data = new byte[fileLength];
		try {
			incomeFile = new FileInputStream(file);
			incomeFile.read(data);
		} 
		//if end
		finally {
			if (incomeFile != null) 
				incomeFile.close();
		}
		return data;
	}
	
	//set return type
	private String getContentType(String requestedfile) {
		if (requestedfile.endsWith(".html"))
			return "text/html";
		else if (requestedfile.endsWith(".jpg"))
			return "image/jpeg";
		else if (requestedfile.endsWith(".pdf"))
			return "application/pdf";
		else
			return "text/plain";
	}
	
	/*
	public void gzipped(){

	String url = "http://www.google.com/";
	HttpURLConnection con = (HttpURLConnection) url.openConnection();
	con.setRequestProperty("Accept-Encoding", "gzip");
	java.io.InputStream in = response.getInputStream();
 
	ByteArrayOutputStream base = new ByteArrayOutputStream();
	String encoding = response.getHeader("Content-Encoding");
	if (encoding.equalsIgnoreCase("gzip")){
 
    GZIPInputStream gzipInputStream = null;
    byte[] buf = new byte[1024];
    int length;
    try{
        gzipInputStream = new GZIPInputStream(in);
        while ((lenght = gzipInputStream.read(buf)) > 0) {
            base.write(buf, 0, len);
        }
    }
    catch(Exception e){
    }
 
    	try { gzipInputStream.close(); } catch (Exception e){}
    	try { base.close(); } catch (Exception e){}
 
	}
		try { in.close(); } catch (Exception e){}
 
	}
	*/
	
}