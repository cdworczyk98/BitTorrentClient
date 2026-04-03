import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Getter
@Setter
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
    private String hexStringInfoHash; //needs constructor
    private String urlSafeStringInfoHash; //needs constructor

    //public event EventHandler<List<IPEndPoint>> PeerListUpdated;

    //public event EventHandler<int> PieceVerified;

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
        if (block == getBlockCount(piece) - 1) {
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

        for (int i = 0; i < files.size(); i++) {
            if ((start < files.getFirst().getOffset() && end < files.get(i).getOffset()) || (start > files.get(i).getOffset() + files.size() && end > files.get(i).getOffset() + files.get(i).getSize()))
                continue;

            String filePath = downloadDirectory + "/" + fileDirectory + files.get(i).getPath();

            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }

            long fstart = Math.max(0, start - files.get(i).getOffset());
            long fend = Math.min(end - files.get(i).getOffset(), files.get(i).getSize());
            int flength = (int) (fend - fstart);
            int bstart = Math.max(0, (int) (files.get(i).getOffset() - start));

            try (FileInputStream stream = new FileInputStream(filePath); FileChannel channel = stream.getChannel()) {
                channel.position(fstart);
                ByteBuffer bb = ByteBuffer.wrap(buffer, bstart, flength);
                channel.read(bb);
            }

        }

        return buffer;
    }

    public void write(long start, byte[] bytes) throws IOException {
        long end = start + bytes.length;

        for (int i = 0; i < files.size(); i++) {
            if ((start < files.get(i).getOffset() && end < files.get(i).getOffset()) ||
                    (start > files.get(i).getOffset() + files.get(i).getSize() && end > files.get(i).getOffset() + files.get(i).getSize()))
                continue;

            String filePath = downloadDirectory + "/" + fileDirectory + files.get(i).getPath();

            String dir = new File(filePath).getParent();
            if (!Files.isDirectory(Paths.get(dir))) {
                Files.createDirectories(Paths.get(dir));
            }

            synchronized (fileWriterLocks[i]) {
                try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw"); FileChannel channel = raf.getChannel()) {
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

    public byte[] ReadPiece(int piece) {
        return Read(piece * piecesSize, getPieceSize(piece));
    }

    public byte[] ReadBlock(int piece, int offset, int length) {
        return Read(piece * piecesSize + offset, length);
    }

    public void WriteBlock(int piece, int block, byte[] bytes) {
        Write(piece * piecesSize + block * blockSize, bytes);
        isBlockAcquired[piece][block] = true;
        Verify(piece);
    }

    public void Verify(int piece) {
        byte[] hash = getHash(piece);

        boolean isVerified = (hash != null && Arrays.equals(hash, pieceHashes[piece]));

        if (isVerified) {
            isPieceVerified[piece] = true;

            for (int j = 0; j < isBlockAcquired[piece].length; j++) {
                isBlockAcquired[piece][j] = true;
            }

            var handler = PieceVerified; //figure out this var
            if (handler != null) {
                handler(this, piece);
            }

            return;
        }

        isPieceVerified[piece] = false;

        // reload the entire piece
        if (isBlockAcquired[piece].All(x = > x)) {
            for (int j = 0; j < isBlockAcquired[piece].length; j++)
                isBlockAcquired[piece][j] = false;
        }
    }

    public byte[] getHash(int piece) throws NoSuchAlgorithmException {
        byte[] data = ReadPiece(piece);

        if (data == null)
            return null;

        return MessageDigest.getInstance("SHA1").digest(data);
    }

    public static Torrent LoadFromFile(String filePath, String downloadPath) throws IOException {
        Object obj = BEncoding.DecodeFile(filePath);
        String name = Paths.get(filePath).getFileName().toString();  //.GetFileNameWithoutExtension(filePath);

        return BEncodingObjectToTorrent(obj, name, downloadPath);
    }

    public static void SaveToFile(Torrent torrent) throws IOException {
        Object obj = TorrentToBEncodingObject(torrent);

        BEncoding.EncodeToFile(obj, torrent.getName() + ".torrent");
    }

    public static long dateTimeToUnixTimestamp( LocalDateTime time )
    {
        return time.toEpochSecond(ZoneOffset.UTC);
    }

    private static Object TorrentToBEncodingObject(Torrent torrent)
    {
        Map<String,Object> dict = new HashMap<>();

        if( torrent.trackers.size() == 1 ) {
            dict.put("announce", torrent.trackers.getFirst().getAddress().getBytes(StandardCharsets.UTF_8));
        }
        else {
            dict.put("comment", torrent.getComment().getBytes(StandardCharsets.UTF_8));
            dict.put("announce", torrent.getTrackers().stream().map(x -> (Object) x.getAddress()).toList());
            dict.put("created by", torrent.getCreatedBy().getBytes(StandardCharsets.UTF_8));
            dict.put("creation date", dateTimeToUnixTimestamp(torrent.creationDate));
            dict.put("encoding", StandardCharsets.UTF_8.name());
            dict.put("info", TorrentInfoToBEncodingObject(torrent));
        }

        return dict;
    }

    private static Object TorrentInfoToBEncodingObject(Torrent torrent)
    {
        Map<String,Object> dict = new HashMap<>();

        dict.put("piece length", (long)torrent.piecesSize);
        byte[] pieces = new byte[20 * torrent.pieceCount];
        for (int i = 0; i < torrent.pieceCount; i++) {
            Buffer.BlockCopy(torrent.pieceHashes[i], 0, pieces, i * 20, 20);
        }
        dict.put("pieces", pieces);

        if (torrent.IsPrivate.HasValue)
            dict.put("private", torrent.IsPrivate.Value ? 1L : 0L);

        if (torrent.files.size() == 1)
        {
            dict.put("name", torrent.files.getFirst().getPath());
            dict("length", torrent.files.getFirst().getSize()0;
        }
        else
        {
            List<Object> files = new List<Object>();

            for (FileItem f : torrent.files)
            {
                Map<String,Object> fileDict = new HashMap<>();
                fileDict.put("path", Arrays.stream(f.getPath().split("/")).map(x -> (Object) x.getBytes(StandardCharsets.UTF_8)).toList());
                fileDict.put("length", f.getSize());
                files.add(fileDict);
            }

            dict.put("files", files);
            dict.put("name", torrent.getFileDirectory().substring(0, torrent.getFileDirectory().length() - 1));
        }

        return dict;
    }

    public static String DecodeUTF8String( Object obj ) throws Exception {
        byte[] bytes = obj.toString().getBytes(StandardCharsets.UTF_8);

        if (bytes == null) {
            throw new Exception("unable to decode utf-8 string, object is not a byte array");
        }

        return String.valueOf(bytes);
    }

    public static LocalDateTime UnixTimeStampToDateTime( double unixTimeStamp )
    {
        return LocalDateTime.ofEpochSecond((int) unixTimeStamp, 0, ZoneOffset.UTC);
    }

    private static Torrent BEncodingObjectToTorrent(Object bencoding, String name, String downloadPath) throws Exception {
        Map<String,Object> obj = (HashMap<String,Object>) bencoding;

        if (obj == null) {
            throw new Exception("not a torrent file");
        }

        // !! handle list
        List<String> trackers = new ArrayList<>();
        if (obj.containsKey("announce"))
            trackers.add(DecodeUTF8String(obj.get("announce")));

        if (!obj.containsKey("info"))
            throw new Exception("Missing info section");

        Map<String,Object> info = (HashMap<String,Object>)obj.get("info");

        if (info == null)
            throw new Exception("error");

        List<FileItem> files = new ArrayList<>();

        if (info.containsKey("name") && info.containsKey("length"))
        {
            files.add(new FileItem(DecodeUTF8String(info.get("name")), (long)info.get("length")));
        }
        else if (info.containsKey("files"))
        {
            long running = 0;

            for (Object item : (List<Object>)info.get("files"))
            {
                var dict = (HashMap<String, Object>) item;

                if (dict == null || !dict.containsKey("path") || !dict.containsKey("length") )
                    throw new Exception("error: incorrect file specification");

                String path = String.join("/", (List<Object>) dict.get("path").stream().map(x -> DecodeUTF8String(x)));

                long size = (long)dict.get("length");

                files.add(new FileItem(path, size, running));

                running += size;
            }
        }
        else
        {
            throw new Exception("error: no files specified in torrent");
        }

        if (!info.containsKey("piece length"))
            throw new Exception("error");
        int pieceSize = (int) info.get("piece length");

        if (!info.containsKey("pieces")) {
            throw new Exception("error");
        }
        byte[] pieceHashes = (byte[])info.get("pieces");

        boolean isPrivate = false;
        if (info.containsKey("private")) {
            isPrivate = ((long)info.get("private") == 1L);
        }

        Torrent torrent = new Torrent(name, downloadPath, files, trackers, pieceSize, pieceHashes, 16384, isPrivate );

        if (obj.containsKey("comment"))
            torrent.setComment(DecodeUTF8String(obj.get("comment")));

        if (obj.containsKey("created by"))
            torrent.setCreatedBy(DecodeUTF8String(obj.get("created by")));

        if (obj.containsKey("creation date"))
            torrent.setCreationDate(UnixTimeStampToDateTime( (int) obj.get("creation date")));

        if (obj.containsKey("encoding"))
            torrent.setEncoding = Encoding.GetEncoding(DecodeUTF8String(obj["encoding"]));

        return torrent;
    }

    public static Torrent Create(String path, List<String> trackers = null, int pieceSize = 32768, String comment = "")
    {
        String name = "";
        List<FileItem> files = new ArrayList<>();

        if (Files.exists(Paths.get(path)))
        {
            name = Paths.get(path).getFileName().toString();

            long size = new FileInfo(path).Length;
            files.add(new FileItem(path, size));
        }
        else
        {
            name = path;
            String directory = path + "/";

            long running = 0;
            for (String file : Directory.EnumerateFiles(path, "*.*", SearchOption.AllDirectories))
            {
                string f = file.Substring(directory.Length);

                if (f.StartsWith("."))
                    continue;

                long size = new FileInfo(file).Length;

                files.Add(new FileItem()
                {
                    Path = f,
                    Size = size,
                    Offset = running
                });

                running += size;
            }
        }

        Torrent torrent = new Torrent(name, "", files, trackers, pieceSize);
        torrent.Comment = comment;
        torrent.CreatedBy = "TestClient";
        torrent.CreationDate = DateTime.Now;
        torrent.Encoding = Encoding.UTF8;

        return torrent;
    }

}
