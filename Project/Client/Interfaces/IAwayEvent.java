package Project.Client.Interfaces;

public interface IAwayEvent extends IGameEvents {
    /**
     * Receives the ready status and id
     * 
     * @param clientId
     * @param isReady
     */
    void onRecieveAway(long clientId, boolean isAway, boolean isQuiet);
}