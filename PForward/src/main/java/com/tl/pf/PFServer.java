package com.tl.pf;

import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.socks.SOCKS;

public class PFServer {

	public static String[] Args;
	public static final String Args_lport = "-p";
	public static final String Args_lip = "-i";
	public static final String Args_dump = "-d";
	public static final String Args_chain = "-c";
	public static final String Args_target = "-t";
	public static final String Args_range = "-r";
	public static final String Args_server = "-server";
	public static final String Args_udp = "-udp";
	public static final String Version = "0.14";

	public static void runServer(String[] args) {
		String serverIP = getArgs(Args_server);
		if (serverIP == null)
			serverIP = "0.0.0.0";
		String serverPort = getArgs(Args_lport);
		if (serverPort == null)
			serverPort = "1080";

		Properties pr = new Properties();
		pr.setProperty("port", serverPort);
		pr.setProperty("iddleTimeout", "600000");
		pr.setProperty("acceptTimeout", "60000");
		pr.setProperty("udpTimeout", "600000");
		pr.setProperty("range", ".");

		pr.setProperty("host", serverIP);
		SOCKS server = SOCKS.getServerMng();
		server.start(pr);
		System.out.println("Server was runned: " + serverIP + ":" + serverPort);
		System.out.println("For exit enter q");
		Scanner console = new Scanner(System.in);
		while (console.nextLine().toUpperCase().hashCode() != "Q".hashCode()) {

		}
		console.close();
		server.stop();
	}

	public static void main(String[] args) {
		System.out.println("PForward Version:" + Version);
		if (args.length==0) {
			Usage(null);
			return;
		}
		Args = args;
		int lport_def = 1080;
		HashSet<Integer> lports = new HashSet<Integer>();
		int backlog = 10000;

		if (isArgs(Args_server)) {
			runServer(args);
		} else {

			String port_arg = getArgs(Args_lport);
			if (port_arg != null) {
				try {
					if (port_arg.contains(",")) {
						String[] ports = port_arg.split(",");
						for (String p : ports) {
							int arg_port = Integer.valueOf(p);
							if ((arg_port < 1) || (arg_port > 65535)) {
								throw new Exception();
							}
							lports.add(arg_port);
						}
					} else {
						int arg_port = Integer.valueOf(port_arg);
						if ((arg_port < 1) || (arg_port > 65535)) {
							throw new Exception();
						}
						lports.add(arg_port);
					}
				} catch (Exception e) {
					Usage(Args_lport);
					return;
				}
			} else {
				lports.add(lport_def);
			}
			String lip_arg = getArgs(Args_lip);
			String ia = null;
			if (lip_arg != null) {
				ia = lip_arg;
			} else {
				Usage(Args_lip);
				return;
			}

			String prange = getArgs(Args_range);
			String target = getArgs(Args_target);
			String t_host = null;
			int t_port = -1;
			if (prange == null) {

				if (target != null) {
					String[] tr = target.split(":");
					if (tr.length == 2) {
						t_host = tr[0];
						t_port = Integer.valueOf(tr[1]);
						if (!isIP(t_host)) {
							Usage(Args_target);
							return;
						}
					} else {
						t_host = getArgs(Args_target);
						if (lports.size() > 0) {
							t_port = lports.iterator().next();
						}
					}
				}
				if (t_host == null) {
					Usage(t_host);
					return;
				}
				if (t_port < 1 || t_port > 65535) {
					Usage(t_host);
					return;
				}
			} else {
				t_host = getArgs(Args_target);
			}

			ProxyOption[] op_chains = null;
			String chains = getArgs(Args_chain);
			if (chains != null) {
				String[] proxy_serv = chains.split("_");
				op_chains = new ProxyOption[proxy_serv.length];
				for (int i = 0; i < proxy_serv.length; i++) {
					String[] opt = proxy_serv[i].split(":");
					if (opt.length == 3) {
						ProxyOption p_opt = null;
						if (opt[0].equals("s5")) {
							p_opt = new ProxyOption(ProxyOption.type_Socks5, opt[1], Integer.valueOf(opt[2]), null);
						}
						if (opt[0].equals("s4")) {
							p_opt = new ProxyOption(ProxyOption.type_Socks4, opt[1], Integer.valueOf(opt[2]), null);
						}
						if (p_opt != null) {
							op_chains[i] = p_opt;
						} else {
							Usage(Args_chain);
							return;
						}

					} else {
						Usage(Args_chain);
						return;
					}
				}

			} else {
				chains = "direct";
			}
			System.out.println("Proxy chains: " + chains);
			System.out.println("Target: " + target);

			int listenType = Server.listenTCP;
			if (isArgs(Args_udp)) {
				listenType = Server.listenUDP;
			}

			// ------------
			if (prange == null) {
				if (lports.size() == 1) {
					Server s = new Server(ia, lports.iterator().next(), backlog, op_chains, t_host, t_port, listenType);
					s.start();
				} else {
					for (int pr : lports) {
						Server s = new Server(ia, pr, backlog, op_chains, t_host, pr, listenType);
						s.start();
					}

				}
			} else {
				String[] rr = prange.split("-");
				if (rr.length != 2) {
					Usage(Args_target);
					return;
				}
				int start = Integer.valueOf(rr[0]);
				int end = Integer.valueOf(rr[1]);
				if (start < end)
					if (start > 0)
						if (end < 65536) {

							for (int p = start; p < end; p++) {
								Server s = new Server(ia, p, backlog, op_chains, t_host, p, listenType);
								s.start();
							}
						}
			}
		}
	}

