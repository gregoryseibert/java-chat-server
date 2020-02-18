package com.company;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.safety.Whitelist;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ChatServer {
    private final Map<String, ICommand> commands;
    private final List<String> colorList;
    private List<Message> messages;
    private List<ClientHandler> clientHandlers;
    private CanteenMenuHandler canteenMenuHandler = null;
    private Whitelist whitelist;
    private int userCounter = 0;

    public ChatServer() {
        messages = new ArrayList<>();
        clientHandlers = new ArrayList<>();

        commands = new HashMap<>();
        commands.put("setname", this::commandSetName);
        commands.put("canteen", this::commandCanteen);
        commands.put("userlist", (ClientHandler clientHandler, String value) -> commandUserList(clientHandler));
        commands.put("help", (ClientHandler clientHandler, String value) -> commandHelp(clientHandler));
        commands.put("exit", (ClientHandler clientHandler, String value) -> commandExit(clientHandler));

        colorList = new ArrayList<>(
                Arrays.asList("red", "#a38a00", "olive", "maroon", "lime", "green", "teal", "navy", "fuchsia", "purple")
        );

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());

        String canteenURL = "http://boeblingen.eurest.de/assets/technologie-and-businesspark-boeblingen/restaurant-boeblingen/Microsite/Restaurant-ESS-36-Speiseplan-KW-";
        canteenURL += new DecimalFormat("00").format(cal.get(Calendar.WEEK_OF_YEAR)) + "-" + cal.get(Calendar.YEAR) + ".pdf";

        try {
            canteenMenuHandler = new CanteenMenuHandler(canteenURL);
        } catch (IOException e) {
            e.printStackTrace();
        }

        whitelist = Whitelist.none();
    }

    public void start() throws IOException {
        ServerSocket server = new ServerSocket(5555);

        InetAddress address = InetAddress.getLocalHost();
        System.out.println("Server started with IP: " + address.getHostAddress() + "\n");

        boolean isRunning = true;
        Socket client = null;

        while (isRunning) {
            try {
                boolean userWasAlreadyConnected = false;
                client = server.accept();

                String clientAddress;
                clientAddress = client.getInetAddress().getHostAddress();

                User user = new User("Anonym" + (userCounter + 1), clientAddress, colorList.get(userCounter % colorList.size()));

                for (ClientHandler ch : clientHandlers) {
                    if (!userWasAlreadyConnected && ch.getUser().getIpAddress().equals(clientAddress)) {
                        user = ch.getUser();
                        userWasAlreadyConnected = true;
                    }
                }

                ClientHandler clientHandler = new ClientHandler(this, client, commands, user, whitelist);
                clientHandler.start();
                clientHandlers.add(clientHandler);

                clientHandler.writeCustomMessage(getCurrentUsersString());
                clientHandler.writeCustomMessage("<b>Use the command \"\\help\" to get a list of all supported commands.</b>");

                if (userWasAlreadyConnected) {
                    System.out.println("Client (IP: '" + clientAddress + "'; HOSTNAME: '" + client.getInetAddress().getHostName() + "'; NAME: '" + clientHandler.getUser().getName() + "') has reentered this server.");
                    broadcastMessage("Client '" + clientHandler.getUser().getName() + "' has reentered this server.");
                } else {
                    userCounter++;
                    System.out.println("New client (IP: '" + clientAddress + "'; HOSTNAME: '" + client.getInetAddress().getHostName() + "'; NAME: '" + clientHandler.getUser().getName() + "') has been connected to this server.");
                    broadcastMessage("New client '" + clientHandler.getUser().getName() + "' has been connected to this server.");
                }
            } catch (Exception e) {
                isRunning = false;

                e.printStackTrace();

                if (client != null) {
                    client.close();
                }
            }
        }
    }

    public void commandCanteen(ClientHandler clientHandler, String value) {
        if (canteenMenuHandler != null) {
            System.out.println(value);

            if (value == null || value.length() == 0 || !value.matches("[1-5]")) {
                LocalDate date = LocalDate.now();
                DayOfWeek dayOfWeek = date.getDayOfWeek();
                List<String> menu = canteenMenuHandler.getMenuOfDay(dayOfWeek);
                clientHandler.writeCustomMessage("<b>Today's canteen menu: \n" + String.join("\n", menu) + "</b>");
            } else {
                DayOfWeek dayOfWeek = DayOfWeek.of(Integer.parseInt(value));
                List<String> menu = canteenMenuHandler.getMenuOfDay(dayOfWeek);
                clientHandler.writeCustomMessage("<b>" + StringUtils.capitalize(dayOfWeek.name().toLowerCase()) + "'s canteen menu: \n" + String.join("\n", menu) + "</b>");
            }


        } else {
            clientHandler.writeCustomMessage("Error while fetching canteen menu.");
        }
    }

    public void commandSetName(ClientHandler clientHandler, String newUsername) {
        if (newUsername == null) {
            clientHandler.writeCustomMessage("You haven't provided the username.");
        } else if (newUsername.length() > 12) {
            clientHandler.writeCustomMessage("Your wanted name is too long. Maximum 12 characters are allowed.");
        } else if (newUsername.length() < 3) {
            clientHandler.writeCustomMessage("Your wanted name is too short. Minimum 3 characters are needed.");
        } else if (!newUsername.matches("\\w+")) {
            clientHandler.writeCustomMessage("Your wanted name contains not allowed characters. Only letters and numbers are allowed.");
        } else if (isUsernameAlreadyInUse(newUsername)) {
            clientHandler.writeCustomMessage("Your wanted name is already in use.");
        } else {
            clientHandler.writeCustomMessage("You've successfully changed your name.");
            broadcastMessage("The user '" + clientHandler.getUser().getName() + "' changed his name to '" + newUsername + "'.");

            clientHandler.getUser().setName(newUsername);
        }
    }

    /*
    public void commandSetColor(ClientHandler clientHandler, String color) {
        if(color == null || !color.matches("^#(?:[0-9a-fA-F]{6})$")) {
            clientHandler.writeCustomMessage("Invalid color code. Format: #000000");
        } else {
            clientHandler.writeCustomMessage("You've successfully changed your color.");
            broadcastMessage("The user '" + clientHandler.getUser().getName() + "' changed his color to <span color=\"" + color + "\">'" + color + "'</span>.");

            clientHandler.getUser().setColor(color);
        }
    }*/

    public void commandUserList(ClientHandler clientHandler) {
        clientHandler.writeCustomMessage(getCurrentUsersString());
    }

    public void commandHelp(ClientHandler clientHandler) {
        clientHandler.writeCustomMessage("<b>Available commands: " + commands.keySet().toString() + "</b>");
    }

    public void commandExit(ClientHandler clientHandler) {
        System.out.println("Client '" + clientHandler.getUser().getName() + "' sends exit command.");
        System.out.println("Closing the connection with '" + clientHandler.getUser().getName() + "'.");
        broadcastMessage("The user '" + clientHandler.getUser().getName() + "' has exited.");

        clientHandler.exit();
    }

    public void broadcastMessage(String message) {
        System.out.println(message);

        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.isRunning()) {
                clientHandler.writeCustomMessage("<b>" + message + "</b>");
            }
        }
    }

    public String getCurrentUsersString() {
        return "<b>Currently connected: [" + clientHandlers.stream().filter(ClientHandler::isRunning).map(ch -> ch.getUser().getName()).collect(Collectors.joining(", ")) + "]</b>";
    }

    public User getUserByUsername(String username) {
        return clientHandlers.stream().filter(ClientHandler::isRunning).map(ClientHandler::getUser).filter(user -> user.getName().equals(username)).findFirst().orElse(null);
    }

    public List<Message> getLatestMessages(int startIndex) {
        return messages.subList(startIndex, messages.size());
    }

    public int getCurrentNumberOfMessages() {
        return messages.size();
    }

    public void addMessage(Message message) {
        messages.add(message);

        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.writeCurrentMessages();
        }
    }

    public boolean isUsernameAlreadyInUse(String username) {
        return clientHandlers.stream().map(c -> c.getUser().getName().toLowerCase()).anyMatch(c -> c.equals(username.toLowerCase()));
    }
}
