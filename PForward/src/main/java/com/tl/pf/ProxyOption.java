package com.tl.pf;

import java.net.PasswordAuthentication;

public class ProxyOption {
	public static final int type_Socks4 = 1;
	public static final int type_Socks5 = 2;
	public static final int type_HTTP = 3;
	public static final int type_XOR = 77;

	private String proxyHost = "";
	private int proxyPort = -1;

	private int proxyType = -1;
	private PasswordAuthentication passwordAuthentication;

	public void setProxyType(int proxyType) {
		this.proxyType = proxyType;
	}

	public int getProxyType() {
		return proxyType;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public PasswordAuthentication getPasswordAuthentication() {
		return passwordAuthentication;
	}

	public ProxyOption(int type, String host, int port, PasswordAuthentication pa) {
		proxyType = type;
		proxyHost = host;
		proxyPort = port;
		passwordAuthentication = pa;

		if (passwordAuthentication == null) {
			passwordAuthentication = new PasswordAuthentication("", new char[] {});
		}

	}
}
