package Project;

public class GameRoom extends Room{
    
    public GameRoom(String name){
        super(name);
        System.out.println(String.format("GameRoom[%s] created", this.name));
        isGame = true;
    }
    public synchronized void startRound(){
        // if all players are ready
        // begin countdown
        // call check logic after countdown is done
    }
    public synchronized void checkResults(){
        // do rps logic check 1 -> 2 -> 3
        // if one remains, reset game, if not 
        // those who lose get mark elim'd, no longer ready checked but still watch
        // start countdown for players to readyup
        // call startRound when done
    }
    
}   
