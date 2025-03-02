package Project.Common;

public enum PayloadType {
    CLIENT_CONNECT, // client requesting to connect to server (passing of initialization data [name])
    CLIENT_ID,  // server sending client id
    SYNC_CLIENT,  // silent syncing of clients in room
    DISCONNECT,  // distinct disconnect action
    ROOM_CREATE,
    ROOM_JOIN, // join/leave room based on boolean
    ROOM_SJOIN, // milestone4, thread will attempt to join as a spectator
    MESSAGE, // sender and message,
    ROOM_LIST, // client: query for rooms, server: result of query,
    READY, // client to trigger themselves as ready, server to sync the related status of a particular client
    SYNC_READY, // quiet version of READY, used to sync existing ready status of clients in a GameRoom
    RESET_READY, // trigger to tell the client to reset their whole local list's ready status (saves network requests)
    PHASE, // syncs current phase of session (used as a switch to only allow certain logic to execute)
    CHOICE, // player choice
    TIME, // sync time
    POINTS, // syncs player points
    LEADERBOARD, //send leaderboard information
    AWAY, //send away status
    SYNC_AWAY, //sync away status for other clients in gameroom
    RESET_AWAY, // reset away status for a client in gameroom
    COOLDOWN // cooldown enabler for gameroom
}