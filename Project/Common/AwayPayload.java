package Project.Common;

public class AwayPayload extends Payload{
    public boolean away;
    public AwayPayload(){
        setPayloadType(PayloadType.AWAY);
    }
    public void setAway(boolean away){
        this.away = away;
    }
    public boolean getAway(){
        return away;
    }
}