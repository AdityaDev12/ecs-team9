package SoftwareBus;

public record Message(int topic, int subtopic, int body) {

    @Override
    public String toString() {
        return topic + "_" + subtopic + "_" + body;
    }

    public static Message fromString(String message){
        String[] parts = message.split("_");
        return new Message(Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
