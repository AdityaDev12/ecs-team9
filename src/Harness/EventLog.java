package Harness;

import Protocol.Message;

/** Prints every message that goes out or comes in, with a timestamp. */
public final class EventLog {

    private final long start = System.nanoTime();
    private final boolean showHex;

    public EventLog(boolean showHex) {
        this.showHex = showHex;
    }

    public void sent(Message m)     { line("SEND", m); }
    public void received(Message m) { line("RECV", m); }

    public void note(String text) {
        System.out.println(stamp() + " ---- " + text);
    }

    public void error(String text) {
        System.out.println(stamp() + " !!   " + text);
    }

    private void line(String direction, Message m) {
        String s = stamp() + " " + direction + " " + m;
        if (showHex) {
            s += "   (raw: " + m.hex() + ")";
        }
        System.out.println(s);
    }

    private String stamp() {
        long ms = (System.nanoTime() - start) / 1_000_000L;
        return String.format("[%02d:%02d.%03d]", ms / 60000, (ms % 60000) / 1000, ms % 1000);
    }
}