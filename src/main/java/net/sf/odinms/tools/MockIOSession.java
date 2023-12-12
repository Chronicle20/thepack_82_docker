package net.sf.odinms.tools;

import java.net.SocketAddress;

import net.sf.odinms.net.MaplePacket;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSessionConfig;


public class MockIOSession extends AbstractIoSession {

    public MockIOSession() {
        super(null);
    }

    @Override
    public IoSessionConfig getConfig() {
        return null;
    }

    @Override
    public IoFilterChain getFilterChain() {
        return null;
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return null;
    }

    @Override
    public IoProcessor getProcessor() {
        return null;
    }

    @Override
    public IoHandler getHandler() {
        return null;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public IoService getService() {
        return null;
    }

    @Override
    public SocketAddress getServiceAddress() {
        return null;
    }

    @Override
    public WriteFuture write(Object message, SocketAddress remoteAddress) {
        return null;
    }

    @Override
    public WriteFuture write(Object message) {
        if (message instanceof MaplePacket) {
            MaplePacket mp = (MaplePacket) message;
            if (mp.getOnSend() != null) {
                mp.getOnSend().run();
            }
        }
        return null;
    }
}
