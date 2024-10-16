package br.imd.processors;

public class MessageProcessor {
    public String[] processMessage(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        String[] parts = message.split("-");
        if (parts.length == 0) {
            return null;
        }
        return parts;
    }
}
