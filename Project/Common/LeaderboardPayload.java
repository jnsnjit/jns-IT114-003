package Project.Common;
import java.util.List;
public class LeaderboardPayload extends Payload{
    //change to serializible scoreboard
    private List<LeaderboardRecord> board;
    
    public LeaderboardPayload(){
        setPayloadType(PayloadType.LEADERBOARD);
    }

    public List<LeaderboardRecord> getLeaderboard(){
        return board;
    }
    public void setLeaderboard(List<LeaderboardRecord> board){
        this.board = board;
    }
    
}
