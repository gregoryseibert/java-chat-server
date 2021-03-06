package com.company;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler extends Thread {
    private final ChatServer chatServer;

    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    private final User user;
    private final Map<String, ICommand> commands;
    private final Whitelist whitelist;
    private final Timer messageWritingTimer;

    private int messageCounter = 0;
    private boolean initialized = false;
    private boolean isRunning;
    private boolean messageWritingLock = false;

    private final int maximumMessageLength = 250;
    private final int messageWritingLockTreshhold = 200;
    private final String commandPattern = "\\\\(\\w+)(\\s([a-zA-Z0-9äöüÄÖÜ]+))?";

    public ClientHandler(ChatServer chatServer, Socket socket, Map<String, ICommand> commands, User user, Whitelist whitelist) throws IOException {
        this.chatServer = chatServer;
        this.socket = socket;
        this.commands = commands;
        this.user = user;
        this.whitelist = whitelist;

        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        messageWritingTimer = new Timer();

        isRunning = true;
    }

    @Override
    public void run() {
        String received;
        isRunning = true;

        while (isRunning) {
            try {
                if (!initialized) {
                    writeCurrentMessages();

                    initialized = true;
                }

                received = reader.readLine();

                if (received != null && !messageWritingLock) {
                    handleReceivedString(received);
                } else if(received != null) {
                    System.out.println(user.getName() + " tried to spam messages.");
                }
            } catch (IOException e) {
                isRunning = false;

                switch (e.getMessage()) {
                    case "Connection reset":
                    case "socket closed":
                        break;
                    default:
                        e.printStackTrace();
                        break;
                }

                chatServer.broadcastMessage("The user '" + user.getName() + "' has been disconnected from the server.");
                System.out.println("Client '" + user.getName() + "' has been disconnected from the server.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            socket.close();
            writer.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleReceivedString(String received) {
        messageWritingLock = true;

        messageWritingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                messageWritingLock = false;
            }
        }, messageWritingLockTreshhold);

        System.out.println(user.getName() + ": " + received);

        if (received.startsWith("\\")) {
            handleCommand(received);
        } else if (received.length() > maximumMessageLength) {
            writer.println("Your message is too long. Maximum of 250 characters is allowed.");
            writer.flush();
        } else if (received.length() > 0) {
            handleMessage(received);
        }
    }

    private void handleCommand(String received) {
        Pattern pattern = Pattern.compile(commandPattern);
        Matcher matcher = pattern.matcher(received);

        if (matcher.find()) {
            ICommand command = commands.get(matcher.group(1));

            if (command != null) {
                writeCustomMessage("Executing command '" + matcher.group(1) + "'.");
                command.function(this, matcher.group(3));
            } else {
                writeCustomMessage("Unknown command '" + matcher.group(1) + "'.");
            }
        } else {
            writeCustomMessage("Couldn't parse command.");
        }
    }

    private void handleMessage(String received) {
        String cleanContent = Jsoup.clean(received, whitelist);

        Message message = new Message(user, cleanContent);

        Pattern pattern = Pattern.compile("(@(\\w+))+");
        Matcher matcher = pattern.matcher(cleanContent);
        while (matcher.find()) {
            String recipientName = matcher.group(2);
            User recipient = chatServer.getUserByUsername(recipientName);
            if (recipient != null) {
                message.addRecipient(recipient);
            }
        }

        chatServer.addMessage(message);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void writeCurrentMessages() {
        int currentNumberOfMessages = chatServer.getCurrentNumberOfMessages();
        if (currentNumberOfMessages > messageCounter) {
            List<Message> messages = chatServer.getLatestMessages(messageCounter);

            StringBuilder messagesString = new StringBuilder();

            for (Message message : messages) {
                List<User> recipients = message.getRecipients();
                if (recipients.contains(user)) {
                    messagesString.append("<b color=\"")
                            .append(message.getUser().getColor())
                            .append("\">")
                            .append(message.getUser().getName())
                            .append("</b>:\t")
                            .append("<u>")
                            .append(message.getContent())
                            .append("</u>");
                } else {
                    messagesString.append(message);
                }

                if (messages.indexOf(message) != messages.size() - 1) {
                    messagesString.append("\n");
                }
            }

            writer.println(messagesString);
            writer.flush();

            messageCounter = currentNumberOfMessages;
        }
    }

    public void writeCustomMessage(String message) {
        writer.println(message);
        writer.flush();
    }

    public void exit() {
        try {
            socket.close();
            writer.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        isRunning = false;
    }

    public User getUser() {
        return user;
    }
}
