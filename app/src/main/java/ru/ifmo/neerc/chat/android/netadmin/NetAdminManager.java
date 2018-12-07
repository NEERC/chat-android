package ru.ifmo.neerc.chat.android.netadmin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

import android.util.Log;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.CollectionNode;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemDeleteEvent;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SubscribeForm;
import org.jivesoftware.smackx.pubsub.SubscribeOptionFields;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.jivesoftware.smackx.pubsub.listener.ItemDeleteListener;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import ru.ifmo.neerc.chat.packet.ComputerExtension;

public class NetAdminManager {
    private static final String TAG = "NetAdmin";

    private static final String ROOT_NODE_ID = "rooms";

    private static final Map<XMPPConnection, NetAdminManager> INSTANCES = new WeakHashMap<>();

    private final XMPPConnection connection;
    private final PubSubManager pubSubManager;

    private final Map<String, Map<String, Computer>> rooms = new TreeMap<>();
    private final Map<String, Computer> computers = new HashMap<>();
    private final Map<String, LeafNode> nodes = new HashMap<>();

    private final List<NetAdminListener> listeners = new ArrayList<NetAdminListener>();

    private boolean isDiscovering = false;

    private final ItemEventListener<PayloadItem<ComputerExtension>> computerListener = new ItemEventListener<PayloadItem<ComputerExtension>>() {
        @Override
        public void handlePublishedItems(ItemPublishEvent<PayloadItem<ComputerExtension>> items) {
            Computer computer = computers.get(items.getNodeId());

            if (!items.getItems().isEmpty()) {
                ComputerExtension payload = items.getItems().get(0).getPayload();
                computer.setReachable(payload.isReachable());

                Log.d(TAG, "Computer " + computer.getName() + " is " + (computer.isReachable() ? "reachable" : "not reachable"));

                if (!isDiscovering) {
                    for (NetAdminListener listener : listeners) {
                        listener.onComputerChanged(computer.getName(), computer);
                    }
                }
            }
        }
    };

    public static synchronized NetAdminManager getInstanceFor(XMPPConnection connection) {
        NetAdminManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new NetAdminManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    private NetAdminManager(XMPPConnection connection) {
        this.connection = connection;
        this.connection.addConnectionListener(new ConnectionListener());

        pubSubManager = PubSubManager.getInstance(connection);
    }

    public void addListener(NetAdminListener listener) {
        listeners.add(listener);
    }

    public boolean isDiscovering() {
        return isDiscovering;
    }

    public Collection<String> getRooms() {
        if (isDiscovering) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(rooms.keySet());
        }
    }

    public Collection<Computer> getComputers(String room) {
        return Collections.unmodifiableCollection(rooms.get(room).values());
    }

    private void discoverRooms() throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        Log.d(TAG, "Discovering rooms");

        isDiscovering = true;

        for (NetAdminListener listener : listeners) {
            listener.onRoomsChanged();
        }

        DiscoverItems roomItems = pubSubManager.discoverNodes(ROOT_NODE_ID);

        for (DiscoverItems.Item roomItem : roomItems.getItems()) {
            Log.d(TAG, "Discovered room " + roomItem.getNode());

            DiscoverItems computerItems = pubSubManager.discoverNodes(roomItem.getNode());

            final Map<String, Computer> room = new TreeMap<String, Computer>();
            rooms.put(roomItem.getNode(), room);

            for (DiscoverItems.Item computerItem : computerItems.getItems()) {
                Log.d(TAG, "Discovered computer " + computerItem.getNode());
                final LeafNode computerNode = pubSubManager.getNode(computerItem.getNode());

                Computer computer = new Computer(computerNode.getId());

                room.put(computer.getName(), computer);
                computers.put(computer.getName(), computer);

                nodes.put(computerNode.getId(), computerNode);

                computerNode.addItemEventListener(computerListener);
                computerNode.subscribe(connection.getUser().toString());
            }
        }

        isDiscovering = false;

        for (NetAdminListener listener : listeners) {
            listener.onRoomsChanged();
        }
    }

    private class ConnectionListener extends AbstractConnectionListener {
        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
            if (((XMPPTCPConnection) connection).streamWasResumed())
                return;

            try {
                discoverRooms();
            } catch (XMPPErrorException | NoResponseException | NotConnectedException | InterruptedException e) {
                Log.e(TAG, "Failed to discover rooms", e);
            }
        }
    }
}
