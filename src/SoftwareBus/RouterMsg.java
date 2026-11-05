package SoftwareBus;

import java.util.UUID;

public sealed interface RouterMsg {
    record Connected() implements RouterMsg {}
    record Valid_UUID(UUID id) implements RouterMsg {}
    record Valid_Resp_UUID(UUID id, boolean resp_uuid) implements RouterMsg {}
    record Publish(UUID id, Message message) implements RouterMsg {}
    record Valid_Publish(UUID id, boolean valid) implements RouterMsg {}
    record SendMessage(Message message) implements RouterMsg {}

    default String encode() {
        return switch (this){
            case Connected() -> "CONNECTED";
            case Valid_UUID(var id)-> "IS_VALID " + id;
            case Valid_Resp_UUID(var id, var resp_uuid)-> "VALID_RESP " + id + " " + resp_uuid;
            case Publish(UUID id, Message message)-> "PUBLISH " + id + " " + message.toString();
            case Valid_Publish(var id, var valid)-> "VALID_PUBLISH " + id + " " + valid;
            case SendMessage(Message message)-> "SEND_MESSAGE " + message.toString();
        };
    }

    static RouterMsg parse(String msg){
        if (msg == null) throw new IllegalArgumentException("null line");
        String[] parts = msg.split(" ");
        return switch (parts[0]){
            case "CONNECTED" -> new Connected();
            case "IS_VALID" -> new Valid_UUID(UUID.fromString(parts[1]));
            case "VALID_RESP" -> new Valid_Resp_UUID(UUID.fromString(parts[1]), Boolean.parseBoolean(parts[2]));
            case "PUBLISH" -> new Publish(UUID.fromString(parts[1]), Message.fromString(parts[2]));
            case "VALID_PUBLISH" -> new Valid_Publish(UUID.fromString(parts[1]), Boolean.parseBoolean(parts[2]));
            case "SEND_MESSAGE" -> new SendMessage(Message.fromString(parts[1]));
            default -> throw new IllegalStateException("Unexpected SoftwareBus.SoftwareBus.RouterMsg: " + msg);
        };
    }
}
