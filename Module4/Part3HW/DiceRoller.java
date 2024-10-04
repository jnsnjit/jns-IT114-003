package Module4.Part3HW;
//implementing dice roller class with name and chosen rolled dice + amount
public class DiceRoller {
    private String name;
    private String diceType;
    private int diceAmount;
    private int diceSides;

    //diceRoller constructer
    public DiceRoller(String name, String diceType){
        this.name = name;
        this.diceType = diceType;
    }
    //checks if diceType string is a proper format, will modify toString method depending on result
    public boolean isDiceFormat(){
        String temp = diceType;
        //loop checks each to see if each character of the string is a digit, if not, check if d, if not, return false
        for(int x = 0; x<temp.length();x++){
            if(Character.isDigit(temp.charAt(x))){
                continue;
            }
            if(temp.charAt(x).equalsIgnoreCase("d")){
                temp = temp.substring(x+1);
                diceAmount = Integer.parseInt(temp.substring(0,x));
                break;
            }
            return false;
        }
        //after checking first half, checks if rest of the string is all digits, returns false if not
        for(int y=0;y<temp.length();y++){
            if(Character.isDigit(temp.charAt(y))){
                continue;
            }else{
                return false;
            }
        }
        diceSides = Integer.parseInt(temp);
        return true;
    }
    //method rolls dice inputted amount of times, and displays the total of the rolled dice.
    private String result(){
        int total = 0;
        int roll;
        for(int n =0;n<diceAmount;n++){
            roll = Math.random() * diceSides;
            total += roll;
        }
        return roll + "";
    }
    //output method, if dice format is correct, will display rolled dice for user, if not, tells that the format was wrong
    public String output(){
        if(isDiceFormat()){
            return name + " rolled " + diceType + " and got " + result();
        }else{
            return "This is not a valid formatted dice";
        }
    }
}
 