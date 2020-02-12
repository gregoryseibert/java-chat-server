package com.company;

import java.time.ZoneId;
import java.time.ZonedDateTime;

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
        return user.getName() + ":\t" + content;
    }
}
