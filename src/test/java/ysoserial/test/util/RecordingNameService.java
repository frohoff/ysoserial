package ysoserial.test.util;

import sun.net.spi.nameservice.NameService;
import ysoserial.payloads.util.Reflections;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RecordingNameService implements NameService {

    private final List<String> lookups = new LinkedList<String>();

    public List<String> getLookups() {
        return Collections.unmodifiableList(lookups);
    }

    @Override
    public InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException {
        lookups.add(host);
        throw new UnknownHostException();
    }

    @Override
    public String getHostByAddr(byte[] addr) throws UnknownHostException {
        throw new UnknownHostException();
    }

    public void install() throws Exception {
        getNameServices().add(this);
    }

    public void uninstall() throws Exception {
        getNameServices().remove(this);
    }

    private static List<NameService> getNameServices() throws Exception {
        return (List<NameService>) Reflections.getFieldValue(InetAddress.class, "nameServices");
    }
}
