package Project;

public class PointsPayload extends Payload {
    private int points;
    protected void addPoints(){
        this.points += 2;
    }
    protected String getPoints(){
        return points + "";
    }
    @Override
    public String toString(){
        return super.toString() + " hi just making sure for now";
    }
}
