import lombok.Getter;

@Getter
public class FileItem {

    private String path;
    private long size;
    private long offset;

//    private String getFormattedSize() {
//        return Torrent.bytesToString(size);
//    }

}
