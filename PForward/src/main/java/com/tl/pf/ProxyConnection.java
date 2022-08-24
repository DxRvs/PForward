package com.tl.pf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

import com.tl.lib.SocksSocket;

public class ProxyConnection extends Thread {

	private ProxyOption[] chains;
	private InputStream chainIStream = null;
	private OutputStream chainOStream = null;
	private String targetIP = null;
	private int targetPort = -1;
	protected Socket clientSocket;
	protected InputStream clietnIStream = null;
	protected OutputStream clientOStream = null;

	private LinkedList<SocksSocket> sslist = new LinkedList<>();

	public ProxyConnection(ProxyOption[] op_chains, String target_host, int target_port, Socket client_socket) {
		chains = op_chains;
		targetIP = target_host;
		targetPort = target_port;
		clientSocket = client_socket;
		try {
			clientSocket.setTcpNoDelay(true);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void BuildChain() throws IOException {
		SocksSocket ss = null;

		for (int i = 0; i < chains.length; i++) {

			boolean isv4 = false;
			if (chains[i].getProxyType() == ProxyOption.type_Socks4)
				isv4 = true;
			ss = new SocksSocket(chains[i].getProxyHost(), chains[i].getProxyPort(),
					chains[i].getPasswordAuthentication(), isv4);

			sslist.add(ss);

			if (i == chains.length - 1) {
				ss.connect(new InetSocketAddress(targetIP, targetPort), 0, chainIStream, chainOStream);
				chainIStream = ss.getInputStream();
				chainOStream = ss.getOutputStream();
				System.out.print("--> " + targetIP + ":" + String.valueOf(targetPort) + " OK ");
			} else {
				InetSocketAddress ia = new InetSocketAddress(chains[i + 1].getProxyHost(),
						chains[i + 1].getProxyPort());
				ss.connect(ia, 0, chainIStream, chainOStream);
				chainIStream = ss.getInputStream();
				chainOStream = ss.getOutputStream();
				System.out.print("--> " + chains[i + 1].getProxyHost() + ":"
						+ String.valueOf(chains[i + 1].getProxyPort()) + " OK ");
			}

		}

	}

	protected void initStream() throws IOException {

		clietnIStream = clientSocket.getInputStream();
		clientOStream = clientSocket.getOutputStream();
	}

	@Override
	public void run() {

		try {
			Connect();
			initStream();
			System.out.println(" SUCCESS");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				clientSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return;
		}

		Thread ClientReader = new Thread(new Runnable() {
			@Override
			public void run() {

				byte[] buf = new byte[4069];
				int total = 0;
				int count = 0;
				try {

					count = clietnIStream.read(buf);

					while (count > 0) {
						total += count;
						System.out.println("client: " + clientSocket.getInetAddress().getHostAddress() + ":"
								+ clientSocket.getPort() + " >>>> " + String.valueOf(count) + " BYTES >>>> " + targetIP
								+ ":" + targetPort + " Total [" + total + "]"); //
						chainOStream.write(buf, 0, count);
						chainOStream.flush();
						count = clietnIStream.read(buf);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
					Close();
					return;
				}

			}
		});
		ClientReader.start();

		byte[] buf = new byte[4069];
		int total = 0;
		int count = 0;
		try {
			count = chainIStream.read(buf);
			while (count > 0) {
				total += count;
				System.out.println("client: " + clientSocket.getInetAddress().getHostAddress() + ":"
						+ clientSocket.getPort() + " <<<< " + String.valueOf(count) + " BYTES <<<< " + targetIP + ":"
						+ targetPort + " Total [" + total + "]"); //
				clientOStream.write(buf, 0, count);
				clientOStream.flush();
				count = chainIStream.read(buf);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			Close();
			return;
		}

	}

	private void Close() {
		if (clientSocket != null) {
			try {
				clientSocket.close();
			} catch (IOException e) {
			}
		}

		try {
			chainIStream.close();
			chainOStream.close();
		} catch (Exception e) {

		}
		for (SocksSocket s : sslist) {
			try {
				s.close();
			} catch (IOException e) {
			}
		}
	}

	private void Connect() throws IOException {
		if (chains != null)
			if (chains.length > 0) {
				BuildChain();
				return;
			}

		Socket ts = new Socket();
		ts.connect(new InetSocketAddress(targetIP, targetPort));
		chainIStream = ts.getInputStream();
		chainOStream = ts.getOutputStream();
		System.out.println("--> " + targetIP + ":" + String.valueOf(targetPort) + " OK ");
		return;
	}

}
