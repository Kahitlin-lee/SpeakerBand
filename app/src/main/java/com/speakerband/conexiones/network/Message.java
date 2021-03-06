package com.speakerband.conexiones.network;

import java.io.Serializable;

/**
 * Created by Catalina on 07/09/2017.
 */
public class Message implements Serializable {

    public MessageType messageType;
    public byte[] content;

    public Message(MessageType messageType, byte[] message) {
        this.messageType = messageType;
        this.content = message;
    }

    public byte[] getContent() {
        return content;
    }
}
