package Protocol;

public interface Messenger {

    /** Join, and get back an ID (the elevator number). */
    int register();

    /** Ask to receive messages on this topic/subtopic. Codes.ANY = wildcard. */
    void subscribe(int id, byte topic, byte sub);

    /** Send a message. */
    void publish(int id, Message message);

    /** Take the next waiting message, or null if there is none. Does not block. */
    Message getMsg(int id, byte topic, byte sub);
}