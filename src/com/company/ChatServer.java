package com.company;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatServer {
    private List<Message> messages;
    private List<ClientHandler> clientHandlers;
    private final List<String> availableCommands;
    private final List<String> colorList;
    private Whitelist whitelist;
    private int userCounter = 0;

    public ChatServer() {
        messages = new ArrayList<>();
        clientHandlers = new ArrayList<>();

        availableCommands = new ArrayList<>();
        availableCommands.add("\\setname");
        availableCommands.add("\\setcolor");
        availableCommands.add("\\userlist");
        availableCommands.add("\\help");
        availableCommands.add("\\exit");

        colorList = new ArrayList<>();
        colorList.add("red");
        colorList.add("gold");
        colorList.add("olive");
        colorList.add("maroon");
        colorList.add("lime");
        colorList.add("green");
        colorList.add("teal");
        colorList.add("navy");
        colorList.add("fuchsia");
        colorList.add("purple");

        whitelist = Whitelist.none();
    }

    public void start() throws IOException {
        ServerSocket server = new ServerSocket(5555);

        InetAddress address = InetAddress.getLocalHost();
        System.out.println("Server started with IP: " + address.getHostAddress() + "\n");

        Socket client = null;
        try {
            while(true) {
                boolean userWasAlreadyConnected = false;
                client = server.accept();

                String clientAddress;
                clientAddress = client.getInetAddress().getHostAddress();

                User user = new User("Anonym" + (userCounter + 1), clientAddress, colorList.get(userCounter % colorList.size()));

                for(ClientHandler ch: clientHandlers) {
                    if(!userWasAlreadyConnected && ch.getUser().getIpAddress().equals(clientAddress)) {
                        user = ch.getUser();
                        userWasAlreadyConnected = true;
                    }
                }

                ClientHandler clientHandler = new ClientHandler(this, client, availableCommands, user);
                clientHandler.start();
                clientHandlers.add(clientHandler);

                clientHandler.writeCustomMessage(getCurrentUsersString());
                clientHandler.writeCustomMessage("<b>Use the command \"\\help\" to get a list of all supported commands.</b>");

                if(userWasAlreadyConnected) {
                    System.out.println("Client (IP: '" + clientAddress + "'; HOSTNAME: '" + client.getInetAddress().getHostName() + "'; NAME: '" + clientHandler.getUser().getName() + "') has reentered this server.");
                    broadcastMessage("Client '" + clientHandler.getUser().getName() + "' has reentered this server.");
                } else {
                    userCounter++;
                    System.out.println("New client (IP: '" + clientAddress + "'; NAME: '" + clientHandler.getUser().getName() + "') has been connected to this server.");
                    broadcastMessage("New client '" + clientHandler.getUser().getName() + "' has been connected to this server.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(client != null) {
                client.close();
            }
        }
    }

    public void broadcastMessage(String message) {
        for(ClientHandler clientHandler: clientHandlers) {
            if(clientHandler.isRunning()) {
                clientHandler.writeCustomMessage("<b>" + message + "</b>");
            }
        }
    }

    public String getCurrentUsersString() {
        return "<b>Currently connected: [" + clientHandlers.stream().filter(ClientHandler::isRunning).map(ch -> ch.getUser().getName()).collect(Collectors.joining(", ")) + "]</b>";
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
        return clientHandlers.stream().map(c -> c.user.getName().toLowerCase()).anyMatch(c -> c.equals(username.toLowerCase()));
    }

    class ClientHandler extends Thread {
        private final ChatServer chatServer;

        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;

        private final User user;
        private final List<String> availableCommands;

        private int messageCounter = 0;
        private boolean initialized = false;
        private boolean isRunning = false;

        public ClientHandler(ChatServer chatServer, Socket socket, List<String> availableCommands, User user) throws IOException {
            this.chatServer = chatServer;
            this.socket = socket;
            this.availableCommands = availableCommands;
            this.user = user;

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

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

                            if (newName.length() > 12) {
                                writer.println("Your wanted name is too long. Maximum 12 characters are allowed");
                                writer.flush();
                            } else if (!newName.matches("\\w+")) {
                                writer.println("Your wanted name contains not allowed characters. Only letters and numbers are allowed.");
                                writer.flush();
                            } else if (chatServer.isUsernameAlreadyInUse(newName)) {
                                writer.println("Your wanted name is already in use.");
                                writer.flush();
                            } else {
                                writer.println("You've successfully changed your name.");
                                writer.flush();
                                chatServer.broadcastMessage("The user '" + user.getName() + "' changed his name to '" + newName + "'.");
                                commandSetName(newName);
                            }
                        } else if(received.startsWith("\\setcolor")) {
                            String color = received.replace("\\setcolor ", "");

                            if(!color.matches("^#([A-Fa-f0-9]{6})$")) {
                                writer.println("The provided color is invalid. Example (hex): #A0A0A0.");
                                writer.flush();
                            } else {
                                user.setColor(color);
                            }
                        } else if (received.startsWith("\\userlist")) {
                            writer.println(chatServer.getCurrentUsersString());
                            writer.flush();
                        } else if (received.equals("\\help")) {
                            writer.println("Available commands: " + availableCommands + ".");
                            writer.flush();
                        } else {
                            writer.println("Invalid command.");
                            writer.flush();
                        }
                    } else if(received.length() > 250) {
                        writer.println("Your message is too long. Maximum of 250 characters is allowed.");
                        writer.flush();
                    } else if(received.length() > 0){
                        String cleanContent = Jsoup.clean(received, whitelist);
                        Message message = new Message(user, cleanContent);
                        chatServer.addMessage(message);
                    }
                } catch (IOException e) {
                    isRunning = false;

                    switch(e.getMessage()) {
                        case "Connection reset":
                            break;
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

            chatServer.broadcastMessage("The user '" + user.getName() + "' has exited.");

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
