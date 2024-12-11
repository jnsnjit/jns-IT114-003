package Project.Server;

import java.util.List;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Collection;
import Project.Common.TimerType;
import java.util.Map.Entry;

import Project.Common.LeaderboardRecord;
import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.TimedEvent;

public class GameRoom extends BaseGameRoom {

    // used for general rounds (usually phase-based turns)
    private TimedEvent roundTimer = null;

    // used for granular turn handling (usually turn-order turns)
    // private TimedEvent turnTimer = null;

    // hashmap of players and there choices, double memory usage though
    protected ConcurrentHashMap<ServerPlayer, Integer> playerChoices = new ConcurrentHashMap<ServerPlayer, Integer>();
    //protected ConcurrentSkipListMap<ServerPlayer, Integer> playerChoices = new ConcurrentSkipListMap<ServerPlayer, Integer>();
    private int round = 0;

    //comment for the future, want logic of rps to be based on players readying up first, instead of player ID, 
    //maybe add attribute like time attribute to player?

    public GameRoom(String name) {
        super(name);
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientAdded(ServerPlayer sp) {
        // sync GameRoom state to new client
        // give a slight delay to allow the Room list content to be sent from the base
        // class
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                syncCurrentPhase(sp.getServerThread());
                syncReadyStatus(sp);
            }
        }.start();
    }
    @Override
    protected void onSpectatorAdded(ServerSpectator ss){
        //syncing for spectators, need less things from the room
        new Thread(){
            @Override
            public void run(){
                try{
                    Thread.sleep(100);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
                //figure this out bruh
                syncCurrentPhase(ss.getServerThread());
            }
        }.start();
    }
    /** {@inheritDoc} */
    @Override
    protected void onClientRemoved(ServerPlayer sp) {
        // added after Summer 2024 Demo
        // Stops the timers so room can clean up
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + playersInRoom.size());
        if (playersInRoom.isEmpty()) {
            resetReadyTimer();
            // resetTurnTimer();
            resetRoundTimer();
            onSessionEnd();
        }
    }
    @Override
    protected void onSpectatorRemoved(ServerSpectator ss){
        // special to remove spectators
        LoggerUtil.INSTANCE.info("Spectator removed, remaing: " + spectatorsInRoom.size());
    }

    // timer handlers
    private void startRoundTimer() {
        roundTimer = new TimedEvent(30, () -> onRoundEnd());
        roundTimer.setTickCallback((time) -> {
            System.out.println("Round Time: " + time);
            sendCurrentTime(TimerType.ROUND, time);
        });
    }

    private void resetRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
            sendCurrentTime(TimerType.ROUND, -1);
        }
    }
    /**
     * private void startTurnTimer(){
     * turnTimer = new TimedEvent(30, ()-> onTurnEnd());
     * turnTimer.setTickCallback((time)->System.out.println("Turn Time: " + time));
     * }
     * 
     * private void resetTurnTimer(){
     * if(turnTimer != null){
     * turnTimer.cancel();
     * turnTimer = null;
     * }
     * }
     */
    // end timer handlers

    // lifecycle methods

    /** {@inheritDoc} */
    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("onSessionStart() start");
        changePhase(Phase.MAKE_CHOICE);
        // send message to players to roll rps.
        LoggerUtil.INSTANCE.info("onSessionStart() end");
        onRoundStart();
    }

    /** {@inheritDoc} */
    @Override
    protected void onRoundStart() {
        // sanity check to make sure it is starting with phase in correct phase
        // move reset to here instead of round end
        if (currentPhase != Phase.MAKE_CHOICE) {
            return;
        }
        sendGameEvent("choose a option! (click one of the buttons)");
        LoggerUtil.INSTANCE.info("onRoundStart() start");
        round++;
        sendGameEvent("Round: " + round);
        // reset values from last game to make sure players can roll again
        Collection<ServerPlayer> players = playersInRoom.values();
        for (ServerPlayer p : players) {
            playerChoices.remove(p);
        }
        resetRoundTimer();
        startRoundTimer();
        // if length of playerchoices = players in round, timer can end
        // all players must make decision, those who dont, do not play
        LoggerUtil.INSTANCE.info("onRoundStart() end");
    }

    /** {@inheritDoc} */

    /**
     * unused
     * 
     * @Override
     *           protected void onTurnStart(){
     *           LoggerUtil.INSTANCE.info("onTurnStart() start");
     *           resetTurnTimer();
     *           startTurnTimer();
     *           LoggerUtil.INSTANCE.info("onTurnStart() end");
     *           }
     */
    // Note: logic between Turn Start and Turn End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    /**
     * unused
     * 
     * @Override
     *           protected void onTurnEnd(){
     *           LoggerUtil.INSTANCE.info("onTurnEnd() start");
     *           resetTurnTimer(); // reset timer if turn ended without the time
     *           expiring
     * 
     *           LoggerUtil.INSTANCE.info("onTurnEnd() end");
     *           }
     *           // Note: logic between Round Start and Round End is typically
     *           handled via timers and user interaction
     */
    /** {@inheritDoc} */
    @Override
    protected void onRoundEnd() {
        // quick check to make sure phase is in progess and not ended
        if (currentPhase == Phase.READY) {
            // in wrong area, leaving
            return;
        }
        LoggerUtil.INSTANCE.info("onRoundEnd() start");
        resetRoundTimer(); // reset timer if round ended without the time expiring
        startRoundTimer();
        // after players make their choices, this when concurrenthashmap should be
        // filled and ready to process
        // Convert map entries to a list for ordered access
        List<Entry<ServerPlayer, Integer>> entries = new ArrayList<>();
        //need to sort by some metric, sorting by clientID
        for (Entry<ServerPlayer, Integer> entry : playerChoices.entrySet()) {
            entries.add(entry);
        }
        for (int i = 0; i < entries.size() - 1; i++) {
            for (int j = 0; j < entries.size() - i - 1; j++) {
                if ((entries.get(j)).getKey().getClientId() > (entries.get(j + 1)).getKey().getClientId()) {
                    // Swap elements
                    Entry<ServerPlayer, Integer> temp = (entries.get(j));
                    entries.set(j, entries.get(j + 1));
                    entries.set(j + 1, temp);
                }
            }
        }
        //orders player choices by client id in ascending order. 1,2,3 ...
        // ATTACK-BASED RPS, "attackers" determine winner and loser"
        // Iterate over each entry and compare with the next one in a circular fashion
        for (int i = 0; i < entries.size(); i++) {
            Entry<ServerPlayer, Integer> currentEntry = entries.get(i);
            Entry<ServerPlayer, Integer> nextEntry = entries.get((i + 1) % entries.size()); // Wraps around to first

            LoggerUtil.INSTANCE.info("Comparing " + currentEntry.getKey() + " with " + nextEntry.getKey());

            if (currentEntry.getValue().compareTo(nextEntry.getValue()) == 0) {
                // rolled the same, both players survive
                LoggerUtil.INSTANCE.info("both players pass");
            } else if ((currentEntry.getValue() + 2 ) %3 == nextEntry.getValue() %3) {
                // player won the attack   (r1 v s3)(p2 v r1)(s3 v p2)
                // add point to winner
                // currentEntry.getKey().addPoint();
                if (determineWinner(nextEntry, currentEntry)) {
                    break;
                }
            } else {
                // player lost the attack
                if (determineWinner(currentEntry, nextEntry)) {
                    break;
                }
            }
        }
        // Display keys with value 1
        // if one player remains, end session, if not, start round with players that did
        // not get elim'd/still in hashmap
        int playersAlive = 0;
        for(Entry<ServerPlayer,Integer> k : playerChoices.entrySet()){
            playersAlive = k.getKey().isAlive() ? ++playersAlive : playersAlive;
        }
        if (playersAlive == 1) {
            ServerPlayer winningPlayer = playerChoices.keySet().iterator().next();
            sendGameEvent("Player[" + winningPlayer.getClientId() + "] WON!!!!");
            // instead of removing them, just make the alive value false, change some logic
            // too
            // playerChoices.remove(winningPlayer);
            LoggerUtil.INSTANCE.info("onRoundEnd() end");
            resetRoundTimer();
            changePhase(Phase.BOARD);
            createLeaderboard();
            new TimedEvent(10, () -> {
                onSessionEnd();
            });
            return;
        } else {
            // next round MUST only let players in player choices roll
            // LAST THING TO IMPLEMENT YAYAYYAYAYAYAYY!
            // i think all i have to do is to empty player choices, and restart from
            // roundstart
            sendGameEvent("Game is not over, tie occured!");
            onRoundStart();
        }

    }
    protected void createLeaderboard(){
        changePhase(Phase.BOARD);
            // Convert map entries to a list for ordered access, contains the player
            List<Entry<Long, ServerPlayer>> entries = new ArrayList<>();
            for (Entry<Long, ServerPlayer> entry : playersInRoom.entrySet()) {
                entries.add(entry);
            }
            // Sort the list by values (ascending order)
            List<LeaderboardRecord> table = new ArrayList<>();
            for (Entry<Long, ServerPlayer> entry : entries) {
                ServerThread st = entry.getValue().getServerThread();
                LeaderboardRecord leaderboard = new LeaderboardRecord(st.getClientName(), entry.getValue().getPoints());
                table.add(leaderboard);
            }
            sendLeaderboard(table);
            //LoggerUtil.INSTANCE.info(leaderboard);
            //send leaderboard to JTable in readyPanel
    }
    /** {@inheritDoc} */
    @Override
    protected void onSessionEnd() {
        // now on session end, must display who won, and the points they earned!
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
        resetReadyStatus();
        LoggerUtil.INSTANCE.info("onSessionEnd() end");
        changePhase(Phase.READY);
        // reset all alive values back to true
        Collection<ServerPlayer> players = playersInRoom.values();
        for (ServerPlayer p : players) {
            p.softReset();
            sendReadyStatus(p,false);
            playerChoices.remove(p);
        }
        // only run this game if a game occured
        
    }
    // end lifecycle methods
    
    // send/sync data to ServerPlayer(s)
    // end send data to ServerPlayer(s)

    protected synchronized void recieveChoice(ServerThread player, Integer choice) {
        // based of user who called (need id), check and process their rps command and
        // store their choice
        // this IS the handle method, at end of this, you can also check if EVERYONE
        // took a turn, if so, enter endround
        //adding additional check bc they saved me for milestone4!!
        try{
            checkPlayerInRoom(player);
            checkCurrentPhase(player, Phase.MAKE_CHOICE);
        } catch(Exception e) {
            LoggerUtil.INSTANCE.severe("exception handled, probably spectator trying to send choice", e);
        }

        ServerPlayer sp = playersInRoom.get(player.getClientId());
        // handle as well if user is elim'd, if so, they cant play, and if they didnt ready up
        if(sp.isAway()){
            List<Long> out = new ArrayList<Long>();
            out.add(player.getClientId());
            sendGameEvent("You are currently marked as away and can't play until the round is over", out);
            return;
        }
        if (!sp.isAlive()) {
            List<Long> out = new ArrayList<Long>();
            out.add(player.getClientId());
            try{
                checkPlayerIsReady(sp);
            } catch (Exception e){
                sendGameEvent("You didnt ready up, so you have to wait until the next round...", out);
            }
            sendGameEvent("You have been elimanated, and you can not roll again until the game is over.", out);
            return;
        }
        // finds associated thread with serverplayer
        sp.setChoice(choice);
        playerChoices.put(sp, choice);
        sendGameEvent("Player [" + player.getClientId() + "] has made their choice!");
        LoggerUtil.INSTANCE.info("Player [" + player.getClientId() + "] has made their choice!");
        checkIfAllMadeTurn();
    }

    // end receive data from ServerThread (GameRoom specific)
    // need a checker method to see if all turns are made
    protected void checkIfAllMadeTurn() {
        long ready = playersInRoom.size();
        long choices = playerChoices.size();
        if (ready == choices) {
            // end round timer
            resetRoundTimer();
            // call roundend section
            LoggerUtil.INSTANCE.info("onRoundStart() end");
            onRoundEnd();
        }
    }
    
    private void checkPlayerIsReady(ServerPlayer sp) throws Exception {
        if (!sp.isReady()) {
            sp.sendGameEvent("You weren't ready in time");
            throw new Exception("Player isn't ready");
        }
    }
    
    private void sendGameEvent(String str) {
        sendGameEvent(str, null);
    }

    /**
     * Sends a game event to specific clients (by id)
     * 
     * @param str
     * @param targets
     */
    private void sendGameEvent(String str, List<Long> targets) {
        playersInRoom.values().removeIf(spInRoom -> {
            boolean canSend = false;
            if (targets != null) {
                if (targets.contains(spInRoom.getClientId())) {
                    canSend = true;
                }
            } else {
                canSend = true;
            }
            if (canSend) {
                boolean failedToSend = !spInRoom.sendGameEvent(str);
                if (failedToSend) {
                    removedClient(spInRoom.getServerThread());
                }
                return failedToSend;
            }
            return false;
        });
    }
    private void sendLeaderboard(List<LeaderboardRecord> board){
        playersInRoom.values().removeIf(spInRoom -> {
            boolean canSend = true;
            if (canSend) {
                boolean failedToSend = !spInRoom.sendLeaderboard(board);
                if (failedToSend) {
                    removedClient(spInRoom.getServerThread());
                }
                return failedToSend;
            }
            return false;
        });
    }

    private void sendPointsUpdate(ServerPlayer sp) {
        playersInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendPointsUpdate(sp.getClientId(), sp.getPoints());
            if (failedToSend) {
                removedClient(spInRoom.getServerThread());
            }
            return failedToSend;
        });
    }

    protected boolean determineWinner(Entry<ServerPlayer, Integer> loser, Entry<ServerPlayer, Integer> winner) {
        // first player rolled losing roll, second rolled winning roll, remove loser
        // from map.
        ServerPlayer lPlayer = loser.getKey();
        ServerPlayer wPlayer = winner.getKey();
        lPlayer.setAlive(false);
        int playersAlive = 0;
        for(Entry<ServerPlayer,Integer> k : playerChoices.entrySet()){
            playersAlive = k.getKey().isAlive() ? ++playersAlive : playersAlive;
        }
        LoggerUtil.INSTANCE.info("first player lost rolls");
        sendGameEvent("Player [" + lPlayer.getClientId() + "] is ELIMINATED! players left is " + playersAlive);
        wPlayer.addPoint();
        sendPointsUpdate(wPlayer);
        LoggerUtil.INSTANCE.info("adding point for player who won");
        // THERE IS ONE WEIRD EDGE CASE, and this occurs when there is only two players
        // in the room
        // modulus will check the same case twice, and spit message twice, which is
        // annoying
        // so just checking edge case, if so, break out of for loop
        if (playersAlive == 1) {
            // if true, must break out of loop to avoid edge case
            return true;
        }
        return false;
    }
}