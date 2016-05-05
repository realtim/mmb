package ru.mmb.loggermanager.widget;

import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class ConsoleMessagesAppender {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    private final int MAX_CAPACITY = 200;

    private final TextView console;
    private LinkedList<String> messages = new LinkedList<String>();

    public ConsoleMessagesAppender(TextView console) {
        this.console = console;
    }

    public void appendMessage(String message) {
        if (messages.size() < MAX_CAPACITY) {
            messages.addFirst(sdf.format(new Date()) + " " + message);
        } else {
            messages.removeLast();
            messages.addFirst(sdf.format(new Date()) + " " + message);
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
