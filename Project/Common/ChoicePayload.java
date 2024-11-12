package Project.Common;

public class ChoicePayload extends Payload {
    private int choice;
    public ChoicePayload(){
        setPayloadType(PayloadType.CHOICE);
    }
    public void setChoice(String choice){
        if(choice.equals("rock")){
            this.choice = 1;
        }
        if(choice.equals("paper")){
            this.choice = 2;
        }
        if(choice.equals("scissors")){
            this.choice = 3;
        }
    }
}
