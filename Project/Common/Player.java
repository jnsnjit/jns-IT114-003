package Project.Common;

/**
 * Common Player data shared between Client and Server
 */
public class Player {
    public static long DEFAULT_CLIENT_ID = -1L;
    private long clientId = Player.DEFAULT_CLIENT_ID;
    private boolean isReady = false;
    private Integer choice;
    private boolean alive = true;
    private int points = 0;
    //milestone4 adding away status
    private boolean away = false;
    
    public long getClientId() {
        return clientId;
    }
    
    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public boolean isReady() {
        return isReady;
    }
    public void setReady(boolean isReady) {
        this.isReady = isReady;
        if(isReady == true){
            away = false;
        }
    }
    public boolean isAway(){
        return away;
    }
    public void setAway(){
        this.away = away ? false : true;
        if(away){
            isReady = false;
        }
    }
    public void setAway(boolean away){
        this.away = away;
    }
    /**
     * Resets all of the data (this is destructive).
     * You may want to make a softer reset for other data
     */
    public void reset(){
        this.clientId = Player.DEFAULT_CLIENT_ID;
        this.isReady = false;
        this.choice = -1;
        this.points = 0;
        this.alive = true;
        this.away = false;
    }
    public void softReset(){
        this.isReady = false;
        this.choice = -1;
        this.alive = true;
    }
    public void setChoice(Integer choice){
        this.choice = choice;
    }
    public Integer getChoice(){
        return choice;
    }
    public void setAlive(boolean alive){
        this.alive = alive;
    }
    public boolean isAlive(){
        return alive;
    }
    public void addPoint(){
        points+=1;
    }
    public int getPoints(){
        return points;
    }
}