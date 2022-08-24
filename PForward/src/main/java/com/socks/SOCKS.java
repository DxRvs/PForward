package com.socks;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import com.socks.server.IdentAuthenticator;

public class SOCKS {
	private static SOCKS main;
	private ProxyServer pserver= null;
	
	private SOCKS() {
		
	}
	
	public static SOCKS getServerMng()
	{
		if (SOCKS.main==null) {
			SOCKS.main = new SOCKS();
		}
		return SOCKS.main;
	}
	
	public ProxyServer start(Properties pr){
		if (pserver==null){
			pserver = main(pr);
		}else{
			pserver.stop();
			pserver = main(pr);
		}
		return pserver;
	}
	
	public void stop(){
		if (pserver!=null){
			pserver.stop();
		}
		pserver = null;
	}
	public ProxyServer getCurrentProxyServer(){
		return pserver;
	}
	

	private  ProxyServer main(Properties pr) {
		String host = null;
		int port = 1080;
		IdentAuthenticator auth = new IdentAuthenticator();
		InetAddress localIP = null;
		if (pr == null) {
			return null;
		}
		if (!addAuth(auth, pr)) {
			return null;
		}
		String port_s = (String) pr.get("port");
		if (port_s != null)
			try {
				port = Integer.parseInt(port_s);
			} catch (NumberFormatException nfe) {
				return null;
			}
		serverInit(pr);
		host = (String) pr.get("host");
		if (host != null)
			try {
				localIP = InetAddress.getByName(host);
			} catch (UnknownHostException uhe) {
				return null;
			}
		ProxyServer server = new ProxyServer(auth);
		server.start(port, 5, localIP);
		return server;
	}


	static boolean addAuth(IdentAuthenticator ident, Properties pr) {

		InetRange irange;

		String range = (String) pr.get("range");
		if (range == null)
			return false;
		irange = parseInetRange(range);

		String users = (String) pr.get("users");

		if (users == null) {
			ident.add(irange, null);
			return true;
		}

		Hashtable uhash = new Hashtable();

		StringTokenizer st = new StringTokenizer(users, ";");
		while (st.hasMoreTokens())
			uhash.put(st.nextToken(), "");

		ident.add(irange, uhash);
		return true;
	}

	/**
	 * Does server initialisation.
	 */
	static void serverInit(Properties props) {
		int val;
		val = readInt(props, "iddleTimeout");
		if (val >= 0) {
			ProxyServer.setIddleTimeout(val);
			inform("Setting iddle timeout to " + val + " ms.");
		}
		val = readInt(props, "acceptTimeout");
		if (val >= 0) {
			ProxyServer.setAcceptTimeout(val);
			inform("Setting accept timeout to " + val + " ms.");
		}
		val = readInt(props, "udpTimeout");
		if (val >= 0) {
			ProxyServer.setUDPTimeout(val);
			inform("Setting udp timeout to " + val + " ms.");
		}

		val = readInt(props, "datagramSize");
		if (val >= 0) {
			ProxyServer.setDatagramSize(val);
			inform("Setting datagram size to " + val + " bytes.");
		}

		proxyInit(props);

	}

	/**
	 * Initialises proxy, if any specified.
	 */
	static void proxyInit(Properties props) {
		String proxy_list;
		Proxy proxy = null;
		StringTokenizer st;

		proxy_list = (String) props.get("proxy");
		if (proxy_list == null)
			return;

		st = new StringTokenizer(proxy_list, ";");
		while (st.hasMoreTokens()) {
			String proxy_entry = st.nextToken();

			Proxy p = Proxy.parseProxy(proxy_entry);

			if (p == null)
				return;
			// exit("Can't parse proxy entry:"+proxy_entry);

			inform("Adding Proxy:" + p);

			if (proxy != null)
				p.setChainProxy(proxy);

			proxy = p;

		}
		if (proxy == null)
			return; // Empty list

		String direct_hosts = (String) props.get("directHosts");
		if (direct_hosts != null) {
			InetRange ir = parseInetRange(direct_hosts);
			inform("Setting direct hosts:" + ir);
			proxy.setDirect(ir);
		}

		ProxyServer.setProxy(proxy);
	}

	/**
	 * Inits range from the string of semicolon separated ranges.
	 */
	static InetRange parseInetRange(String source) {
		InetRange irange = new InetRange();

		StringTokenizer st = new StringTokenizer(source, ";");
		while (st.hasMoreTokens())
			irange.add(st.nextToken());

		return irange;
	}

	/**
	 * Integer representaion of the property named name, or -1 if one is not
	 * found.
	 */
	static int readInt(Properties props, String name) {
		int result = -1;
		String val = (String) props.get(name);
		if (val == null)
			return -1;
		StringTokenizer st = new StringTokenizer(val);
		if (!st.hasMoreElements())
			return -1;
		try {
			result = Integer.parseInt(st.nextToken());
		} catch (NumberFormatException nfe) {
			inform("Bad value for " + name + ":" + val);
		}
		return result;
	}

	// Display functions
	// /////////////////

	static void inform(String s) {

	}

}
