package Project.Common;

public class LeaderboardPayload extends Payload{
    private String[][] leaderboard;
    
    public LeaderboardPayload(){
        setPayloadType(PayloadType.LEADERBOARD);
    }

    public void setLeaderboard(String[][] leaderboard) {
        this.leaderboard = leaderboard;
    }
    public String[][] getLeaderboard(){
        return leaderboard;
    }
    
}
