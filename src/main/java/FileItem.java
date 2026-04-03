import lombok.Getter;

@Getter
public class FileItem {

    private String path;
    private long size;
    private long offset;

    public FileItem(String path, long size) {
        this.path = path;
        this.size = size;
    }

    public FileItem(String path, long size, long offset) {
        this.path = path;
        this.size = size;
        this.offset = offset;
    }

//    private String getFormattedSize() {
//        return Torrent.bytesToString(size);
//    }

}
