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
    private final List<String> availableCommands;

    public ChatServer() {
        messages = new ArrayList<>();
        clientHandlers = new ArrayList<>();

        availableCommands = new ArrayList<>();
        availableCommands.add("\\setname");
        availableCommands.add("\\solve");
        availableCommands.add("\\help");
        availableCommands.add("\\exit");
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

                for(ClientHandler clientHandler: clientHandlers) {
                    clientHandler.writeCustomMessage("New client has been connected to this server.");
                }

                ClientHandler clientHandler = new ClientHandler(this, client, availableCommands);
                clientHandler.start();
                clientHandlers.add(clientHandler);

                String currentlyConnected = clientHandlers.stream().filter(ch -> !ch.getUser().getIpAddress().equals(clientHandler.getUser().getIpAddress())).map(ch -> ch.getUser().getName()).collect(Collectors.joining(", "));
                if(currentlyConnected.length() == 0) {
                    currentlyConnected = "---";
                }
                clientHandler.writeCustomMessage("Currently connected: " + currentlyConnected);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(client != null) {
                client.close();
            }
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

        for(ClientHandler clientHandler: clientHandlers) {
            clientHandler.writeCurrentMessages();
        }
    }

    public boolean isUsernameAlreadyInUse(String username) {
        return clientHandlers.stream().map(c -> c.user.getName()).anyMatch(c -> c.equals(username));
    }

    class ClientHandler extends Thread {
        private final ChatServer chatServer;

        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;

        private final User user;
        private final List<String> availableCommands;
        private final MathSolver mathSolver;

        private int messageCounter = 0;
        private boolean initialized = false;
        private boolean isRunning = false;

        public ClientHandler(ChatServer chatServer, Socket socket, List<String> availableCommands) throws IOException {
            this.chatServer = chatServer;
            this.socket = socket;
            this.availableCommands = availableCommands;

            user = new User("Anonym", socket.getInetAddress().getHostAddress());

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());

            mathSolver = new MathSolver();
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

                    if (received == null) {
                        continue;
                    }

                    System.out.println(user.getName() + ": " + received);

                    if (received.startsWith("\\")) {
                        if (received.equals("\\exit")) {
                            writer.println("You've been disconnected from the server.");
                            writer.flush();
                            commandExit();
                        } else if (received.startsWith("\\setname")) {
                            String newName = received.replace("\\setname ", "");

                            if (newName.length() > 10) {
                                writer.println("Your wanted name is too long. Maximum 10 characters are allowed");
                                writer.flush();
                            } else if (!newName.matches("\\w+")) {
                                writer.println("Your wanted name contains not allowed characters. Only letters and numbers are allowed.");
                                writer.flush();
                            } else if (chatServer.isUsernameAlreadyInUse(newName)) {
                                writer.println("Your wanted name is already in use.");
                                writer.flush();
                            } else {
                                writer.println("You've changed your name from '" + user.getName() + "' to '" + newName + "'.");
                                writer.flush();
                                commandSetName(newName);
                            }
                        } else if (received.startsWith("\\solve")) {
                            String equation = received.replace("\\solve", "");

                            if (equation.length() == 0) {
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
                        } else if (received.equals("\\help")) {
                            writer.println("Available commands: " + availableCommands + ".");
                            writer.flush();
                        } else {
                            writer.println("Invalid command.");
                            writer.flush();
                        }
                    } else if(received.length() > 100) {
                        writer.println("Your message is too long. Maximum of 100 characters is allowed.");
                        writer.flush();
                    } else if(received.length() > 0){
                        Message message = new Message(user, received);
                        chatServer.addMessage(message);
                    }
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
                socket.close();
                writer.close();
                reader.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        public boolean isRunning() {
            return isRunning;
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

        private void writeCustomMessage(String message) {
            writer.println(message);
            writer.flush();
        }

        private void commandExit() throws IOException {
            System.out.println("Client '" + user.getName() + "' sends exit command.");
            System.out.println("Closing the connection with '" + user.getName() + "'.");

            socket.close();
            writer.close();
            reader.close();

            isRunning = false;
        }

        private void commandSetName(String name) {
            System.out.println("Client '" + user.getName() + "' changed his name to '" + name + "'.");

            user.setName(name);
        }

        public User getUser() {
            return user;
        }
    }
}
