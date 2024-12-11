package Project.Common;

public class CooldownPayload extends Payload {
    public boolean enable;
    public CooldownPayload(){
        setPayloadType(PayloadType.COOLDOWN);
    }
    public void setCooldown(boolean enable){
        this.enable = enable;
    }
    public boolean getCooldown(){
        return enable;
    }
}
