package Protocol;

/**
 * The 8-byte message: TOPIC | SUBTOPIC | BODY
 *
 *   [0]    topic
 *   [1]    subtopic  (which elevator this is for)
 *   [2]    body      (the command or state code)
 *   [3..6] value     (a number: speed, or percent open)
 *   [7]    checksum
 */
public final class Message {

    public static final int SIZE = 8;

    private final byte topic;
    private final byte sub;
    private final byte body;
    private final int  value;

    public Message(byte topic, byte sub, byte body, int value) {
        this.topic = topic;
        this.sub = sub;
        this.body = body;
        this.value = value;
    }

    public Message(byte topic, byte sub, byte body) {
        this(topic, sub, body, 0);
    }

    public byte topic() { return topic; }
    public byte sub()   { return sub; }
    public byte body()  { return body; }
    public int  value() { return value; }

    /** Pack into the 8 bytes that go on the wire. */
    public byte[] toBytes() {
        byte[] b = new byte[SIZE];
        b[0] = topic;
        b[1] = sub;
        b[2] = body;
        b[3] = (byte) (value >>> 24);
        b[4] = (byte) (value >>> 16);
        b[5] = (byte) (value >>> 8);
        b[6] = (byte) value;
        b[7] = checksum(b);
        return b;
    }

    /** Unpack 8 bytes back into a message. */
    public static Message fromBytes(byte[] b) {
        if (b == null || b.length != SIZE) {
            throw new IllegalArgumentException("message must be " + SIZE + " bytes");
        }
        if (b[7] != checksum(b)) {
            throw new IllegalArgumentException("bad checksum");
        }
        int v = ((b[3] & 0xFF) << 24) | ((b[4] & 0xFF) << 16)
                | ((b[5] & 0xFF) << 8)  |  (b[6] & 0xFF);
        return new Message(b[0], b[1], b[2], v);
    }

    private static byte checksum(byte[] b) {
        int x = 0;
        for (int i = 0; i < SIZE - 1; i++) {
            x ^= b[i];
        }
        return (byte) x;
    }

    /** "01 01 02 00 00 00 00 02" */
    public String hex() {
        byte[] b = toBytes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", b[i]));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        String s = Codes.topicName(topic) + " sub=" + (sub & 0xFF)
                + " " + Codes.bodyName(topic, body);
        if (value != 0) {
            s += " value=" + value;
        }
        return s;
    }
}