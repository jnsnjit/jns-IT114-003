package Project.Server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
 

import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.TimedEvent;

public class GameRoom extends BaseGameRoom {
    
    // used for general rounds (usually phase-based turns)
    private TimedEvent roundTimer = null;

    // used for granular turn handling (usually turn-order turns)
    //private TimedEvent turnTimer = null;
    
    // hashmap of players and there choices, double memory usage though
    protected ConcurrentHashMap<ServerPlayer,Integer> playerChoices = new ConcurrentHashMap<ServerPlayer, Integer>();

    public GameRoom(String name) {
        super(name);
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientAdded(ServerPlayer sp){
        // sync GameRoom state to new client
        syncCurrentPhase(sp);
        syncReadyStatus(sp);
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientRemoved(ServerPlayer sp){
        // added after Summer 2024 Demo
        // Stops the timers so room can clean up
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + playersInRoom.size());
        if(playersInRoom.isEmpty()){
            resetReadyTimer();
            //resetTurnTimer();
            resetRoundTimer();
            onSessionEnd();
        }
    }

    // timer handlers
    private void startRoundTimer(){
        roundTimer = new TimedEvent(30, ()-> onRoundEnd());
        roundTimer.setTickCallback((time)->System.out.println("Round Time: " + time));
    }
    private void resetRoundTimer(){
        if(roundTimer != null){
            roundTimer.cancel();
            roundTimer = null;
        }
    }
    /**
    private void startTurnTimer(){
        turnTimer = new TimedEvent(30, ()-> onTurnEnd());
        turnTimer.setTickCallback((time)->System.out.println("Turn Time: " + time));
    }
    
    private void resetTurnTimer(){
        if(turnTimer != null){
            turnTimer.cancel();
            turnTimer = null;
        }
    }
    */
    // end timer handlers
    
    // lifecycle methods

    /** {@inheritDoc} */
    @Override
    protected void onSessionStart(){
        LoggerUtil.INSTANCE.info("onSessionStart() start");
        changePhase(Phase.MAKE_CHOICE);
        //send message to players to roll rps.
        LoggerUtil.INSTANCE.info("onSessionStart() end");
        onRoundStart();
    }

    /** {@inheritDoc} */
    @Override
    protected void onRoundStart(){
        //sanity check to make sure it is starting with phase in correct phase
        //move reset to here instead of round end 
        if(currentPhase != Phase.MAKE_CHOICE){
            return;
        }
        sendMessage(null, "make rps command! 'rps rock' ...");
        LoggerUtil.INSTANCE.info("onRoundStart() start");
        //reset values from last game to make sure players can roll again
        Collection<ServerPlayer> players = playersInRoom.values();
        for(ServerPlayer p : players){
            playerChoices.remove(p);
        }
        resetRoundTimer();
        startRoundTimer();
        //if length of playerchoices = players in round, timer can end
        //all players must make decision, those who dont, do not play
        LoggerUtil.INSTANCE.info("onRoundStart() end");
    }

    /** {@inheritDoc} */
    /**unused
    @Override
    protected void onTurnStart(){
        LoggerUtil.INSTANCE.info("onTurnStart() start");
        resetTurnTimer();
        startTurnTimer();
        LoggerUtil.INSTANCE.info("onTurnStart() end");
    }
    */
    // Note: logic between Turn Start and Turn End is typically handled via timers and user interaction
    /** {@inheritDoc} */
    /** unused
    @Override
    protected void onTurnEnd(){
        LoggerUtil.INSTANCE.info("onTurnEnd() start");
        resetTurnTimer(); // reset timer if turn ended without the time expiring

        LoggerUtil.INSTANCE.info("onTurnEnd() end");
    }
    // Note: logic between Round Start and Round End is typically handled via timers and user interaction
    */
    /** {@inheritDoc} */
    @Override
    protected void onRoundEnd(){
        //quick check to make sure phase is in progess and not ended
        if(currentPhase == Phase.READY){
            //in wrong area, leaving
            return;
        }
        LoggerUtil.INSTANCE.info("onRoundEnd() start");
        resetRoundTimer(); // reset timer if round ended without the time expiring
        startRoundTimer();
        //after players make their choices, this when concurrenthashmap should be filled and ready to process
        // Convert map entries to a list for ordered access
        List<Entry<ServerPlayer, Integer>> entries = new ArrayList<>();
        for (Entry<ServerPlayer, Integer> entry : playerChoices.entrySet()) {
            entries.add(entry);
        }
        // ATTACK-BASED RPS, "attackers" determine winner and loser"
        // Iterate over each entry and compare with the next one in a circular fashion
        for (int i = 0; i < entries.size(); i++) {
            Entry<ServerPlayer, Integer> currentEntry = entries.get(i);
            Entry<ServerPlayer, Integer> nextEntry = entries.get((i + 1) % entries.size()); // Wraps around to first

            LoggerUtil.INSTANCE.info("Comparing " + currentEntry.getKey() + " with " + nextEntry.getKey());

            if (currentEntry.getValue().compareTo(nextEntry.getValue()) == 0) {
                // rolled the same, both players survive
                LoggerUtil.INSTANCE.info("both players pass");
            } else if (currentEntry.getValue().compareTo(nextEntry.getValue()) > 0){
                //player won the attack
                if(determineWinner(nextEntry, currentEntry)){
                    break;
                }
            } else {
                //player lost the attack
                if(determineWinner(currentEntry, nextEntry)){
                    break;
                }
            }
        }
        // Display keys with value 1
        //if one player remains, end session, if not, start round with players that did not get elim'd/still in hashmap
        if(playerChoices.size() == 1){
            ServerPlayer winningPlayer = playerChoices.keySet().iterator().next();
            sendMessage(null, "Player[" + winningPlayer.getClientId() + "] WON!!!!");
            //instead of removing them, just make the alive value false, change some logic too
            //playerChoices.remove(winningPlayer);
            LoggerUtil.INSTANCE.info("onRoundEnd() end");
            resetRoundTimer();
            onSessionEnd();
        } else {
            //next round MUST only let players in player choices roll
            //LAST THING TO IMPLEMENT YAYAYYAYAYAYAYY!
            //i think all i have to do is to empty player choices, and restart from roundstart
            sendMessage(null,"Game is not over, tie occured!");
            onRoundStart();
        }
        
    }

    /** {@inheritDoc} */
    @Override
    protected void onSessionEnd(){
        //now on session end, must display who won, and the points they earned!
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
        resetReadyStatus();
        changePhase(Phase.READY);
        // Convert map entries to a list for ordered access, contains the player
        List<Entry<Long, ServerPlayer>> entries = new ArrayList<>();
        for (Entry<Long, ServerPlayer> entry : playersInRoom.entrySet()) {
            entries.add(entry);
        }
        // Sort the list by values (ascending order)
        String leaderboard = "\n";
        int placement = 0;
        for (Entry<Long, ServerPlayer> entry : entries) {
            placement +=1;
            ServerThread st = entry.getValue().getServerThread();
            leaderboard = leaderboard + placement +". " + st.getClientName() + ": " + entry.getValue().getPoints() + " points" + "\n";
        }
        LoggerUtil.INSTANCE.info(leaderboard);
        sendMessage(null,leaderboard);
        LoggerUtil.INSTANCE.info("onSessionEnd() end");
        //reset all alive values back to true
        Collection<ServerPlayer> players = playersInRoom.values();
        for(ServerPlayer p : players){
            p.reset();
        }
    }
    // end lifecycle methods

    // send/sync data to ServerPlayer(s)
    // end send data to ServerPlayer(s)

    // receive data from ServerThread (GameRoom specific)
    protected synchronized void recieveChoice(ServerThread player, Integer choice){
        //based of user who called (need id), check and process their rps command and store their choice
        //this IS the handle method, at end of this, you can also check if EVERYONE took a turn, if so, enter endround
        ServerPlayer sp = playersInRoom.get(player.getClientId());
        //handle as well if user is elim'd, if so, they cant play
        if(!sp.isAlive()){
            player.sendMessage("You have been elimanated, and you can not roll again until the game is over.");
            return;
        }
        //finds associated thread with serverplayer
        sp.setChoice(choice);
        playerChoices.put(sp,choice);
        sendMessage(null,"Player [" + player.getClientId() + "] has made their choice!");
        LoggerUtil.INSTANCE.info("Player [" + player.getClientId() + "] has made their choice!");
        checkIfAllMadeTurn();
    }
    // end receive data from ServerThread (GameRoom specific)
    // need a checker method to see if all turns are made
    protected void checkIfAllMadeTurn(){
        long ready = playersInRoom.size();
        long choices = playerChoices.size();
        if(ready == choices){
            //end round timer
            resetRoundTimer();
            //call roundend section
            LoggerUtil.INSTANCE.info("onRoundStart() end");
            onRoundEnd();
        }
    }
    protected boolean determineWinner(Entry<ServerPlayer, Integer> loser, Entry<ServerPlayer, Integer> winner){
        //first player rolled losing roll, second rolled winning roll, remove loser from map.
        ServerPlayer lPlayer = loser.getKey();
        ServerPlayer wPlayer = winner.getKey();
        lPlayer.setAlive(false);
        playerChoices.remove(lPlayer);
        LoggerUtil.INSTANCE.info("first player lost rolls");
        sendMessage(null, "Player [" + lPlayer.getClientId() + "] is ELIMINATED! players left is " + playerChoices.size());
        wPlayer.addPoint();
        LoggerUtil.INSTANCE.info("adding point for player who won");
        //THERE IS ONE WEIRD EDGE CASE, and this occurs when there is only two players in the room
        //modulus will check the same case twice, and spit message twice, which is annoying
        //so just checking edge case, if so, break out of for loop
        if(playerChoices.size() == 1){
            //if true, must break out of loop to avoid edge case
            return true;
        }
        return false;
    }
}