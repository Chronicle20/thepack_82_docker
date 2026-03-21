package net.sf.odinms.net.world;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class NoVerifySslRMIClientSocketFactory implements RMIClientSocketFactory, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
            SSLParameters params = socket.getSSLParameters();
            params.setEndpointIdentificationAlgorithm(null);
            socket.setSSLParameters(params);
            socket.startHandshake();
            return socket;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("SSL socket creation failed", e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NoVerifySslRMIClientSocketFactory;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
