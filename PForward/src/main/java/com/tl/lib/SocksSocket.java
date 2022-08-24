package com.tl.lib;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SocksSocket implements SocksConsts {

	private String server = null;
	private int port = DEFAULT_PORT;

	protected InetSocketAddress external_address;

	private boolean useV4 = false;
	private Socket cmdsock = null;

	protected InputStream cmdIn = null;
	protected OutputStream cmdOut = null;

	private PasswordAuthentication auth;

	public SocksSocket(String s_server, int s_port, PasswordAuthentication pw, boolean is_v4) {
		useV4 = is_v4;
		server = s_server;
		port = s_port;
		auth = pw;
	}

	public void setV4() {
		useV4 = true;
	}
	
	public void close() throws IOException{
		if(cmdsock!=null)
		{cmdsock.close();}
		cmdsock = null;
		cmdIn.close();
		cmdOut.close();
	}
	
	

	public void connect(SocketAddress endpoint, int timeout, InputStream ii, OutputStream oo) throws IOException {
		InetSocketAddress epoint = (InetSocketAddress) endpoint;

		createCommandConnection(ii, oo);

		BufferedOutputStream out = new BufferedOutputStream(cmdOut, 512);
		InputStream in = cmdIn;

		if (useV4) {
			// SOCKS Protocol version 4 doesn't know how to deal with
			// DOMAIN type of addresses (unresolved addresses here)
			if (epoint.isUnresolved())
				throw new UnknownHostException(epoint.toString());
			connectV4(in, out, epoint, auth.getUserName());
			return;
		}

		// This is SOCKS V5
		out.write(PROTO_VERS);
		//out.write(2);
		out.write(1);
		out.write(NO_AUTH);
		//out.write(USER_PASSW);
		out.flush();
		byte[] data = new byte[2];
		int i = readSocksReply(in, data);
		/*if (i != 2 || ((int) data[0]) != PROTO_VERS) {
			// Maybe it's not a V5 sever after all
			// Let's try V4 before we give up
			// SOCKS Protocol version 4 doesn't know how to deal with
			// DOMAIN type of addresses (unresolved addresses here)
			if (epoint.isUnresolved())
				throw new UnknownHostException(epoint.toString());
			connectV4(in, out, epoint, auth.getUserName());
			return;
		}*/
		if (((int) data[1]) == NO_METHODS)
			throw new SocketException("SOCKS : No acceptable methods");
		if (!authenticate(data[1], in, out, auth)) {
			throw new SocketException("SOCKS : authentication failed");
		}
		out.write(PROTO_VERS);
		out.write(CONNECT);
		out.write(0);
		/* Test for IPV4/IPV6/Unresolved */
		if (epoint.isUnresolved()) {
			out.write(DOMAIN_NAME);
			out.write(epoint.getHostName().length());
			try {
				out.write(epoint.getHostName().getBytes("ISO-8859-1"));
			} catch (java.io.UnsupportedEncodingException uee) {
				assert false;
			}
			out.write((epoint.getPort() >> 8) & 0xff);
			out.write((epoint.getPort() >> 0) & 0xff);
		} else if (epoint.getAddress() instanceof Inet6Address) {
			out.write(IPV6);
			out.write(epoint.getAddress().getAddress());
			out.write((epoint.getPort() >> 8) & 0xff);
			out.write((epoint.getPort() >> 0) & 0xff);
		} else {
			out.write(IPV4);
			out.write(epoint.getAddress().getAddress());
			out.write((epoint.getPort() >> 8) & 0xff);
			out.write((epoint.getPort() >> 0) & 0xff);
		}
		out.flush();
		data = new byte[4];
		i = readSocksReply(in, data);
		if (i != 4)
			throw new SocketException("Reply from SOCKS server has bad length");
		SocketException ex = null;
		int nport, len;
		byte[] addr;
		switch (data[1]) {
		case REQUEST_OK:
			// success!
			switch (data[3]) {
			case IPV4:
				addr = new byte[4];
				i = readSocksReply(in, addr);
				if (i != 4)
					throw new SocketException("Reply from SOCKS server badly formatted");
				data = new byte[2];
				i = readSocksReply(in, data);
				if (i != 2)
					throw new SocketException("Reply from SOCKS server badly formatted");
				nport = ((int) data[0] & 0xff) << 8;
				nport += ((int) data[1] & 0xff);
				break;
			case DOMAIN_NAME:
				len = data[1];
				byte[] host = new byte[len];
				i = readSocksReply(in, host);
				if (i != len)
					throw new SocketException("Reply from SOCKS server badly formatted");
				data = new byte[2];
				i = readSocksReply(in, data);
				if (i != 2)
					throw new SocketException("Reply from SOCKS server badly formatted");
				nport = ((int) data[0] & 0xff) << 8;
				nport += ((int) data[1] & 0xff);
				break;
			case IPV6:
				len = data[1];
				addr = new byte[len];
				i = readSocksReply(in, addr);
				if (i != len)
					throw new SocketException("Reply from SOCKS server badly formatted");
				data = new byte[2];
				i = readSocksReply(in, data);
				if (i != 2)
					throw new SocketException("Reply from SOCKS server badly formatted");
				nport = ((int) data[0] & 0xff) << 8;
				nport += ((int) data[1] & 0xff);
				break;
			default:
				ex = new SocketException("Reply from SOCKS server contains wrong code");
				break;
			}
			break;
		case GENERAL_FAILURE:
			ex = new SocketException("SOCKS server general failure");
			break;
		case NOT_ALLOWED:
			ex = new SocketException("SOCKS: Connection not allowed by ruleset");
			break;
		case NET_UNREACHABLE:
			ex = new SocketException("SOCKS: Network unreachable");
			break;
		case HOST_UNREACHABLE:
			ex = new SocketException("SOCKS: Host unreachable");
			break;
		case CONN_REFUSED:
			ex = new SocketException("SOCKS: Connection refused");
			break;
		case TTL_EXPIRED:
			ex = new SocketException("SOCKS: TTL expired");
			break;
		case CMD_NOT_SUPPORTED:
			ex = new SocketException("SOCKS: Command not supported");
			break;
		case ADDR_TYPE_NOT_SUP:
			ex = new SocketException("SOCKS: address type not supported");
			break;
		}
		if (ex != null) {
			in.close();
			out.close();
			throw ex;
		}
		external_address = epoint;
	}

	protected void createCommandConnection(InputStream i, OutputStream o) throws IOException {
		if (i != null)
			if (o != null) {
				cmdIn = i;
				cmdOut = o;
				return;
			}
		
		cmdsock = new Socket();
		InetSocketAddress ia = new InetSocketAddress(server, port);
		cmdsock.connect(ia);
		cmdIn = cmdsock.getInputStream();
		cmdOut = cmdsock.getOutputStream();
		System.out.print("--> "+server+":"+String.valueOf(port)+" OK ");
		
	}

	private boolean authenticate(byte method, InputStream in, BufferedOutputStream out, PasswordAuthentication pw)
			throws IOException {
		byte[] data = null;
		int i;
		// No Authentication required. We're done then!
		if (method == NO_AUTH)
			return true;
		/**
		 * User/Password authentication. Try, in that order : - The application
		 * provided Authenticator, if any - The user preferences
		 * java.net.socks.username & java.net.socks.password - the user.name &
		 * no password (backward compatibility behavior).
		 */
		if (method == USER_PASSW) {
			String userName = null;
			String password = null;
			final InetAddress addr = InetAddress.getByName(server);

			if (pw != null) {
				userName = pw.getUserName();
				password = new String(pw.getPassword());
			}
			if (userName == null)
				return false;
			out.write(1);
			out.write(userName.length());
			try {
				out.write(userName.getBytes("ISO-8859-1"));
			} catch (java.io.UnsupportedEncodingException uee) {
				assert false;
			}
			if (password != null) {
				out.write(password.length());
				try {
					out.write(password.getBytes("ISO-8859-1"));
				} catch (java.io.UnsupportedEncodingException uee) {
					assert false;
				}
			} else
				out.write(0);
			out.flush();
			data = new byte[2];
			i = readSocksReply(in, data);
			if (i != 2 || data[1] != 0) {
				/*
				 * RFC 1929 specifies that the connection MUST be closed if
				 * authentication fails
				 */
				out.close();
				in.close();
				return false;
			}
			/* Authentication succeeded */
			return true;
		}
		return false;
	}

	protected int readSocksReply(InputStream in, byte[] data) throws IOException {
		int len = data.length;
		int received = 0;
		for (int attempts = 0; received < len && attempts < 3; attempts++) {
			int count = in.read(data, received, len - received);
			if (count < 0)
				throw new SocketException("Malformed reply from SOCKS server");
			received += count;
		}
		return received;
	}

	protected void connectV4(InputStream in, OutputStream out, InetSocketAddress endpoint, String userName)
			throws IOException {
		if (!(endpoint.getAddress() instanceof Inet4Address)) {
			throw new SocketException("SOCKS V4 requires IPv4 only addresses");
		}
		out.write(PROTO_VERS4);
		out.write(CONNECT);
		out.write((endpoint.getPort() >> 8) & 0xff);
		out.write((endpoint.getPort() >> 0) & 0xff);
		out.write(endpoint.getAddress().getAddress());

		try {
			out.write(userName.getBytes("ISO-8859-1"));
		} catch (java.io.UnsupportedEncodingException uee) {
			assert false;
		}
		out.write(0);
		out.flush();
		byte[] data = new byte[8];
		int n = readSocksReply(in, data);
		if (n != 8)
			throw new SocketException("Reply from SOCKS server has bad length: " + n);
		if (data[0] != 0 && data[0] != 4)
			throw new SocketException("Reply from SOCKS server has bad version");
		SocketException ex = null;
		switch (data[1]) {
		case 90:
			// Success!
			external_address = endpoint;
			break;
		case 91:
			ex = new SocketException("SOCKS request rejected");
			break;
		case 92:
			ex = new SocketException("SOCKS server couldn't reach destination");
			break;
		case 93:
			ex = new SocketException("SOCKS authentication failed");
			break;
		default:
			ex = new SocketException("Reply from SOCKS server contains bad status");
			break;
		}
		if (ex != null) {
			in.close();
			out.close();
			throw ex;
		}
	}

	public InputStream getInputStream() throws IOException {
		if (external_address != null)

		{
			return cmdIn;
		} else {
			throw new IOException("socks proxy error");
		}
	}

	public OutputStream getOutputStream() throws IOException {
		if (external_address != null)

		{
			return cmdOut;
		} else {
			throw new IOException("socks proxy error");
		}
	}

}
