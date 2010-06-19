package little.http;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;



/**
 * A <code>HTTPConnection</code> is used to establish an TCP/IP
 * connection to a HTTP server.
 *  
 * @author feijian_zhuo
 * @version 2010-06-05
 * @email allthetime10000@gmail.com
 */

public class HTTPConnection {
	
	private Socket socket;
	private final HTTPWriter hw;
	private final HTTPReader hr;

	public HTTPConnection(String host, int port) throws UnknownHostException, IOException {
		socket = new Socket(host,port);
		hw = new HTTPWriter(this,socket.getOutputStream());
		hr = new HTTPReader(this,socket.getInputStream());
	}
	
	public void close() throws IOException{
		socket.close();
	}
	
	public boolean isClosed(){
		return socket.isClosed();
	}
	
	public void writeRequestLine(String method,String url){
		this.writeRequestLine(method, url,"HTTP/1.1");
	}
	
	public void writeRequestLine(String method,String url,String version){
		hw.writeRequestLine(method, url, version);
	}
	
	public void writeHeader(String header){
		hw.writeHeader(header);
	}
	
	public void finishRequest() throws IOException{
		hw.finishRequest();
	}
	
	public void writeBody(byte[] bs){
		hw.writeBody(bs);
	}
	
	public void flush() throws IOException{
		hw.flush();
	}
	
	public String readResponseLine() throws IOException {
		return hr.readResponseLine();
	}
	
	public String readHeader() throws IOException {
		return hr.readHeader();
	}
	
	public InputStream getHTTPBodyInputStream() throws IOException {		
		return hr.getHTTPBodyInputStream();
	}
}
