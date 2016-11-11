/***************************************************************************************************************
 * COEN 317 Winter2016 HW1 - Webserver
 * Author : Shilpita Roy (sroy@scu.edu)
 * Date Submission : Jan-28-2016
 * Purpose: This is a Code for Web server which performs the following
 * 			1. Opens a Socket
 * 			2. Listens over the socket for Client Requests
 * 			3. Upon receiving the Request it processes and sends response using Multi-threading
 * 			4. By Default the socket once established is "Keep-alive" for both HTTP 1.0 and HTTP 1.1
 * 			5. If specified Connection : close then the socket closes after serving each request.
 * 			6. Errors handled -> 400 Bad request, 403 Forbidden , 404 Not Found , 501 - Unimplemented Method
 * ****************************************************************************************************************/


import java.io.*;
import java.net.*;
import java.util.*;

public final class MyWebServer {
	private static int port ;  //= 8000 ;
	
	private static String document_root = ""; //= "C:/www.scu.edu/";
	
	private final static String myServerName = "Server: COEN317Rocks/0.1";
	
	//Create a Hash-map to Map the MIME supported and sent in the ResponseHeader
	private static Map<String, String> mimeMapping = new HashMap<String, String>() {
		
		private static final long serialVersionUID = 1L;

			{
				put("html", "text/html");
				put("css", "text/css");
				put("js", "application/js");
				put("jpg", "image/jpg");
				put("jpeg", "image/jpeg");
				put("png", "image/png");
				put("pdf","application/pdf");
			}
	 };
	
	//Create a Hash-map to Map the Code Descriptors supported and sent in the ResponseHeader
	private static Map<String, String> descriptorMap = new HashMap<String, String>() {
		
		private static final long serialVersionUID = 1L;

		{
			put("200", "OK");
			put("400", "Bad Request");
			put("403", "Forbidden");
			put("404", "Not Found");
			put("415", "Unsupported Media Type");
			put("501", "Not Implemented");
		}
	};
	
	// function created to form the Response Header for HTTP request
	private static void responseHeader(String protocol,String code, String mime, int length,String connection_type ,DataOutputStream out) throws Exception {
				
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		Calendar cal=Calendar.getInstance(TimeZone.getDefault());
		Date date = cal.getTime();
		
		out.writeBytes(protocol+ " " + code + " " +descriptorMap.get(code) +"\r\n");
		out.writeBytes("Date: "+ date + "\r\n");
		out.writeBytes("Content-Type: " + mimeMapping.get(mime) + "\r\n");
		out.writeBytes("Content-Length: " + length + "\r\n"); 
		out.writeBytes("Connection: "+ connection_type + "\r\n");
		out.writeBytes("Expires:Sun, 31 Dec 2017 16:00:00 GMT" + "\r\n");
		out.writeBytes(myServerName);
		out.writeBytes("\r\n\r\n");
	}
 
