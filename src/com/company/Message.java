package com.company;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private User user;
    private ZonedDateTime date;
    private String content;

    public Message(User user, String content) {
        this.user = user;
        this.content = content;
        date = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
    }

    public User getUser() {
        return user;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
        //DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "[" + date.format(dateFormat) + "]" + "[" + date.format(timeFormat) + "] " + user.getName() + ": " + content;
    }
}
