package com.company;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Message {
    private User user;
    private List<User> recipients;
    private ZonedDateTime date;
    private String content;

    public Message(User user, String content) {
        this.user = user;
        this.content = content;
        recipients = new ArrayList<>();
        date = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
    }

    public User getUser() {
        return user;
    }

    public List<User> getRecipients() {
        return recipients;
    }

    public void addRecipient(User user) {
        recipients.add(user);
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "<b color=\"" + user.getColor() + "\">" + user.getName() + "</b>:\t" + content;
    }
}