	//Member Function to Handle the Request from Client and send Response
	private static String requestHandler(BufferedReader in, DataOutputStream out) throws Exception { 
		
		String inrequestline = null;    //request line
		String inheader      = null ;   //request header 

		String method         = null; 
		String reqURL         = null; 
        String protocol       = null; 
        
        String connection_type   = null;
        String errresponseString = null;
		
		boolean done = false;
		
		inrequestline = in.readLine();
		
		if(inrequestline == null)
			return "close";
		
		// read entire header
	    while (!done) {
	    	
	    	inheader = in.readLine();
            
             if (inheader.startsWith("Connection") && inheader.endsWith("close"))  
				    connection_type = "close";
        	
            if (inheader.length() == 0)
				 done = true;   
        }
	    
		//Split the Client Requested Header  
		String[] tokens  = inrequestline.split(" "); 
	
		if(tokens.length < 3) {
			
			errresponseString = "400 Bad Request";
			connection_type = "close";
			responseHeader("HTTP/1.0","400", "html", errresponseString.length(), connection_type, out);
			out.write(errresponseString.getBytes());
			System.out.println("400 Wrong token number");
			return connection_type;
		}
		
		method   = tokens[0];
		reqURL   = tokens[1];  
		protocol = tokens[2];
		  
		//  System.out.println("Requested method:"+tokens[0]);
		//  System.out.println("Requested URL:"+tokens[1]);
		//  System.out.println("Protocol:"+tokens[2]);
	 
		if (!(protocol.equals("HTTP/1.0") || protocol.equals("HTTP/1.1"))){
			
			  errresponseString = "400 Bad Request";
			  connection_type = "close";
			  responseHeader(protocol, "400", "html", errresponseString.length(), connection_type, out);
			  out.write(errresponseString.getBytes());
			  return connection_type;
		 }	
		 
		if (protocol.equals("HTTP/1.0"))
			connection_type = "close";
		else
			connection_type = "keep-alive";
		
	
		  if (!method.equals("GET")) {
			  
			  errresponseString = "501 Requested Method Not Implemented";
			  responseHeader(protocol,"501", "html", errresponseString.length(), connection_type, out);
			  out.write(errresponseString.getBytes());
			  return connection_type; 
		  }
		  
		//Set URL for the Request to Index.Html
			
		  if (reqURL == "")
				  reqURL = "index.html";
		  else if (reqURL.endsWith("/"))
				  reqURL+="index.html";
			  
		  String mime = reqURL.substring(reqURL.lastIndexOf(".")+1); 
			  //System.out.println("MediaFile:"+mime);
			 
		  try {

				    // Open file
					byte[] fileBytes = null;
					FileInputStream is = new FileInputStream(document_root+reqURL);
					fileBytes = new byte[is.available()];
					is.read(fileBytes);
					
					// Send header
					responseHeader(protocol, "200", mime, fileBytes.length, connection_type ,out);
					
					// Write content of file
					out.write(fileBytes);
				
			} catch(FileNotFoundException e) {
					
				    String errStr = e.getMessage();
				    System.out.println(errStr);
				    
				    if (errStr.contains("No such file or directory")){
				    	
				    	errresponseString = "404 File Not Found";
						responseHeader(protocol, "404", "html", errresponseString.length(), connection_type, out);
						out.write(errresponseString.getBytes());
						
				    } else if (errStr.contains("Permission denied")) {
				    	
				    	errresponseString = "403 Forbidden Access";
						responseHeader(protocol, "403", "html", errresponseString.length(), connection_type, out);
						out.write(errresponseString.getBytes());
				    }
				}
		  
		  return connection_type;
	}	  
	
	// WorkerThread Class definition  for request handling 
	private static class WorkerThread implements Runnable {

		protected Socket socket = null;

		BufferedReader in;
		DataOutputStream out;
		String connection_type; 
		
	
		public WorkerThread(Socket connectionSocket)  {
			this.socket = connectionSocket;
		}

		public void run() {
			
			boolean done = false;
			
			try {
				
			    while (!done) {
			    	
				      this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				
				      this.out = new DataOutputStream(this.socket.getOutputStream());
				
				      this.connection_type = requestHandler(this.in, this.out);

				      this.out.flush();
			    
				      this.in.mark(0);
				      
				      this.in.reset();
			    
				      if (this.connection_type == "close") {
				    	  this.out.close();
				    	  this.in.close();
				    	  done = true;
				      } else {
				    	  this.socket.setSoTimeout(5000); // persistent connection for 5 secs.
				      }
				      
		         }
			    	
			} catch (Exception e) { 
		         
				System.out.println(e.getMessage() + " : closing connection");  //"Time Out.Socket closing");
				
				try {
					this.out.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		    	 try {
					this.in.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
			} 
		}
	}
	
	public static void main(String[] args)throws Exception {
		
		
		if(args.length < 2) {
			System.out.println("Insufficent arguments");
			return;
		}
		
			
		document_root = args[0];
		port = Integer.parseInt(args[1]);
	
		
		//Create a Web socket 
		ServerSocket myServerSocket = new ServerSocket(port);
		
		try {	
			while(true){
				
					Socket connectionSocket = myServerSocket.accept(); 
					new Thread(new WorkerThread(connectionSocket)).start(); // creates thread for request handling
			}
		}	catch(SocketTimeoutException e) {
			
			System.out.println(e.getMessage());
			
		}
		
		myServerSocket.close();
	    myServerSocket = null;
     }
}
