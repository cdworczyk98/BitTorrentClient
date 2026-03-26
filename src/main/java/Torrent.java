import lombok.Getter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Torrent {

    private String name;
    private List<FileItem> files;
    private String fileDirectory; //needs constructor
    private String downloadDirectory;

    private List<Tracker> trackers = new ArrayList<>();
    private String comment;
    private String createdBy;
    private LocalDateTime creationDate;

    private int blockSize;
    private int piecesSize;
    private long totalSize; //needs constructor

    private String FormattedPieceSize; //needs constructor
    private String FormattedTotalSize; //needs constructor

    private int pieceCount; //needs constructor

    private byte[][] pieceHashes;
    private boolean[] isPieceVerified;
    private boolean[][] isBlockAcquired;

    public String vereifiedPiecesString; //needs constructor
    public int verifieedPieceeCount; //needs constructor
    public double veereifiedRatio; //needs constructor
    public boolean isCompleted; //needs constructor
    public boolean isStarted; //needs constructor

    public long uploaded = 0;
    public long downloaded; //needs constructor
    public long left; //needs constructor

    private final byte[] infoHash = new byte[20];
    private  String hexStringInfoHash; //needs constructor
    private String urlSafeStringInfoHash; //needs constructor

    //public event EventHandler<List<IPEndPoint>> PeerListUpdated;

    private Object[] fileWriterLocks;

    public Torrent(String name, String location, List<FileItem> files, List<String> trackers, int piecesSize, byte[] pieceHashes, int blockSize) throws IOException {
        this.name = name;
        this.downloadDirectory = location;
        this.files = files;
        fileWriterLocks = new Object[files.size()];
        for (int i = 0; i < this.files.size(); i++) {
            this.fileWriterLocks[i] = new Object();
        }

        if (trackers != null) {
            for (String url : trackers) {
                Tracker tracker = new Tracker(url);
                this.trackers.add(tracker);
                //tracker.PeerListUpdated += HandlePeerListUpdated;
            }
        }

        this.piecesSize = piecesSize;
        this.blockSize = blockSize;

        int count = (int) Math.ceil(this.totalSize / (double) this.piecesSize);

        this.pieceHashes = new byte[count][];
        this.isPieceVerified = new boolean[count];
        this.isBlockAcquired = new boolean[count][];

        for (int i = 0; i < this.pieceCount; i++) {
            this.isBlockAcquired[i] = new boolean[getBlockCount(i)];
        }

        if (pieceHashes != null) {
            for (int i = 0; i < this.pieceCount; i++) {
                this.pieceHashes[i] = getHash(i);
            }
        } else {
            for (int i = 0; i < this.pieceCount; i++) {
                this.pieceHashes[i] = new byte[20];
                //Buffer.BlockCopy(pieceHashes, i * 20, PieceHashes[i], 0, 20);
            }
        }

        Object info = TorrentInfoToBEncodingObject(this);
        byte[] bytes = BEncoding.Encode(info);
        infoHash = DigestUtils.sha1Hex("my string")
    }

    public int getPieceSize(int piece) {
        if (piece == pieceCount - 1) {
            int remainder = Math.toIntExact(totalSize % piecesSize);
            if (remainder != 0) {
                return remainder;
            }
        }

        return piecesSize;
    }

    public int getBlockSize(int piece, int block) {
        if(block == getBlockCount(piece) - 1) {
            int reaminder = Math.toIntExact(getPieceSize(piece) % blockSize);
            if (reaminder != 0) {
                return reaminder;
            }
        }

        return blockSize;
    }

    public int getBlockCount(int piece) {
        return (int) (Math.ceil(getPieceSize(piece) / (double) blockSize));
    }

}
