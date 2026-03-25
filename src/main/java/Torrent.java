import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class Torrent {

    private String name;
    private List<FileItem> files;
    private String fileDirectory; //needs constructor
    private String downloadDirectory;

    private List<Tracker> trackers;
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
