import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;

public class BEncoding {

    private static final byte DictionaryStart = "d".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte DictionaryEnd = "e".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte ListStart = "l".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte ListEnd = "e".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte NumberStart = "i".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte NumberEnd = "e".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte ByteArrayDivider = ":".getBytes(StandardCharsets.UTF_8)[0];

    public static Object Decode(Byte[] bytes) {
        Iterator<Byte> iterator = Arrays.stream(bytes).iterator();




    }

    public static Object DecodeNextObject(Iterator<Byte> iterator) {

        byte b = iterator.next();
        if (b == DictionaryStart) {
            return DecodeDictionary(iterator);
        }

        if (b == ListStart) {
            return DecodeList(iterator);
        }

        if (b == NumberStart) {
            return DecodeNumber(iterator);
        }

        return DecodeByteArray(iterator);
    }

    public static long DecodeNumber(Iterator<Byte> iterator) {
        StringBuilder stringBuilder = new StringBuilder();
        while (iterator.hasNext()) {
            byte b = iterator.next();

            if(b == NumberEnd) {
                break;
            }

            stringBuilder.append((char) b);
        }

        return Long.parseLong(stringBuilder.toString());
    }

    public static Object DecodeFile(String filePathStr) throws IOException {
        Path filePath = Path.of(filePathStr);
        if(!Files.exists(filePath)) {
            throw new FileNotFoundException("Unable to find file: " + filePathStr);
        }

        byte[] bytes = Files.readAllBytes(filePath);

        Byte[] newByteArray = new Byte[bytes.length];
        for(int i = 0; i < bytes.length; i++) {
            newByteArray[i] = bytes[i];
        }

        return Decode(newByteArray);
    }


}
