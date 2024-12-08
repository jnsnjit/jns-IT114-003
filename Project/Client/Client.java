package Project.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Project.Client.Interfaces.IClientEvents;
import Project.Client.Interfaces.IConnectionEvents;
import Project.Client.Interfaces.ICooldownEvent;
import Project.Client.Interfaces.IMessageEvents;
import Project.Client.Interfaces.IPhaseEvent;
import Project.Client.Interfaces.IPointsEvent;
import Project.Client.Interfaces.IReadyEvent;
import Project.Client.Interfaces.IRoomEvents;
import Project.Client.Interfaces.IAwayEvent;
import Project.Client.Interfaces.IBoardEvents;
import Project.Client.Interfaces.ITimeEvents;
import Project.Client.Interfaces.ITurnEvent;
import Project.Common.ConnectionPayload;
import Project.Common.Constants;
import Project.Common.CooldownPayload;
import Project.Common.LeaderboardPayload;
import Project.Common.LeaderboardRecord;
import Project.Common.LoggerUtil;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Common.Phase;
import Project.Common.PointsPayload;
import Project.Common.ReadyPayload;
import Project.Common.RoomResultsPayload;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;
import Project.Common.TimerPayload;
import Project.Common.TimerType;
import Project.Common.AwayPayload;
import Project.Common.ChoicePayload;

/**
 * Demoing bi-directional communication between client and server in a
 * multi-client scenario
 */
public enum Client {
    INSTANCE;

