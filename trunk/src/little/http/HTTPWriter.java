package little.http;




import java.io.IOException;
import java.io.OutputStream;

/**
 * A <code>HTTPWriter</code> is used to encode http request to an TCP/IP
 * connection to a HTTP server.
 *  
 * @author feijian_zhuo
 * @version 2010-06-05
 * @email allthetime10000@gmail.com
 */


public class HTTPWriter
{
	HTTPConnection hc;
	byte arr[];
	int pos;
	
	OutputStream os;

	HTTPWriter(HTTPConnection hc,OutputStream os)
	{
		this.hc = hc;
		arr = new byte[256];
		pos = 0;
		this.os = os;
	}

	void writeRequestLine(String method,String url){
		this.writeRequestLine(method, url,"HTTP/1.1");
	}
	
	void writeRequestLine(String method,String url,String version){
		writeBytes(method.getBytes());
		writeSP();
		writeBytes(url.getBytes());
		writeSP();
		writeBytes(version.getBytes());
		writeCRLF();
	}
	
	void writeHeader(String header){
		writeBytes(header.getBytes());
		writeCRLF();
	}
	
	void finishRequest() throws IOException{
		writeCRLF();
		os.write(this.getBytes());
		os.flush();
		pos = 0;
	}
	
	void writeBody(byte[] bs){
		throw new RuntimeException("unsuppered");
	}
	
	void flush() throws IOException{
		os.flush();
	}
	
	private void writeSP(){
		writeBytes(" ".getBytes());
	}
	
	private void writeCRLF(){
		writeBytes("\r\n".getBytes());
	}

	private void resize(int len)
	{
		byte new_arr[] = new byte[len];
		System.arraycopy(arr, 0, new_arr, 0, arr.length);
		arr = new_arr;
	}


	private byte[] getBytes()
	{
		byte[] dst = new byte[pos];
		System.arraycopy(arr, 0, dst, 0, pos);
		return dst;
	}
	

	void writeBytes(byte[] buff)
	{
		writeBytes(buff, 0, buff.length);
	}
	
	void writeBytes(byte[] buff, int off, int len)
	{
		if ((pos + len) > arr.length)
			resize(arr.length + len + 32);
		System.arraycopy(buff, off, arr, pos, len);
		pos += len;
	}
}
