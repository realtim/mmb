package ru.mmb.datacollector.widget;

import android.widget.TextView;

import java.util.LinkedList;

public class ConsoleMessagesAppender {
    private final int MAX_CAPACITY = 200;

    private final TextView console;
    private LinkedList<String> messages = new LinkedList<String>();

    public ConsoleMessagesAppender(TextView console) {
        this.console = console;
    }

    public void appendMessage(String message) {
        if (messages.size() < MAX_CAPACITY) {
            messages.addFirst(message);
        } else {
            messages.removeLast();
            messages.addFirst(message);
        }
        console.setText(buildMessagesText());
    }

    private String buildMessagesText() {
        StringBuilder sb = new StringBuilder();
        for (String message : messages) {
            sb.append(message).append("\n");
        }
        return sb.toString();
    }

    public void clear() {
        messages.clear();
        console.setText("");
    }
}
