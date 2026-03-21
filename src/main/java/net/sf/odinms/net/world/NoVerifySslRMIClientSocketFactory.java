package net.sf.odinms.net.world;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.rmi.ssl.SslRMIClientSocketFactory;

public class NoVerifySslRMIClientSocketFactory extends SslRMIClientSocketFactory implements Serializable {
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = super.createSocket(host, port);
        if (socket instanceof SSLSocket sslSocket) {
            SSLParameters params = sslSocket.getSSLParameters();
            params.setEndpointIdentificationAlgorithm(null);
            sslSocket.setSSLParameters(params);
        }
        return socket;
    }
}
