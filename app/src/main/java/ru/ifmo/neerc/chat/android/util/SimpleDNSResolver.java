package ru.ifmo.neerc.chat.android.util;

import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration.DnssecMode;
import org.jivesoftware.smack.initializer.SmackInitializer;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.util.dns.SRVRecord;

public class SimpleDNSResolver extends DNSResolver implements SmackInitializer {

    private static SimpleDNSResolver INSTANCE = new SimpleDNSResolver();

    public static DNSResolver getInstance() {
        return INSTANCE;
    }

    public SimpleDNSResolver() {
        super(false);
    }

    @Override
    protected List<SRVRecord> lookupSRVRecords0(String name, List<HostAddress> failedAddresses, DnssecMode dnssecMode) {
        throw new UnsupportedOperationException();
    }

    public static void setup() {
        DNSUtil.setDNSResolver(getInstance());
    }

    @Override
    public List<Exception> initialize() {
        setup();
        return null;
    }
}
