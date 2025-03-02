package Project.Server;

import Project.Common.LeaderboardRecord;
import Project.Common.Phase;
import Project.Common.Player;
import Project.Common.TimerType;

import java.util.List;

/**
 * Server-only data about a player
 * Added in ReadyCheck lesson/branch for non-chatroom projects.
 * If chatroom projects want to follow this design update the following in this lesson:
 * Player class renamed to User
 * clientPlayer class renamed to ClientUser (or the original ClientData)
 * ServerPlayer class renamed to ServerUser
 */
public class ServerPlayer extends Player{
    private ServerThread client; // reference to wrapped ServerThread
    public ServerPlayer(ServerThread clientToWrap){
        client = clientToWrap;
        setClientId(client.getClientId());
    }
    /**
     * Used only for passing the ServerThread to the base class of Room.
     * Favor creating wrapper methods instead of interacting with this directly.
     * @return ServerThread reference
     */
    public ServerThread getServerThread(){
        return client;
    }
    // add any wrapper methods to call on the ServerThread
    // don't used the exposed full ServerThread object
    public boolean sendReadyStatus(long clientId, boolean isReady, boolean quiet){
        return client.sendReadyStatus(clientId, isReady, quiet);
    }
    public boolean sendReadyStatus(long clientId, boolean isReady){
       return client.sendReadyStatus(clientId, isReady);
    }
    public boolean sendCurrentTime(TimerType timerType, int time) {
        return client.sendCurrentTime(timerType, time);
    }

    public boolean sendPointsUpdate(long clientId, int points) {
        return client.sendPointsUpdate(clientId, points);
    }
    public boolean sendResetReady(){
        return client.sendResetReady();
    }

    public boolean sendCurrentPhase(Phase phase){
        return client.sendCurrentPhase(phase);
    }
    public boolean sendGameEvent(String message){
        return client.sendGameEvent(message);
    }
    public boolean sendLeaderboard(List<LeaderboardRecord> board){
        return client.sendLeaderboard(board);
    }
    //milestone4 away serverthread wrappers
    public boolean sendAwayReset(){
        return client.sendAwayReset();
    }
    public boolean sendAwayStatus(long clientId, boolean isAway){
        return client.sendAwayStatus(clientId, isAway);
    }
    public boolean sendAwayStatus(long clientId, boolean isAway, boolean quiet){
        return client.sendAwayStatus(clientId, isAway, quiet);
    }
    public boolean sendCooldown(long clientId, boolean cooldown){
        return client.sendCooldown(clientId, cooldown);
    }
    public boolean sendCooldown(long clientId, boolean cooldown, boolean quiet){
        return client.sendCooldown(clientId, cooldown, quiet);
    }
}