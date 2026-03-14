void main() throws IOException {
    String input = "d8:announce33:http://192.168.1.72:6969/announce7:comment12:test comment10:created by18:qBittorrent v5.1.213:creation datei1773413412e4:infod6:lengthi3120e4:name15:random_text.txt12:piece lengthi16384e6:pieces20:¶‡\u0090´êëTâÑp\u0013æ/Ø\\¤ Úàóee";
    Byte[] bytes = new Byte[input.length()];
    for (int i = 0; i < input.length(); i++) {
        bytes[i] = (byte) input.charAt(i);
    }
    Object test = BEncoding.Decode(bytes);
    byte[] test2 = BEncoding.Encode(test);
}