	public static final String getArgs(String key) {
		for (int i = 0; i < Args.length; i++) {
			if (Args[i].equals(key)) {
				if (Args.length > (i + 1)) {
					return Args[i + 1];
				}
			}
		}
		return null;
	}

	public static final boolean isArgs(String key) {
		for (int i = 0; i < Args.length; i++) {
			if (Args[i].equals(key)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isIP(String ip) {
		Pattern p = Pattern.compile("[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}");
		String finish = null;
		Matcher m = p.matcher(ip);
		while (m.find()) {
			finish = m.group();
		}
		if (finish != null) {
			if (ip.equals(finish))
				return true;
		}
		return false;
	}

	public static final void Usage(String wrong) {
		if (wrong != null) {
			System.out.println("Wrong argument: " + wrong);
		}
		System.out.println("Arguments:");
		System.out.println("	-i - listen ip address");
		System.out.println("	-p - listen tcp port list");
		System.out.println("	-r - listen tcp port range");
		System.out.println("	-t - target of connections");
		System.out.println("	     if target port is not defined then used listen port value ");
		System.out.println("	-c - proxy chains definition");
		System.out.println("	     defenition format:");
		System.out
				.println("	          ProxyType:FirstProxyHostname:ProxyPotr_ProxyType:NextProxyHostname:ProxyPotr");
		System.out.println("	          where ProxyType s4 for socks4 proxy and s5 for socks5 proxy");
		System.out.println("Forward tcp port via proxy chains:");
		System.out.println("	Usage: [-i 8.8.8.8] [-p 1080] [-c s5:1.1.1.1:1080_s4:2.2.2.2:1080] -t target_ipv4:80");
		System.out
				.println("	Usage: [-i 8.8.8.8] -p 135[,445,..] [-c s5:1.1.1.1:1080_s4:2.2.2.2:1080] -t target_ipv4");
		System.out.println("	Usage: [-i 8.8.8.8] -r 1000-2000 [-c s4:1.1.1.1:1080_s4:2.2.2.2:1080] -t target_ipv4");
		System.out.println("Start socks5 local server:");
		System.out.println("	Usage: -server [8.8.8.8] [-p 1080]");
	}

	public static final void PLog(String mess) {
		System.out.println(mess);
	}

}
