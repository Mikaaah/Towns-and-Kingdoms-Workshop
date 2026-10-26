package nl.townsandkingdoms.core.projector;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.*;

/** Public-address-only, size/time bounded HTTP image transfer. No game or client classes. */
public final class PublicImageDownload {
    private static final int LIMIT=8*1024*1024;
    public static byte[] download(String url)throws Exception {
        if(url.length()>ProjectorSettings.MAX_URL)throw new IOException("Image URL is too long.");
        return download(URI.create(url),0);
    }
    /** Pin each connection to a validated public IP, including every redirect. */
    private static byte[] download(URI uri,int redirects)throws Exception{
        if(redirects>3||!ProjectorSettings.validUrl(uri.toASCIIString()))throw new IOException("Invalid image URL or too many redirects.");
        InetAddress[] resolved=InetAddress.getAllByName(uri.getHost());
        if(resolved.length==0)throw new IOException("Image host not found.");
        for(InetAddress address:resolved)if(!isPublic(address))throw new IOException("Image links must use a public internet host.");
        boolean tls="https".equalsIgnoreCase(uri.getScheme());int port=uri.getPort()==-1?(tls?443:80):uri.getPort();
        Socket raw=new Socket();raw.connect(new InetSocketAddress(resolved[0],port),5000);raw.setSoTimeout(7000);
        try(Socket socket=tls?tlsSocket(raw,uri.getHost(),port):raw){
            String path=uri.getRawPath();if(path==null||path.isEmpty())path="/";if(uri.getRawQuery()!=null)path+="?"+uri.getRawQuery();
            String host=uri.getHost().contains(":")?"["+uri.getHost()+"]":uri.getHost();
            String request="GET "+path+" HTTP/1.1\r\nHost: "+host+(uri.getPort()==-1?"":":"+port)+"\r\nUser-Agent: TNK-Workshop/0.2\r\nAccept: image/png,image/jpeg,image/gif\r\nAccept-Encoding: identity\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            InputStream input=new BufferedInputStream(new DeadlineInputStream(socket));
            int remainingHeaders=16384;String first=line(input,remainingHeaders);remainingHeaders-=first.length()+2;
            String[] status=first.split(" ",3);if(status.length<2)throw new IOException("Invalid image response.");
            int code=Integer.parseInt(status[1]);var headers=new HashMap<String,String>();
            while(true){String l=line(input,remainingHeaders);remainingHeaders-=l.length()+2;if(l.isEmpty())break;int colon=l.indexOf(':');if(colon>0)headers.put(l.substring(0,colon).toLowerCase(Locale.ROOT),l.substring(colon+1).trim());}
            if(code>=300&&code<400){String target=headers.get("location");if(target==null)throw new IOException("Image redirect has no destination.");return download(uri.resolve(target),redirects+1);}
            if(code!=200)throw new IOException("Image host returned HTTP "+code+".");
            if(!headers.getOrDefault("content-encoding","identity").equalsIgnoreCase("identity"))throw new IOException("Unsupported compressed image response.");
            if(Long.parseLong(headers.getOrDefault("content-length","0"))>LIMIT)throw new IOException("Image exceeds 8 MB.");
            var out=new ByteArrayOutputStream();
            if(headers.getOrDefault("transfer-encoding","").equalsIgnoreCase("chunked")){
                while(true){String size=line(input,128).split(";",2)[0].trim();long n=Long.parseLong(size,16);if(n==0)break;if(n<0||n>LIMIT-out.size())throw new IOException("Image exceeds 8 MB.");copy(input,out,(int)n);if(!line(input,2).isEmpty())throw new IOException("Invalid image transfer.");}
            }else if(headers.containsKey("content-length")){
                long n=Long.parseLong(headers.get("content-length"));if(n<0||n>LIMIT)throw new IOException("Invalid image size.");copy(input,out,(int)n);
            }else{
                byte[] buffer=new byte[8192];for(int n;(n=input.read(buffer))!=-1;){if(out.size()+n>LIMIT)throw new IOException("Image exceeds 8 MB.");out.write(buffer,0,n);}
            }
            return out.toByteArray();
        }finally{raw.close();}
    }
    private static Socket tlsSocket(Socket raw,String host,int port)throws IOException{
        SSLSocket socket=(SSLSocket)((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(raw,host,port,true);
        var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");
        if(!host.contains(":")&&!host.matches("[0-9.]+"))parameters.setServerNames(List.of(new SNIHostName(host)));
        socket.setSSLParameters(parameters);socket.startHandshake();return socket;
    }
    public static boolean isPublic(InetAddress address){
        if(address.isAnyLocalAddress()||address.isLoopbackAddress()||address.isLinkLocalAddress()||address.isSiteLocalAddress()||address.isMulticastAddress())return false;
        byte[] b=address.getAddress();int a=b[0]&255,c=b[1]&255;
        if(b.length==4)return a!=0&&a!=127&&a<224&&!(a==100&&c>=64&&c<=127)&&!(a==169&&c==254)&&!(a==192&&(c==0||c==168))&&!(a==198&&(c==18||c==19));
        return (a&0xE0)==0x20; // IPv6 global unicast only; excludes ULA, mapped and local ranges.
    }
    private static final class DeadlineInputStream extends FilterInputStream {
        private final Socket socket; private final long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        DeadlineInputStream(Socket socket)throws IOException{super(socket.getInputStream());this.socket=socket;}
        private void remaining()throws IOException{long ms=TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime());if(ms<=0)throw new SocketTimeoutException("Image download timed out.");socket.setSoTimeout((int)Math.min(7000,Math.max(1,ms)));}
        @Override public int read()throws IOException{remaining();return in.read();}
        @Override public int read(byte[] data,int offset,int length)throws IOException{remaining();return in.read(data,offset,length);}
    }
    private static String line(InputStream input,int max)throws IOException{
        if(max<2)throw new IOException("Image response headers too large.");var out=new ByteArrayOutputStream();
        while(out.size()<max){int c=input.read();if(c==-1)throw new EOFException("Incomplete image response.");if(c=='\n')return out.toString(StandardCharsets.US_ASCII).replace("\r","");out.write(c);}throw new IOException("Image response line too long.");
    }
    private static void copy(InputStream input,OutputStream output,int size)throws IOException{
        byte[] buffer=new byte[8192];while(size>0){int n=input.read(buffer,0,Math.min(size,buffer.length));if(n<0)throw new EOFException("Incomplete image.");output.write(buffer,0,n);size-=n;}
    }
}
