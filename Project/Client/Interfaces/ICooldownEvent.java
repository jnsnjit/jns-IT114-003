package Project.Client.Interfaces;

public interface ICooldownEvent extends IGameEvents {
    /**
     * Receives the ready status and id
     * 
     * @param clientId
     * @param isReady
     */
    void onReciveeCooldown(long clientId, boolean cooldown, boolean isQuiet);
}