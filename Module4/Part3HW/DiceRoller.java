package Module4.Part3HW;

import java.util.Random;//implementing dice roller class with name and chosen rolled dice + amount
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
    // method to check if a string is numeric
    private boolean isNumeric(String input) {
        if (input == null) {
            return false;
        }
        for (char c : input.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
    //checks if diceType string is a proper format, will modify toString method depending on result
    public boolean isDiceFormat(){
        int isDChar = diceType.indexOf("d");
        String temp = diceType;
        if(isDChar == -1){
            return false;
        }
        String amountPart = temp.substring(0,isDChar);
        String sidesPart = temp.substring(isDChar + 1);
        // checks if valid with numeric method
        if (!isNumeric(amountPart) || !isNumeric(sidesPart)) {
            return false; 
        }
        // parse the amounts and sides
        try{
            diceAmount = Integer.parseInt(amountPart); 
            diceSides = Integer.parseInt(sidesPart); 
        }catch(Exception e){
            return false;
        }
        return true;
    }
    //method rolls dice inputted amount of times, and displays the total of the rolled dice.
    private String result(){
        int total = 0;
        int roll = 0;
        Random r = new Random();
        for(int n =0;n<diceAmount;n++){
            roll = (int) (r.nextInt(diceSides) + 1);
            total += roll;
        }
        //return total of dice rolls
        return total + "";
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
 