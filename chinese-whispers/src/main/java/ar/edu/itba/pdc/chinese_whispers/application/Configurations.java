package ar.edu.itba.pdc.chinese_whispers.application;

import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.AuthenticationProvider;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.ConfigurationsConsumer;
import ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces.MetricsProvider;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.MetricsConsumer;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ProxyConfigurationProvider;

import java.util.*;

/**
 * Created by drocheg on 29/10/2016.
 * <p>
 * Class that manages system configurations.
 * This class implements the singleton pattern.
 */
// TODO DIEGO NO BORRAR DIEGO DIEGO TODO DIEGO TODO TODO DIEGO
public class Configurations implements ProxyConfigurationProvider, ConfigurationsConsumer,
        AuthenticationProvider, MetricsProvider, MetricsConsumer {


    /**
     * States if the system is l337ing.
     */
    private boolean processL337;
    /**
     * Stores users that are being silenced.
     */
    private Set<String> silencedUsers;
    /**
     * Stores to which server is being multiplexed each user belonging to this map key set.
     */
    private Map<String, HostAndPort> multiplexedUsers;
    /**
     * Stores where the default server is listening.
     */
    private HostAndPort defaultServer;
    /**
     * Map storing user and passwords for administration protocol
     */
    private Map<String, String> authorizationMap;
    /**
     * Holds the amount of bytes transferred since system start up.
     */
    private int transferredBytes;

    /**
     * Holds the singleton.
     */
    private static Configurations configurationsInstance;


    /**
     * Private constructor to implement singleton pattern.
     */
    private Configurations() {
        silencedUsers = new HashSet<>();
        processL337 = false;
        multiplexedUsers = new HashMap<>();
        defaultServer = new HostAndPort("localhost", 5222);
        authorizationMap = new HashMap<>();
        authorizationMap.put("PROTOS", "42");
        transferredBytes = 0;
    }


    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance.
     */
    public static Configurations getInstance() {
        if (configurationsInstance == null) {
            configurationsInstance = new Configurations();
        }
        return configurationsInstance;
    }

    /**
     * Returns the l337 property (when {@code true}, system is L337ing messages).
     *
     * @return
     */
    public boolean isProcessL337() {
        return processL337;
    }


    /**
     * Returns {@code true} if the user is silenced (i.e. it can receive messages, but not send).
     *
     * @param clientJID The user's JID.
     * @return {@code true} if the user is silenced, or {@code false} otherwise.
     */
    public boolean isSilenced(String clientJID) {
        return silencedUsers.contains(clientJID);
    }

    /**
     * Returns the host in which the user is being multiplexed.
     *
     * @param clientJid The user's JID.
     * @return The host.
     */
    public String getMultiplexedServerHost(String clientJid) {
        HostAndPort hap = multiplexedUsers.get(clientJid);
        return hap == null ? defaultServer.host : hap.host;
    }

    /**
     * Returns the port in which the host (to which to user is being multiplexed) is listening.
     *
     * @param clientJid The user's JID
     * @return The port.
     */
    public Integer getMultiplexedServerPort(String clientJid) {
        HostAndPort hap = multiplexedUsers.get(clientJid);
        return hap == null ? defaultServer.port : hap.port;
    }

    /**
     * Adds the given user with the given password, or replace the password if the user already existed.
     */
    public void setPassword(String username, String password) {
        authorizationMap.put(username, password);
    }


    // ProxyConfigurationProvider
    @Override
    public String getServer(String clientJid) {
        return getMultiplexedServerHost(clientJid);
    }

    @Override
    public int getServerPort(String clientJid) {
        return getMultiplexedServerPort(clientJid);
    }

    @Override
    public boolean isUserSilenced(String clientJid) {
        return isSilenced(clientJid);
    }


    // ConfigurationsConsumer

    /**
     * Sets the L337 property (when {@code true}, system will L337 messages).
     *
     * @param isL337 the L337 new value.
     */
    @Override
    public void setL337Processing(boolean isL337) {
        this.processL337 = isL337;
    }

    @Override
    public void silenceUser(String username) {
        silencedUsers.add(username);
    }

    @Override
    public void unSilenceUser(String username) {
        silencedUsers.remove(username);
    }

    @Override
    public void setDefaultServer(String host, int port) {
        defaultServer = new HostAndPort(host, port);
    }

    @Override
    public void multiplexUser(String clientJid, String host, int port) {
        if (clientJid == null) {
            // Rest of params are checked when creating HostAndPort object
            throw new IllegalArgumentException();
        }
        HostAndPort hap = multiplexedUsers.get(clientJid);
        if (hap == null) {
            hap = defaultServer;
        }
        multiplexedUsers.put(clientJid, new HostAndPort(host, port));
    }

    @Override
    public void multiplexToDefaultServer(String userJid) {
        multiplexedUsers.remove(userJid);
    }


    // Authentication provider
    @Override
    public boolean isValidUser(String username, String password) {
        return password != null && password.equals(authorizationMap.get(username));
    }


    // Metric provider
    @Override
    public int transportedBytes() {
        return transferredBytes;
    }

    @Override
    public List<String> silencedUsers() {
        return new LinkedList<>(silencedUsers);
    }

    // Metric consumer


    @Override
    public void transferredBytes(int amountOfBytes) {
        transferredBytes += amountOfBytes;
    }

    /**
     * Class that encapsulates host and port.
     */
    private static class HostAndPort {
        private final String host;
        private final int port;

        private HostAndPort(String host, int port) {
            if (host == null || host.isEmpty() || port < 0 || port > 0xFFFF) {
                throw new IllegalArgumentException();
            }
            this.host = host;
            this.port = port;
        }
    }
}
