import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class BEncoding {

    private static final byte DictionaryStart = "d".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte DictionaryEnd = "e".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte ListStart = "l".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte ListEnd = "e".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte NumberStart = "i".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte NumberEnd = "e".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte ByteArrayDivider = ":".getBytes(StandardCharsets.UTF_8)[0];

    private static byte currentByte;
    private static byte previousByte;

    private static byte moveIterator(Iterator<Byte> iterator) {
        if (currentByte != 0) {
            previousByte = currentByte;
        }
        currentByte = iterator.next();

        return currentByte;
    }

    public static Object Decode(Byte[] bytes) {
        Iterator<Byte> iterator = Arrays.stream(bytes).iterator();
        moveIterator(iterator);
        return DecodeNextObject(iterator);
    }

    public static byte[] Encode(Object object) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(object.toString().getBytes(StandardCharsets.UTF_8));

        EncodeNextObject(byteArrayOutputStream, object);

        return byteArrayOutputStream.toByteArray();
    }

    // Encoding methods
    
    public static void EncodeNextObject(ByteArrayOutputStream byteArrayOutputStream, Object object) {
        if (object instanceof byte[]) {
            
        } else if (object instanceof String) {
            
        } else if (object instanceof Long) {
            
        } else if (object instanceof List<?> ) {
            
        } else if (object instanceof Map) {

        } else {
            throw new IllegalArgumentException("Invalid object type");
        }
    }

    public static void EncodeNumber(ByteArrayOutputStream byteArrayOutputStream, long input) throws IOException {
        byteArrayOutputStream.write(NumberStart);
        byteArrayOutputStream.write(String.valueOf(input).getBytes());
        byteArrayOutputStream.write(NumberEnd);
    }

    public static void EncodeByteArray(ByteArrayOutputStream byteArrayOutputStream, byte[] body) throws IOException {
        byteArrayOutputStream.write(String.valueOf(body.length).getBytes());
        byteArrayOutputStream.write(ByteArrayDivider);
        byteArrayOutputStream.write(body);
    }

    public static void EncodeString(ByteArrayOutputStream byteArrayOutputStream, String input) throws IOException {
        EncodeByteArray(byteArrayOutputStream, input.getBytes(StandardCharsets.UTF_8));
    }

    public static void EncodeList(ByteArrayOutputStream byteArrayOutputStream, List<Object> input) {
        byteArrayOutputStream.write(ListStart);
        for(Object o : input) {
            EncodeNextObject(byteArrayOutputStream, o);
        }
        byteArrayOutputStream.write(ListEnd);
    }

    public static void EncodeMap(ByteArrayOutputStream byteArrayOutputStream, Map<String, Object> input) throws IOException {
        byteArrayOutputStream.write(DictionaryStart);

        List<Object> sortedKeys = input.keySet().stream()
                        .sorted(Comparator.comparing(
                                x -> x.getBytes(StandardCharsets.UTF_8),
                                Arrays::compare
                        ))
                        .collect(Collectors.toList());

        byteArrayOutputStream.write(DictionaryEnd);
    }

    public static void EncodeToFile(Object object, String filePath) throws IOException {
        Files.write(Paths.get(filePath), Encode(object));
    }

    // Decoding methods----------------

    public static Object DecodeNextObject(Iterator<Byte> iterator) {

         if (currentByte == DictionaryStart) {
             moveIterator(iterator);
            return DecodeDictionary(iterator);
        }

        if (currentByte == ListStart) {
            return DecodeList(iterator);
        }

        if (currentByte == NumberStart) {
            return DecodeNumber(iterator);
        }

        return DecodeByteArray(iterator);
    }

    public static Map<String, Object> DecodeDictionary(Iterator<Byte> iterator) {
        Map<String, Object> map = new HashMap<>();
        List<String> keys = new ArrayList<>();

        while (iterator.hasNext()) {
            if (currentByte == DictionaryEnd) {
                break;
            }

            String key = new String(DecodeByteArray(iterator),  StandardCharsets.UTF_8);
            moveIterator(iterator);
            Object value = DecodeNextObject(iterator);

            keys.add(key);
            map.put(key, value);
            moveIterator(iterator);
        }


        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys);
        if(!sortedKeys.equals(keys)) {
            throw new IllegalStateException("Error loading dictionary, keys are not sorted.");
        }

        return map;
    }

    public static byte[] DecodeByteArray(Iterator<Byte> iterator) {
        List<Byte> lengthBytes = new ArrayList<>();

        do {
            if(currentByte == ByteArrayDivider) {
                break;
            }
            lengthBytes.add(currentByte);
            moveIterator(iterator);
        }  while (iterator.hasNext());

        String lengthString = getStringFromByteList(lengthBytes);

        int length;
        try {
            length = Integer.parseInt(lengthString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse length");
        }

        byte[] bytes2 = new byte[length];

        for  (int i = 0; i < length; i++) {
            moveIterator(iterator);
            bytes2[i] = currentByte;
        }

        return bytes2;
    }

    public static List<Object> DecodeList(Iterator<Byte> iterator) {
        List<Object> list = new ArrayList<>();

        while (iterator.hasNext()) {

            byte b = iterator.next();
            if(b == ListEnd) {
                break;
            }
            list.add(iterator.next());
        }

        return list;
    }

    public static long DecodeNumber(Iterator<Byte> iterator) {
        StringBuilder stringBuilder = new StringBuilder();
        while (iterator.hasNext()) {
           moveIterator(iterator);

            if(currentByte == NumberEnd) {
                break;
            }

            stringBuilder.append((char) currentByte);
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

    private static String getStringFromByteList(List<Byte> lengthBytes) {
        byte[] bytes = new byte[lengthBytes.size()];
        for (int i = 0; i < lengthBytes.size(); i++) {
            bytes[i] = lengthBytes.get(i);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }


}
