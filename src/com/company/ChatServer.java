package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatServer {
    private List<Message> messages;
    private List<ClientHandler> clientHandlers;

    public ChatServer() {
        messages = new ArrayList<>();
        clientHandlers = new ArrayList<>();
    }

    public void start() throws IOException {
        ServerSocket server = new ServerSocket(5555);

        InetAddress address = InetAddress.getLocalHost();
        System.out.println("Server started with IP: " + address.getHostAddress() + "\n");

        Socket client = null;
        try {
            while(true) {
                client = server.accept();

                String clientAddress;
                clientAddress = client.getInetAddress().getHostAddress();
                System.out.println("New client '" + clientAddress + "' has been connected to this server.");

                ClientHandler clientHandler = new ClientHandler(this, client);
                clientHandler.start();
                clientHandlers.add(clientHandler);
            }
        } catch (Exception e) {
            client.close();
            e.printStackTrace();
        }
    }

    public List<Message> getLatestMessages(int startIndex) {
        return messages.subList(startIndex, messages.size());
    }

    public int getCurrentNumberOfMessages() {
        return messages.size();
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    public boolean isUsernameAlreadyInUse(String username) {
        return clientHandlers.stream().map(c -> c.user.getName()).anyMatch(c -> c.equals(username));
    }

    class ClientHandler extends Thread {
        private final ChatServer chatServer;
        private final User user;
        private final BufferedReader reader;
        private final PrintWriter writer;
        private final Socket socket;
        private final List<String> availableCommands;

        private final MathSolver mathSolver;

        private int messageCounter = 0;
        private boolean initialized = false;

        public ClientHandler(ChatServer chatServer, Socket socket) throws IOException {
            this.chatServer = chatServer;
            this.socket = socket;

            user = new User(socket.getInetAddress().getHostAddress(), socket.getInetAddress().getHostAddress());

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());

            availableCommands = new ArrayList<>();
            availableCommands.add("\\setname");
            availableCommands.add("\\solve");
            availableCommands.add("\\help");
            availableCommands.add("\\exit");

            mathSolver = new MathSolver();
        }

        @Override
        public void run() {
            String received;
            boolean isRunning = true;

            while (isRunning) {
                try {
                    if(!initialized) {
                        writeCurrentMessages();

                        initialized = true;
                    }

                    received = reader.readLine();

                    System.out.println(user.getName() + ": " + received);

                    if (received.startsWith("\\")) {
                        if(received.equals("\\exit")) {
                            writer.println("You've been disconnected from the server.");
                            writer.flush();
                            commandExit();
                        } else if(received.startsWith("\\setname")) {
                            String newName = received.replace("\\setname ", "");

                            if(newName.length() > 10) {
                                writer.println("Your wanted name is too long. Maximum 10 characters are allowed");
                                writer.flush();
                            } else if(!newName.matches("\\w+")) {
                                writer.println("Your wanted name contains not allowed characters. Only letters and numbers are allowed.");
                                writer.flush();
                            } else if(chatServer.isUsernameAlreadyInUse(newName)) {
                                writer.println("Your wanted name is already in use.");
                                writer.flush();
                            } else {
                                writer.println("You've changed your name from '" + user.getName() + "' to '" + newName + "'.");
                                writer.flush();
                                commandSetName(newName);
                            }
                        } else if(received.startsWith("\\solve")) {
                            String equation = received.replace("\\solve", "");

                            if(equation.length() == 0) {
                                writer.println("You haven't provided the equation.");
                                writer.flush();
                            } else {
                                try {
                                    double result = mathSolver.input(equation);
                                    writer.println("Result is '" + result + "'.");
                                    writer.flush();
                                } catch (NullPointerException e) {
                                    writer.println("Your equation is invalid.");
                                    writer.flush();
                                }
                            }
                        } else if(received.equals("\\help")) {
                            writer.println("Available commands: " + availableCommands + ".");
                            writer.flush();
                        } else {
                            writer.println("Invalid command.");
                            writer.flush();
                        }
                    } else {
                        Message message = new Message(user, received);
                        chatServer.addMessage(message);
                        messageCounter++;
                    }

                    writeCurrentMessages();
                } catch (IOException e) {
                    isRunning = false;

                    switch(e.getMessage()) {
                        case "Connection reset":
                            System.out.println("Client '" + user.getName() + "' has disconnected from the server.");
                            break;
                        case "socket closed":
                            System.out.println("Client '" + user.getName() + "' has been disconnected from the server.");
                            break;
                        default:
                            e.printStackTrace();
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                this.writer.close();
                this.reader.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        private void writeCurrentMessages() {
            int currentNumberOfMessages = chatServer.getCurrentNumberOfMessages();
            if(currentNumberOfMessages > messageCounter) {
                List<Message> messages = chatServer.getLatestMessages(messageCounter);

                String messagesString = messages.stream().map(Object::toString).collect(Collectors.joining("\n"));

                writer.println(messagesString);
                writer.flush();

                messageCounter = currentNumberOfMessages;
            }
        }

        private void commandExit() throws IOException {
            System.out.println("Client " + this.socket + " sends exit...");
            System.out.println("Closing this connection.");
            this.socket.close();
            System.out.println("Connection closed");
        }

        private void commandSetName(String name) {
            System.out.println("Client '" + user.getName() + "' changed his name to '" + name + "'.");

            user.setName(name);
        }
    }
}
