package little.http.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import little.http.HTTPConnection;

/**
 * A <code>Example</code> is just an easy example show how easy it is.
 *  
 * @author feijian_zhuo
 * @version 2010-06-05
 * @email allthetime10000@gmail.com
 */

public class Example {
	public static void main(String[] argv) throws UnknownHostException,IOException {
		
		HTTPConnection hc = new HTTPConnection("www.265.com",80);

		hc.writeRequestLine("GET","/Xinrui_KuZhan/");
		hc.writeHeader("Connection: keep-alive");
		hc.writeHeader("Host: " + "www.265.com");
		hc.finishRequest();

		System.out.println(hc.readResponseLine());
		
		String header = hc.readHeader();
		for (; header.length() > 0;) {
			System.out.println(header);
			header = hc.readHeader();
		}

		InputStream bi = hc.getHTTPBodyInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(bi));
		String line = br.readLine();
		for (; line != null; line = br.readLine()) {
			System.out.println(line);
		}
		br.close();
	}
}
