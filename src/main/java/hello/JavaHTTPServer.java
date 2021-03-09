package hello;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;

public class JavaHTTPServer implements Runnable{ 

	static final int PORT = 8080;

	private Socket connect;
	private String method;
  	private String path;
	private String fullUrl;
	private Map<String, String> headers = new HashMap<String, String>();
	private Map<String, String> queryParameters = new HashMap<String, String>();
	
	public JavaHTTPServer(Socket c) {
		connect = c;
	}
	
	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);

			System.out.println("Server started.\nListening for connections on port : http://localhost:" + PORT + " ...\n");
			
			// we listen until user halts server execution
			while (true) {
				JavaHTTPServer myServer = new JavaHTTPServer(serverConnect.accept());
				Thread thread = new Thread(myServer);
				thread.start();
			}
			
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		BufferedReader in = null; 
		PrintWriter out = null; 
		BufferedOutputStream dataOut = null;
		
		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			out = new PrintWriter(connect.getOutputStream());
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			//Get all Parameters
			parse(in);
			System.out.println(method);
			System.out.println(path);
			System.out.println(queryParameters);


			if (!method.equals("GET")) {
				out.println("HTTP/1.1 501 Not Implemented");
				out.println(); 
				out.flush();
				// file
				dataOut.flush();
				
			} else {
				StringBuilder responseContent = new StringBuilder();

				switch(path) {

					case "/cmdInjection":
						String testQ = queryParameters.get("cmd");
						String[] command = { "/bin/bash", "-c", "" + testQ };
						Process process = Runtime.getRuntime().exec(command);
						BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
						String tmp;
						while ((tmp = reader.readLine()) != null) {
							responseContent.append(tmp).append("\n");
						}
						break;

					case "/xss":
						String xssQuery = queryParameters.get("cmd");

						responseContent.append("<html><bod>"+xssQuery+"</body></html>");
						break;

					case "/pathtraversal":
						File path = new File(queryParameters.get("file"));
						if (path.exists()) {
							responseContent.append(Files.readString(path.toPath()));
						} else {
							responseContent.append("404 Not Found");
						}
					default:
						responseContent.append("200");
				}

					// send HTTP Headers
					out.println("HTTP/1.1 200 OK");
					out.println("Date: " + new Date());
					out.println("Content-length: " + responseContent.toString().getBytes().length);
					out.println(); 
					out.flush();
					
					dataOut.write((responseContent.toString().getBytes()));
					dataOut.flush();
			}
			
		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connect.close();
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 

		}
		
		
	}

	  private void parseQueryParameters(String queryString)  {
		for (String parameter : queryString.split("&"))  {
		  int separator = parameter.indexOf('=');
		  if (separator > -1)  {
		  	//URL Decode		
			try{
				parameter=java.net.URLDecoder.decode(parameter, "UTF-8");
	
			}
			catch(IOException ioe){
			}

			queryParameters.put(parameter.substring(0, separator),
			  parameter.substring(separator + 1));
		  } else  {
			queryParameters.put(parameter, null);
		  }
		}
	  }

	public boolean parse(BufferedReader in) throws IOException {
		String initialLine = in.readLine();
		
		StringTokenizer tok = new StringTokenizer(initialLine);
		String[] components = new String[3];
		for (int i = 0; i < components.length; i++) {
		  if (tok.hasMoreTokens())  {
			components[i] = tok.nextToken();
			System.out.println(components[i]);
		  } else  {
			return false;
		  }
		}
	
		method = components[0];
		fullUrl = components[1];
	
		// Consume headers
		while (true)  {
		  String headerLine = in.readLine();
		  if (headerLine.length() == 0) {
			break;
		  }
	
		  int separator = headerLine.indexOf(":");
		  if (separator == -1)  {
			return false;
		  }
		  headers.put(headerLine.substring(0, separator),
			headerLine.substring(separator + 1));
		}
	
		if (components[1].indexOf("?") == -1) {
		  path = components[1];
		} else  {
		  path = components[1].substring(0, components[1].indexOf("?"));
		  parseQueryParameters(components[1].substring(
			components[1].indexOf("?") + 1));
		}
	
		if ("/".equals(path)) {
		  path = "/index.html";
		}
	
		return true;
	  }
	
}