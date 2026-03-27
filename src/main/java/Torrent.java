import lombok.Getter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

    private byte[] infoHash = new byte[20];
    private  String hexStringInfoHash; //needs constructor
    private String urlSafeStringInfoHash; //needs constructor

    //public event EventHandler<List<IPEndPoint>> PeerListUpdated;

    private Object[] fileWriterLocks;

    public Torrent(String name, String location, List<FileItem> files, List<String> trackers, int piecesSize, byte[] pieceHashes, int blockSize) throws IOException, NoSuchAlgorithmException {
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
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(bytes);
        byte[] digest = md.digest();

        StringBuilder hexString = new StringBuilder();

        for (byte b : digest) {
            hexString.append(String.format("%02x", b));
        }

        this.infoHash = hexString.toString().getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < this.pieceCount; i++) {
            Verify(i);
        }
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

    public byte[] read(long start, int length) throws IOException {
        long end = start + length;
        byte[] buffer = new byte[length];

        for (int i=0; i<files.size(); i++)
        {
            if ((start < files.getFirst().getOffset() && end < files.get(i).getOffset()) || (start > files.get(i).getOffset() + files.size() && end > files.get(i).getOffset() + files.get(i).getSize()))
                continue;

            String filePath = downloadDirectory + "/" + fileDirectory + files.get(i).getPath();

            File file = new File(filePath);
            if (!file.exists()) {
                return  null;
            }

            long fstart = Math.max(0, start - files.get(i).getOffset());
            long fend = Math.min(end - files.get(i).getOffset(), files.get(i).getSize());
            int flength = (int) (fend - fstart);
            int bstart = Math.max(0, (int) (files.get(i).getOffset() - start));

            try(FileInputStream stream = new FileInputStream(filePath); FileChannel channel = stream.getChannel()) {
                channel.position(fstart);
                ByteBuffer bb = ByteBuffer.wrap(buffer, bstart, flength);
                channel.read(bb);
            }

        }

        return buffer;
    }

    public void write(long start, byte[] bytes) throws IOException {
        long end = start + bytes.length;

        for (int i = 0; i < files.size(); i++)
        {
            if ((start < files.get(i).getOffset() && end < files.get(i).getOffset()) ||
                    (start > files.get(i).getOffset() + files.get(i).getSize() && end > files.get(i).getOffset() + files.get(i).getSize()))
                continue;

            String filePath = downloadDirectory + "/" + fileDirectory + files.get(i).getPath();

            String dir = new File(filePath).getParent();
            if (!Files.isDirectory(Paths.get(dir))) {
                Files.createDirectories(Paths.get(dir));
            }

            synchronized (fileWriterLocks[i])
            {
                try(RandomAccessFile raf = new RandomAccessFile(filePath, "rw"); FileChannel channel = raf.getChannel()) {
                {
                    long fstart = Math.max(0, start - files.get(i).getOffset());
                    long fend = Math.min(end - files.get(i).getOffset(), files.get(i).getSize());
                    int flength = (int) (fend - fstart);
                    int bstart = Math.max(0, (int) (files.get(i).getOffset() - start));

                    channel.position(fstart);
                    ByteBuffer bb = ByteBuffer.wrap(bytes, bstart, flength);
                    while (bb.hasRemaining()) {
                        channel.write(bb);
                    }
                }
            }
        }
    }
        }

}
