package com.company;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();

        try {
            chatServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
