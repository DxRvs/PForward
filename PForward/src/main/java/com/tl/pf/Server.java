package com.tl.pf;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {

	private InetAddress ia;
	private int listenPort = 1080;
	private int backlog = 10000;
	private ProxyOption[] op_chains;
	private String targetName;
	private int targetPort;
	private int listenType;
	public static final int listenTCP = 0;
	public static final int listenUDP = 1;

	public Server(String iaddr, int listenport, int bl, ProxyOption[] chains, String target, int target_port,
			int listentype) {
		InetSocketAddress isa = new InetSocketAddress(iaddr, listenport);
		ia = isa.getAddress();
		listenPort = listenport;
		backlog = bl;
		op_chains = chains;
		targetName = target;
		targetPort = target_port;
		listenType = listentype;
	}

	@Override
	public void run() {
		ServerSocket serv = null;
		if (ia != null) {
			try {
				serv = new ServerSocket(listenPort, backlog, ia);
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		} else {
			try {
				serv = new ServerSocket(listenPort, backlog);
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}

		if (serv != null) {
			System.out.println(
					"server is listening: " + serv.getInetAddress().getHostAddress() + ":" + serv.getLocalPort());
			while (true) {
				try {
					Socket s = serv.accept();
					PLog("client: " + s.getInetAddress().getHostAddress() + ":" + s.getPort() + " -> " + targetName
							+ ":" + targetPort);
					ProxyConnection pc = null;

					pc = new ProxyConnection(op_chains, targetName, targetPort, s);

					pc.start();
					// RUN

				} catch (IOException e) {
					PLog(e.getMessage());
					return;
				}

			}
		}
	}

	public static final void PLog(String mess) {
		System.out.println(mess);
	}

}
