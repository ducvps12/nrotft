package network;

import java.io.IOException;

public class Message extends network.io.Message {

    public Message(int command) {
        super(command);
    }

    public Message(byte command) {
        super(command);
    }

    public Message(byte command, byte[] data) {
        super(command, data);
    }

    public void writeLongByEmti(long value, boolean writeInt) throws IOException {
        if (writeInt) {
            writeInt((int) value);
            return;
        }
        writeLong(value);
    }
}