    {
        // statically initialize the client-side LoggerUtil
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024); // 2MB
        config.setFileCount(1);
        config.setLogLocation("client.log");
        // Set the logger configuration
        LoggerUtil.INSTANCE.setConfig(config);
    }
    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");
    private volatile boolean isRunning = true; // volatile for thread-safe visibility
    private ConcurrentHashMap<Long, ClientPlayer> knownClients = new ConcurrentHashMap<>();
    private ClientPlayer myData;
    private Phase currentPhase = Phase.READY;
    public boolean cooldown = false;
    // constants (used to reduce potential types when using them in code)
    private final String COMMAND_CHARACTER = "/";
    private final String CREATE_ROOM = "createroom";
    private final String JOIN_ROOM = "joinroom";
    private final String LIST_ROOMS = "listrooms";
    private final String DISCONNECT = "disconnect";
    private final String LOGOFF = "logoff";
    private final String LOGOUT = "logout";
    private final String SINGLE_SPACE = " ";
    // other constants
    private final String READY = "ready";
    private final String RPS = "rps";
    private final String AWAY = "away";
    private final String COOLDOWN = "cooldown";
    //callback that updates the UI
    private static List<IClientEvents> events = new ArrayList<IClientEvents>();

    public void addCallback(IClientEvents e) {
        events.add(e);
    }
    // needs to be private now that the enum logic is handling this
    private Client() {
        LoggerUtil.INSTANCE.info("Client Created");
        myData = new ClientPlayer();
    }

    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine if the server had a problem
        // and is just for lesson's sake
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    /**
     * Takes an IP address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @return true if connection was successful
     */
    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            LoggerUtil.INSTANCE.info("Client connected");
            // Use CompletableFuture to run listenToServer() in a separate thread
            CompletableFuture.runAsync(this::listenToServer);
        } catch (UnknownHostException e) {
            LoggerUtil.INSTANCE.warning("Unknown host", e);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("IOException", e);
        }
        return isConnected();
    }

    /**
     * Takes an ip address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @param username
     * @param callback (for triggering UI events)
     * @return true if connection was successful
     */
    public boolean connect(String address, int port, String username, IClientEvents callback) {
        myData.setClientName(username);
        addCallback(callback);
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            LoggerUtil.INSTANCE.info("Client connected");
            // Use CompletableFuture to run listenToServer() in a separate thread
            CompletableFuture.runAsync(this::listenToServer);
            sendClientName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }
    /**
     * <p>
     * Check if the string contains the <i>connect</i> command
     * followed by an IP address and port or localhost and port.
     * </p>
     * <p>
     * Example format: 123.123.123.123:3000
     * </p>
     * <p>
     * Example format: localhost:3000
     * </p>
     * https://www.w3schools.com/java/java_regex.asp
     * 
     * @param text
     * @return true if the text is a valid connection command
     */
    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    /**
     * Controller for handling various text commands.
     * <p>
     * Add more here as needed
     * </p>
     * 
     * @param text
     * @return true if the text was a command or triggered a command
     */
    private boolean processClientCommand(String text) throws IOException {
        if (isConnection(text)) {
            if (myData.getClientName() == null || myData.getClientName().length() == 0) {
                System.out.println(TextFX.colorize("Name must be set first via /name command", Color.RED));
                return true;
            }
            // replaces multiple spaces with a single space
            // splits on the space after connect (gives us host and port)
            // splits on : to get host as index 0 and port as index 1
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            sendClientName();
            return true;
        } else if ("/quit".equalsIgnoreCase(text)) {
            close();
            return true;
        } else if (text.startsWith("/name")) {
            myData.setClientName(text.replace("/name", "").trim());
            System.out.println(TextFX.colorize("Set client name to " + myData.getClientName(), Color.CYAN));
            return true;
        } else if (text.equalsIgnoreCase("/users")) {
            // chatroom version
            /*
             * System.out.println(
             * String.join("\n", knownClients.values().stream()
             * .map(c -> String.format("%s(%s)", c.getClientName(),
             * c.getClientId())).toList()));
             */
            // non-chatroom version
            System.out.println(
                    String.join("\n", knownClients.values().stream()
                            .map(c -> String.format("%s(%s) %s", c.getClientName(), c.getClientId(),
                                    c.isReady() ? "[x]" : "[ ]"))
                            .toList()));
            return true;
        } else { // logic previously from Room.java
            // decided to make this as separate block to separate the core client-side items
            // vs the ones that generally are used after connection and that send requests
            if (text.startsWith(COMMAND_CHARACTER)) {
                boolean wasCommand = false;
                String fullCommand = text.replace(COMMAND_CHARACTER, "");
                String part1 = fullCommand;
                String[] commandParts = part1.split(SINGLE_SPACE, 2);// using limit so spaces in the command value
                                                                     // aren't split
                final String command = commandParts[0];
                final String commandValue = commandParts.length >= 2 ? commandParts[1] : "";
                switch (command) {
                    case CREATE_ROOM:
                        sendCreateRoom(commandValue);
                        wasCommand = true;
                        break;
                    case JOIN_ROOM:
                        sendJoinRoom(commandValue);
                        wasCommand = true;
                        break;
                    case LIST_ROOMS:
                        sendListRooms(commandValue);
                        wasCommand = true;
                        break;
                    // Note: these are to disconnect, they're not for changing rooms
                    case DISCONNECT:
                    case LOGOFF:
                    case LOGOUT:
                        sendDisconnect();
                        wasCommand = true;
                        break;
                    // others
                    case READY:
                        sendReady();
                        wasCommand = true;
                        break;
                    case RPS:
                        sendChoice(commandValue);
                        wasCommand = true;
                        break;
                    //milestone4, new case, client wants to go away
                    case AWAY:
                        sendAway();
                        wasCommand = true;
                        break;
                    case COOLDOWN:
                        sendCooldownModifier();
                        wasCommand = true;
                        break;
                }
                return wasCommand;
            }
        }
        return false;
    }
    public long getMyClientId() {
        return myData.getClientId();
    }

    public void clientSideGameEvent(String str) {
        events.forEach(event -> {
            if (event instanceof IMessageEvents) {
                // Note: using -2 to target GameEventPanel
                ((IMessageEvents) event).onMessageReceive(Constants.GAME_EVENT_CHANNEL, str);
            }
        });
    }
    //milestone4, cooldown payload
    public void sendCooldownModifier(){
        CooldownPayload clp = new CooldownPayload();
        send(clp);
    }
    // send methods to pass data to the ServerThread
    /**
     * Sends the client's intent to be ready.
     * Can also be used to toggle the ready state if coded on the server-side
     */
    public void sendReady() {
        ReadyPayload rp = new ReadyPayload();
        rp.setReady(true); // <- techically not needed as we'll use the payload type as a trigger
        send(rp);
    }
    public void sendAway(){
        AwayPayload ap = new AwayPayload();
        ap.setAway(true);
        send(ap);
    }
    public void sendChoice(String choice){
        String choicef = choice.toLowerCase();
        ChoicePayload rps = new ChoicePayload();
        if(choicef.equals("rock") || choicef.equals("paper") || choicef.equals("scissors")){
            rps.setChoice(choicef);
            send(rps);
        }else{
            System.out.println("Choice must either be rock, paper, or scissors. Only works in active non-lobby rooms");
        }
    }
    /**
     * Sends a search to the server-side to get a list of potentially matching Rooms
     * 
     * @param roomQuery optional partial match search String
     */
    public void sendListRooms(String roomQuery) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_LIST);
        p.setMessage(roomQuery);
        send(p);
    }

    /**
     * Sends the room name we intend to create
     * 
     * @param room
     */
    public void sendCreateRoom(String room) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_CREATE);
        p.setMessage(room);
        send(p);
    }

    /**
     * Sends the room name we intend to join
     * 
     * @param room
     */
    public void sendJoinRoom(String room) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_JOIN);
        p.setMessage(room);
        send(p);
    }

    /**
     * Tells the server-side we want to disconnect
     */
    void sendDisconnect() throws IOException{
        Payload p = new Payload();
        p.setPayloadType(PayloadType.DISCONNECT);
        send(p);
    }

    /**
     * Sends desired message over the socket
     * 
     * @param message
     */
    public void sendMessage(String message) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setMessage(message);
        send(p);
    }

    /**
     * Sends chosen client name after socket handshake
     */
    private void sendClientName() {
        if (myData.getClientName() == null || myData.getClientName().length() == 0) {
            System.out.println(TextFX.colorize("Name must be set first via /name command", Color.RED));
            return;
        }
        ConnectionPayload cp = new ConnectionPayload();
        cp.setClientName(myData.getClientName());
        send(cp);
    }

    /**
     * Generic send that passes any Payload over the socket (to ServerThread)
     * 
     * @param p
     */
    private void send(Payload p) {
        try {
            out.writeObject(p);
            out.flush();
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Socket send exception", e);
        }

    }
    // end send methods

    public void start() throws IOException {
        LoggerUtil.INSTANCE.info("Client starting");

        // Use CompletableFuture to run listenToInput() in a separate thread
        CompletableFuture<Void> inputFuture = CompletableFuture.runAsync(this::listenToInput);

        // Wait for inputFuture to complete to ensure proper termination
        inputFuture.join();
    }

    /**
     * Listens for messages from the server
     */
    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                Payload fromServer = (Payload) in.readObject(); // blocking read
                if (fromServer != null) {
                    // System.out.println(fromServer);
                    processPayload(fromServer);
                } else {
                    LoggerUtil.INSTANCE.info("Server disconnected");
                    break;
                }
            }
        } catch (ClassCastException | ClassNotFoundException cce) {
            LoggerUtil.INSTANCE.severe("Error reading object as specified type: ", cce);
        } catch (IOException e) {
            if (isRunning) {
                LoggerUtil.INSTANCE.info("Connection dropped", e);
            }
        } finally {
            closeServerConnection();
        }
        LoggerUtil.INSTANCE.info("listenToServer thread stopped");
    }

        /**
     * Listens for keyboard input from the user
     */
    @Deprecated
    private void listenToInput() {
        try (Scanner si = new Scanner(System.in)) {
            System.out.println("Waiting for input"); // moved here to avoid console spam
            while (isRunning) { // Run until isRunning is false
                String line = si.nextLine();
                LoggerUtil.INSTANCE.severe(
                        "You shouldn't be using terminal input for Milestone 3. Interaction should be done through the UI");
                if (!processClientCommand(line)) {
                    if (isConnected()) {
                        sendMessage(line);
                    } else {
                        System.out.println(
                                "Not connected to server (hint: type `/connect host:port` without the quotes and replace host/port with the necessary info)");
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Error in listentToInput()", e);
        }
        LoggerUtil.INSTANCE.info("listenToInput thread stopped");
    }

    /**
     * Closes the client connection and associated resources
     */
    private void close() {
        isRunning = false;
        closeServerConnection();
        LoggerUtil.INSTANCE.info("Client terminated");
        // System.exit(0); // Terminate the application
    }

    /**
     * Closes the server connection and associated resources
     */
    private void closeServerConnection() {
        myData.reset();
        knownClients.clear();
        try {
            if (out != null) {
                LoggerUtil.INSTANCE.info("Closing output stream");
                out.close();
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.info("Error closing output stream", e);
        }
        try {
            if (in != null) {
                LoggerUtil.INSTANCE.info("Closing input stream");
                in.close();
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.info("Error closing input stream", e);
        }
        try {
            if (server != null) {
                LoggerUtil.INSTANCE.info("Closing connection");
                server.close();
                LoggerUtil.INSTANCE.info("Closed socket");
            }
        } catch (IOException e) {
            LoggerUtil.INSTANCE.info("Error closing socket", e);
        }
    }

    public static void main(String[] args) {
        Client client = Client.INSTANCE;
        try {
            client.start();
        } catch (IOException e) {
            LoggerUtil.INSTANCE.info("Exception from main()", e);
        }
    }

    /**
     * Handles received message from the ServerThread
     * 
     * @param payload
     */
    private void processPayload(Payload payload) {
        try {
            LoggerUtil.INSTANCE.info("Received Payload: " + payload);
            switch (payload.getPayloadType()) {
                case PayloadType.CLIENT_ID: // get id assigned
                    ConnectionPayload cp = (ConnectionPayload) payload;
                    processClientData(cp.getClientId(), cp.getClientName());
                    break;
                case PayloadType.SYNC_CLIENT: // silent add
                    cp = (ConnectionPayload) payload;
                    processClientSync(cp.getClientId(), cp.getClientName());
                    break;
                case PayloadType.DISCONNECT: // remove a disconnected client (mostly for the specific message vs leaving
                                             // a room)
                    cp = (ConnectionPayload) payload;
                    processDisconnect(cp.getClientId(), cp.getClientName());
                    // note: we want this to cascade
                case PayloadType.ROOM_JOIN: // add/remove client info from known clients
                    cp = (ConnectionPayload) payload;
                    processRoomAction(cp.getClientId(), cp.getClientName(), cp.getMessage(), cp.isConnect());
                    break;
                case PayloadType.ROOM_LIST:
                    RoomResultsPayload rrp = (RoomResultsPayload) payload;
                    processRoomsList(rrp.getRooms(), rrp.getMessage());
                    break;
                case PayloadType.MESSAGE: // displays a received message
                    processMessage(payload.getClientId(), payload.getMessage());
                    break;
                case PayloadType.READY:
                    ReadyPayload rp = (ReadyPayload) payload;
                    processReadyStatus(rp.getClientId(), rp.isReady(), false);
                    break;
                case PayloadType.SYNC_READY:
                    ReadyPayload qrp = (ReadyPayload)payload;
                    processReadyStatus(qrp.getClientId(), qrp.isReady(), true);
                    break;
                case PayloadType.RESET_READY:
                    // note no data necessary as this is just a trigger
                    processResetReady();
                    break;
                case PayloadType.PHASE:
                    processPhase(payload.getMessage());
                    break;
                case PayloadType.LEADERBOARD:
                    LeaderboardPayload lp = (LeaderboardPayload) payload;
                    processLeaderboard(lp.getLeaderboard());
                    break;
                //milestone4 away processing
                case PayloadType.AWAY:
                    AwayPayload ap = (AwayPayload) payload;
                    processAwayStatus(ap.getClientId(), ap.getAway(), false);
                    break;
                case PayloadType.COOLDOWN:
                    CooldownPayload clp = (CooldownPayload) payload;
                    processCooldown(clp.getClientId(),clp.getCooldown(),false);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Could not process Payload: " + payload, e);
        }
    }

    /**
     * Returns the ClientName of a specific Client by ID.
     * 
     * @param id
     * @return the name, or Room if id is -1, or [Unknown] if failed to find
     */
    public String getClientNameFromId(long id) {
        if (id == ClientPlayer.DEFAULT_CLIENT_ID) {
            return "Room";
        }
        if (knownClients.containsKey(id)) {
            return knownClients.get(id).getClientName();
        }
        return "[Unknown]";
    }
    // payload processors
    private void processLeaderboard(List<LeaderboardRecord> ldr){
        events.forEach(event ->{
            if(event instanceof IBoardEvents){
                ((IBoardEvents) event).onRecieveLeaderboard(ldr);
            }
        });
    }
    private void processCooldown(long clientId, boolean cooldown, boolean quiet){
        //client now knows what gameroom has for cooldown status
        this.cooldown = cooldown;
        if (!knownClients.containsKey(clientId)) {
            LoggerUtil.INSTANCE.severe(String.format("Received cooldown status [%s] for client id %s who is not known"));
            return;
        }
        ClientPlayer cp = knownClients.get(clientId);
        if (!quiet) {
            System.out.println(String.format("%s[%s]: Gameroom has %s ten second choice cooldowns", cp.getClientName(), cp.getClientId(),
                    cooldown ? "enabled" : "disabled"));
        }
        events.forEach(event -> {
            if (event instanceof ICooldownEvent) {
                ((ICooldownEvent) event).onReciveeCooldown(clientId, cooldown, quiet);
            }
        });
    }
    private void processAwayStatus(long clientId, boolean isAway, boolean quiet){
        if (!knownClients.containsKey(clientId)) {
            LoggerUtil.INSTANCE.severe(String.format("Received away status [%s] for client id %s who is not known",
                    isAway ? "away" : "not away", clientId));
            return;
        }
        ClientPlayer cp = knownClients.get(clientId);
        cp.setAway(isAway);
        if (!quiet) {
            System.out.println(String.format("%s[%s] is %s", cp.getClientName(), cp.getClientId(),
                    isAway ? "away" : "not away"));
        }
        events.forEach(event -> {
            if (event instanceof IAwayEvent) {
                ((IAwayEvent) event).onRecieveAway(clientId, isAway, quiet);
            }
        });
    }
    private void processPoints(long clientId, int points) {
        if (clientId == ClientPlayer.DEFAULT_CLIENT_ID) {
            knownClients.values().forEach(cp -> cp.addPoint());
        }
        else{
            knownClients.get(clientId).addPoint();
        }
        events.forEach(event -> {
            if (event instanceof IPointsEvent) {
                ((IPointsEvent) event).onPointsUpdate(clientId, points);
            }
        });
    }

    private void processCurrentTimer(TimerType timerType, int time) {
        events.forEach(event -> {
            if (event instanceof ITimeEvents) {
                ((ITimeEvents) event).onTimerUpdate(timerType, time);
            }
        });
    }
    private void processPhase(String phase) {
        currentPhase = Enum.valueOf(Phase.class, phase);
        System.out.println(TextFX.colorize("Current phase is " + currentPhase.name(), Color.YELLOW));
        events.forEach(event -> {
            if (event instanceof IPhaseEvent) {
                ((IPhaseEvent) event).onReceivePhase(currentPhase);
            }
        });
    }
    private void processResetReady() {
        knownClients.values().forEach(cp -> cp.setReady(false));
        System.out.println("Ready status reset for everyone");
    }
    private void processReadyStatus(long clientId, boolean isReady, boolean quiet) {
        if (!knownClients.containsKey(clientId)) {
            LoggerUtil.INSTANCE.severe(String.format("Received ready status [%s] for client id %s who is not known",
                    isReady ? "ready" : "not ready", clientId));
            return;
        }
        ClientPlayer cp = knownClients.get(clientId);
        cp.setReady(isReady);
        if (!quiet) {
            System.out.println(String.format("%s[%s] is %s", cp.getClientName(), cp.getClientId(),
                    isReady ? "ready" : "not ready"));
        }
        events.forEach(event -> {
            if (event instanceof IReadyEvent) {
                ((IReadyEvent) event).onReceiveReady(clientId, isReady, quiet);
            }
        });
    }

    private void processRoomsList(List<String> rooms, String message) {
        // invoke onReceiveRoomList callback
        events.forEach(event -> {
            if (event instanceof IRoomEvents) {
                ((IRoomEvents) event).onReceiveRoomList(rooms, message);
            }
        });

        if (rooms == null || rooms.size() == 0) {
            System.out.println(TextFX.colorize("No rooms found matching your query", Color.RED));
            return;
        }
        System.out.println(TextFX.colorize("Room Results:", Color.PURPLE));
        System.out.println(String.join("\n", rooms));

    }

    private void processDisconnect(long clientId, String clientName) {
        // invoke onClientDisconnect callback
        events.forEach(event -> {
            if (event instanceof IConnectionEvents) {
                ((IConnectionEvents) event).onClientDisconnect(clientId, clientName);
            }
        });
        System.out.println(TextFX.colorize(
                String.format("*%s disconnected*", clientId == myData.getClientId() ? "You" : clientName), Color.RED));
        if (clientId == myData.getClientId()) {
            closeServerConnection();
        }
    }

    private void processClientData(long clientId, String clientName) {
        if (myData.getClientId() == ClientPlayer.DEFAULT_CLIENT_ID) {
            myData.setClientId(clientId);
            myData.setClientName(clientName);
            // invoke onReceiveClientId callback
            events.forEach(event -> {
                if (event instanceof IConnectionEvents) {
                    ((IConnectionEvents) event).onReceiveClientId(clientId);
                }
            });
            // knownClients.put(cp.getClientId(), myData);// <-- this is handled later
        }
    }

    private void processMessage(long clientId, String message) {
        String name = knownClients.containsKey(clientId) ? knownClients.get(clientId).getClientName() : "Room";
        System.out.println(TextFX.colorize(String.format("%s: %s", name, message), Color.BLUE));
        // invoke onMessageReceive callback
        events.forEach(event -> {
            if (event instanceof IMessageEvents) {
                ((IMessageEvents) event).onMessageReceive(clientId, message);
            }
        });
    }

    private void processClientSync(long clientId, String clientName) {

        if (!knownClients.containsKey(clientId)) {
            ClientPlayer cd = new ClientPlayer();
            cd.setClientId(clientId);
            cd.setClientName(clientName);
            knownClients.put(clientId, cd);
            // invoke onSyncClient callback
            events.forEach(event -> {
                if (event instanceof IConnectionEvents) {
                    ((IConnectionEvents) event).onSyncClient(clientId, clientName);
                }
            });
        }
    }

    private void processRoomAction(long clientId, String clientName, String message, boolean isJoin) {

        if (isJoin && !knownClients.containsKey(clientId)) {
            ClientPlayer cd = new ClientPlayer();
            cd.setClientId(clientId);
            cd.setClientName(clientName);
            knownClients.put(clientId, cd);
            System.out.println(TextFX.colorize(
                    String.format("*%s[%s] joined the Room %s*", clientName, clientId, message), Color.GREEN));
            // invoke onRoomJoin callback
            events.forEach(event -> {
                if (event instanceof IRoomEvents) {
                    ((IRoomEvents) event).onRoomAction(clientId, clientName, message, isJoin);
                }
            });
        } else if (!isJoin) {
            ClientPlayer removed = knownClients.remove(clientId);
            if (removed != null) {
                System.out.println(TextFX.colorize(
                        String.format("*%s[%s] left the Room %s*", clientName, clientId, message), Color.YELLOW));
                // invoke onRoomJoin callback
                events.forEach(event -> {
                    if (event instanceof IRoomEvents) {
                        ((IRoomEvents) event).onRoomAction(clientId, clientName, message, isJoin);
                    }
                });
            }
            // clear our list
            if (clientId == myData.getClientId()) {
                knownClients.clear();
                // invoke onResetUserList()
                events.forEach(event -> {
                    if (event instanceof IConnectionEvents) {
                        ((IConnectionEvents) event).onResetUserList();
                    }
                });
            }
        }
    }
    // end payload processors
}