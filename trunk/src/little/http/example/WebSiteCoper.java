package little.http.example;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.io.RandomAccessFile;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import little.http.HTTPConnection;

/**
 * A <code>WebSiteCoper</code> is used to copy a web site to a dirtory.
 *  
 * @author feijian_zhuo
 * @version 2010-06-05
 * @email allthetime10000@gmail.com
 */
public class WebSiteCoper {
	public static Pattern uri_pattern1 = Pattern.compile("(href=|src=){1}[=?\"\'\\w\\.\\-/:]+(\\s|>){1}");//<img src="images/mtimg.gif"/>
	public static Pattern uri_pattern2 = Pattern.compile("(url\\(){1}[=?\"\'\\w\\.\\-/]+(\\)){1}"); //url(images/index.jpg)
	String host;
	HTTPConnection hc;

	Map<String, String> urlMap = new HashMap<String, String>();
	List<String> urls = new LinkedList<String>();

	private void addOne(String uri) throws IOException {
		if (urlMap.containsKey(uri))
			return;
		System.out.println("add one uri = " + uri);
		urlMap.put(uri, uri);
		urls.add(uri);
	}

	private void sendOne(String uri) throws IOException {
		hc.writeRequestLine("GET", uri);
		hc.writeHeader("Connection: keep-alive");
		hc.writeHeader("Host: " + host);
		hc.finishRequest();
	}

	public void copyWebSite(String host, int port, String savePath)
			throws UnknownHostException, IOException {
		this.host = host;
		hc = new HTTPConnection(host, port);

		addOne("/");// seed

		for (; urls.size() > 0;) {


			String encode = "UTF-8";
			String Content_Type = null;
			String Location =null;
			
			String uri = urls.remove(0);
			String base_uri = uri.substring(0, uri.lastIndexOf("/")) + "/";
			
			new File(savePath + base_uri).mkdirs();
			
			//send the request
			sendOne(uri);

			String rl;
			try {
				rl = hc.readResponseLine();
			} catch (Exception e) {
				if (hc.isClosed()) {
					hc = new HTTPConnection(host, port);
					sendOne(uri);
					rl = hc.readResponseLine();
				} else
					throw new RuntimeException(e);
			}
			
			System.out.println( uri + " = " + rl);
			
			String status = rl.split(" ")[1].trim();

			String header = hc.readHeader();
			for (; header.length() > 0;) {
				if (header.startsWith("Content-Type"))
					Content_Type = header.substring(header.indexOf(":") + 1).trim();
				if (header.startsWith("Location"))
					 Location = header.substring(header.indexOf(":") + 1).trim();
				header = hc.readHeader();
			}
			
			
			if(status.startsWith("1") || status.equals("204")|| status.equals("304")){//no body
				System.out.println("fuck" + status + uri);
				continue;
				
			}
			

			InputStream bi = hc.getHTTPBodyInputStream();
			
			if("404".equals(status)){
				bi.close();
				continue;
			}
			
			if("301".equals(status)){//another page,this site
				bi.close();
				Location = Location.substring(7);
				String Host = Location.substring(0,Location.indexOf("/"));
				Location = Location.substring(Location.indexOf("/"));
				
				if(Host.equals(host))
					addOne(Location);
				
				continue;
			}
			
			if("302".equals(status)){// another page this site
				bi.close();
				Location = Location.substring(7);
				String Host = Location.substring(0,Location.indexOf("/"));
				Location = Location.substring(Location.indexOf("/"));
				
				if(Host.equals(host))
					addOne(Location);
				
				continue;
			}

			if(uri.indexOf('?')>0)//delete parameter
				uri = uri.substring(0,uri.indexOf('?'));
			
			if (uri.endsWith("/"))
				uri = uri + "index.html";

			File f = new File(savePath + uri);
			if (!f.exists()){
				try {
					f.createNewFile();
				} catch(Exception e){
					e.printStackTrace();
					System.out.println(savePath + uri);
				}
			}
			RandomAccessFile raf = new RandomAccessFile(f, "rw");

			if (uri.indexOf(".html") !=-1 || uri.indexOf(".css") != -1 || uri.indexOf(".php")!=-1) {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(bi));
				String line = br.readLine();
				for (; line != null; line = br.readLine()) {
					raf.write(line.getBytes());
					raf.write("\r\n".getBytes());
					
					// deal something like "<base href="http://www.examplexxxxxxxx.com/" />
					if( line.startsWith("<base href=") ) {
							String new_base = line.substring(12, (line.lastIndexOf("\"")>0?line.lastIndexOf("\""):line.lastIndexOf("'")) );
							if( new_base.startsWith("http://") ){
								base_uri = new_base.substring(7);
								base_uri = base_uri.substring(base_uri.indexOf("/"));
							}
					}
					
					
					Matcher matcher =  uri_pattern1.matcher(line);
					while (matcher.find()) {
						String new_uri = matcher.group();

						int lastIndex = new_uri.lastIndexOf("/>");
						if (lastIndex == -1)
							lastIndex = new_uri.lastIndexOf(">");
						if (lastIndex == -1)
							lastIndex = new_uri.lastIndexOf(" ");

						new_uri = new_uri.substring(new_uri.indexOf("=") + 2,
								lastIndex - 1).trim();

						if (new_uri.length() > 0) {
							if (new_uri.startsWith("http://"))
								continue;
							else if (!new_uri.startsWith("/"))
								new_uri = base_uri + new_uri;
							addOne(new_uri);
						}
					}
					
					matcher =  uri_pattern2.matcher(line);
					while (matcher.find()) {
						String new_uri = matcher.group();
						new_uri = new_uri.substring(4,new_uri.lastIndexOf(")"));
						if (new_uri.length() > 0) {
							if (new_uri.startsWith("http://"))
								continue;
							else if (!new_uri.startsWith("/"))
								new_uri = base_uri + new_uri;
							addOne(new_uri);
						}
					}	
				}
				br.close();
			} else {
				byte[] buf = new byte[256];
				int read = bi.read(buf);
				while (read != -1){
					raf.write(buf, 0, read);
					read = bi.read(buf);
				}
				bi.close();
			}
			raf.close();
		}

	}

	public static void main(String[] argv) throws UnknownHostException,IOException {
		WebSiteCoper main = new WebSiteCoper();
		main.copyWebSite("www.freeloongson.com", 80, "E:/test3");
	}
}
