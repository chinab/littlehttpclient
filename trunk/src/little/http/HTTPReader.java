package little.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A <code>HTTPReader</code> is used to decode http response from an TCP/IP
 * connection to a HTTP server.
 *  
 * @author feijian_zhuo
 * @version 2010-06-05
 * @email allthetime10000@gmail.com
 */


public class HTTPReader {
	
	HTTPConnection hc;
	String Transfer_Encoding = null;
	String Content_Length = null;
	String Connection = null;
	boolean isResponseLineReaded = false;
	boolean isHeadersReaded = false;
	boolean isBodyReaded = false;
	InputStream in;
	InputStream bodyInputStream;
	// when chencked
	List<String> trainer = new ArrayList<String>();
	byte[] buff;
	int start;
	int end;

	private void resetx() {
		Transfer_Encoding = null;
		Content_Length = null;
		Connection = null;
		isResponseLineReaded = false;
		isHeadersReaded = false;
		isBodyReaded = false;
		bodyInputStream = null;
	}

	HTTPReader(HTTPConnection hc, InputStream in) {
		this.hc = hc;
		buff = new byte[512];
		this.in = in;
		start = end = 0;
	}

	private int buffRemain() {
		return end - start;
	}

	private void buffConsume(int len) {
		if (len > end - start)
			throw new RuntimeException("len > end - index");
		start += len;
	}

	private int buffFillMore() throws IOException {
		if (end > buff.length && end < 0)
			throw new RuntimeException("end > arr.length && end < 0");

		if (start == end)
			start = end = 0;

		if (start > (buff.length / 2)) {// move to head to free space
			for (int i = start; i < end; i++) {
				buff[i - start] = buff[i];
			}
			start = 0;
			end = end - start;
		}

		if (end == buff.length) {
			byte[] new_arr = new byte[buff.length + 256];
			System.arraycopy(buff, 0, new_arr, 0, buff.length);
			buff = new_arr;
		}

		int read = in.read(buff, end, buff.length - end);
		if (read == 0)
			throw new RuntimeException("buffFillMore read = " + read);
		
		end = end + read;
		
		if (read == -1)
			hc.close();
		
		return read;
	}

	private String readCRLRLne() throws IOException {
		
		boolean CR_present = false;
		int byte_count = 0;	
		for (;;) {
		
			int len = buffRemain();
			if (byte_count == len) {
				int read = buffFillMore();
				if (read == -1)
					throw new RuntimeException("fillMore() == -1");
			}
			len = buffRemain();

			for (; byte_count < len; byte_count++) {
				if (buff[start + byte_count] == '\r') {
					CR_present = true;
					continue;
				}
				if (CR_present && buff[start + byte_count] == '\n') {
					String ret = new String(buff, start, byte_count - 1);
					buffConsume(byte_count + 1);
					return ret;
				}
				CR_present = false;
			}
		}
	}

	String readResponseLine() throws IOException {
		if (isResponseLineReaded)
			throw new RuntimeException("isResponseLineReaded true");
		isResponseLineReaded = true;
		return readCRLRLne();
	}

	String readHeader() throws IOException {
		if (!isResponseLineReaded)
			throw new RuntimeException("isRequestLineReaded false");
		if (isHeadersReaded)
			throw new RuntimeException("isHeadersReaded true");
		
		String header = readCRLRLne();
		if (header.length() == 0)
			isHeadersReaded = true;
		else if (header.startsWith("Transfer-Encoding")) {
			Transfer_Encoding = header.substring(header.indexOf(":") + 1).trim();
			Content_Length = null;
		} else if (Transfer_Encoding == null && header.startsWith("Content-Length"))
			Content_Length = header.substring(header.indexOf(":") + 1).trim();
		else if (Connection == null && header.startsWith("Connection")) {
			Connection = header.substring(header.indexOf(":") + 1).trim();
		}
		return header;
	}

	InputStream getHTTPBodyInputStream() throws IOException {
		if (!isResponseLineReaded)
			throw new RuntimeException("isRequestLineReaded false");
		if (!isHeadersReaded)
			throw new RuntimeException("isHeadersReaded false");
		if (isBodyReaded)
			throw new RuntimeException("isBodyReaded true");

		if (bodyInputStream == null) {
			bodyInputStream = new InputStream() {
				int left;
				boolean EOF = false;
				boolean first = true;

				@Override
				public int read() throws IOException {
					if (EOF)
						return -1;
					if (Transfer_Encoding != null
							&& Transfer_Encoding.equals("chunked")) {
						if (left == 0) {
							if (!first) {
								String nullStr = readCRLRLne();
								if (nullStr.length() > 0)
									throw new RuntimeException("nullStr.length() > 0");

							} else
								first = false;

							String lengthStr = readCRLRLne();
							left = lengthStr.indexOf(';') == -1 ? Integer
									.parseInt(lengthStr, 16) : Integer
									.parseInt(lengthStr.substring(0, lengthStr
											.indexOf(';')), 16);
							if (left == 0) {// the last chunked
								EOF = true;
								// read trainer
								trainer.clear();
								String trainer_header = readCRLRLne();
								for (; trainer_header.length() > 0;){
									trainer.add(trainer_header);
									trainer_header = readCRLRLne();
								}
								
								resetx();

								return -1;
							}
						}
					} else if (Content_Length != null) {
						if (first) {
							left = Integer.parseInt(Content_Length);
							first = false;
						}

						
						if (left == 0) {
							EOF = true;
							resetx();
							return -1;
						}

					} else if (Connection != null && Connection.equals("close")) {
						//just nothing to do now
					} else {
						//i don't know what this status is,just tell me when you know
						throw new RuntimeException("un supperted!");
					}

					if (buffRemain() == 0) {
						int read = buffFillMore();
						if (read == -1) {
							EOF = true;
							resetx();
							return -1;
						}
					}
					int ret = buff[start] & 0xff;
					buffConsume(1);
					--left;
					return ret;
				}

				public void close() throws IOException {
					for (; this.read() != -1;);
				}
			};
		}
		
		return bodyInputStream;
	}
}
