package Project.Common;

import java.io.Serializable;
public class LeaderboardRecord implements Serializable {
    private int rank;
    private String name;
    private int points;

    public LeaderboardRecord(String name, int points){
        this.rank = 0;
        this.name = name;
        this.points = points;
    }   
    //getters for table filling
    public int getRank(){
        return rank;
    }
    public String getName(){
        return name;
    }
    public int getPoints(){
        return points;
    }
    public void setRank(int rank){
        this.rank = rank;
    }
}
