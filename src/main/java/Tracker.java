import lombok.Getter;

@Getter
public class Tracker {

    //public event EventHandler<List<IPEndPoint>> PeerListUpdated;

    private String address;

    public Tracker(String address) {
        this.address = address;
    }

}
