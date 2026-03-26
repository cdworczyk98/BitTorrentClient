import lombok.Getter;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Getter
public class Tracker {

    private final List<BiConsumer<Object, List<InetSocketAddress>>> peerListUpdatedListeners = new ArrayList<>();

    private String address;

    public Tracker(String address) {
        this.address = address;
    }

    public void addPeerListUpdatedListener(BiConsumer<Object, List<InetSocketAddress>> listener) {
        peerListUpdatedListeners.add(listener);
    }

    public void removePeerListUpdatedListener(BiConsumer<Object, List<InetSocketAddress>> listener) {
        peerListUpdatedListeners.remove(listener);
    }

    protected void onPeerListUpdated(List<InetSocketAddress> peers) {
        for (BiConsumer<Object, List<InetSocketAddress>> listener : peerListUpdatedListeners) {
            listener.accept(this, peers);
        }
    }

}
