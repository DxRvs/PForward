Info:
=============

**PForward** is util for make port forwarding via proxy chains

Build:
=============
<pre>
git clone https://{project repo}
cd ./pforward/PForward
gradle build
</pre>
or with docker
<pre>
docker run --rm -u gradle -v "$PWD":/home/gradle/project -w /home/gradle/project gradle gradle build --warning-mode all
</pre>
Run:
=============
<pre>
java -jar ./build/libs/PForward.jar  
PForward Version:0.14
Arguments:
	-i - listen ip address
	-p - listen tcp port list
	-r - listen tcp port range
	-t - target of connections
	     if target port is not defined then used listen port value 
	-c - proxy chains definition
	     defenition format:
	          ProxyType:FirstProxyHostname:ProxyPotr_ProxyType:NextProxyHostname:ProxyPotr
	          where ProxyType s4 for socks4 proxy and s5 for socks5 proxy
Forward tcp port via proxy chains:
	Usage: [-i 8.8.8.8] [-p 1080] [-c s5:1.1.1.1:1080_s4:2.2.2.2:1080] -t target_ipv4:80
	Usage: [-i 8.8.8.8] -p 135[,445,..] [-c s5:1.1.1.1:1080_s4:2.2.2.2:1080] -t target_ipv4
	Usage: [-i 8.8.8.8] -r 1000-2000 [-c s4:1.1.1.1:1080_s4:2.2.2.2:1080] -t target_ipv4
Start socks5 local server:
	Usage: -server [8.8.8.8] [-p 1080]
</pre>
