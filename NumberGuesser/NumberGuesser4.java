package NumberGuesser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.Base64;

//initizing all important vars for first time player
public class NumberGuesser4 {
    private int maxLevel = 1;
    private int level = 1;
    private int strikes = 0;
    private int maxStrikes = 5;
    private int number = -1;

    //TASK 5: HINT SYSTEM
    private int hints = 0;
    //TASK 5

    private boolean pickNewRandom = true;
    private Random random = new Random();
    private String fileName = "ng4.txt";
    //private String[] fileHeaders = { "Level", "Strikes", "Number", "MaxLevel","Hints" };// used for readability

    //takes local state of variables in java file and saves it to ng4.txt
    private void saveState() {
        String[] data = { level + "", strikes + "", number + "", maxLevel + "", hints + ""};

        //TASK 2
        data = meanShuffle(data,false);//for anti-data tampering, converts values into base64
        //TASK 2 

        String output = String.join(",", data);

        try (FileWriter fw = new FileWriter(fileName)) {
            //fw.write(String.join(",", fileHeaders));
            //fw.write("\n");
            fw.write(output);//write encoded values
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //TASK 2
    //to avoid data tampering, will remove file headers and turn numbers into base64
    private String[] meanShuffle(String[] data,boolean state){
        if(state){
            for(int i =0;i<data.length;i++){
                data[i] = new String(Base64.getDecoder().decode(data[i]));
            }
        }else{
            for(int i =0;i<data.length;i++){
                data[i] = Base64.getEncoder().encodeToString(data[i].getBytes());
            }
        }
        return data;
    }
    //TASK 2
    private void loadState() {
        File file = new File(fileName);
        if (!file.exists()) {
            // Not providing output here as it's expected for a fresh start
            return;
        }
        try (Scanner reader = new Scanner(file)) {
            int lineNumber = 0;
            while (reader.hasNextLine()) {
                String text = reader.nextLine();
                // System.out.println("Text: " + text);
                if (lineNumber == 0) {
                    String[] data = text.split(",");
                    //takes the encoded values and decodes them
                    data = meanShuffle(data, true);
                    //TASK 2
                    String level = data[0];
                    String strikes = data[1];
                    String number = data[2];
                    String maxLevel = data[3];
                    String hints = data[4];
                    int temp = strToNum(level);
                    if (temp > -1) {
                        this.level = temp;
                    }
                    temp = strToNum(strikes);
                    if (temp > -1) {
                        this.strikes = temp;
                    }
                    temp = strToNum(number);
                    if (temp > -1) {
                        this.number = temp;
                        pickNewRandom = false;
                    }
                    temp = strToNum(maxLevel);
                    if (temp > -1) {
                        this.maxLevel = temp;
                    }
                    temp = strToNum(hints);
                    if (temp > -1) {
                        this.hints = temp;
                    }
                }
                lineNumber++;
            }
        } catch (FileNotFoundException e) {// specific exception
            e.printStackTrace();
        } catch (Exception e2) {// any other unhandled exception
            e2.printStackTrace();
        }
        System.out.println("Loaded state");
        int range = 10 + ((level - 1) * 5);
        System.out.println("Welcome to level " + level);
        System.out.println(
                "I picked a random number between 1-" + (range) + ", let's see if you can guess.");
    }

    //gets random number
    private void generateNewNumber(int level) {
        int range = 10 + ((level - 1) * 5);
        System.out.println("Welcome to level " + level);
        System.out.println("I picked a random number between 1-" + (range) + ", let's see if you can guess.");
        number = random.nextInt(range) + 1;
    }
    //runs when the user guesses random number correctly before running out of strikes
    private void win() {
        System.out.println("That's right!");
        level++;// level up!
        strikes = 0;
        hints = 0;
    }

    private boolean processCommands(String message) {
        boolean processed = false;
        if (message.equalsIgnoreCase("quit")) {
            System.out.println("Tired of playing? No problem, see you next time.");
            processed = true;
        }
        //TASK 5
        if(message.equalsIgnoreCase("help")){
            hints();
        }
        //TASK 5
        return processed;
    }
    //runs if the user does not guess correctly within amount of strikes
    private void lose() {
        System.out.println("Uh oh, looks like you need to get some more practice.");
        System.out.println("The correct number was " + number);
        strikes = 0;
        hints = 0;
        level--;
        if (level < 1) {
            level = 1;
        }
        //TASK 3
        changeDifficulty();
        //TASK 3
    }
    //TASK 3: askes user to change difficulty after losing, 10,5,3 strikes...
    private void changeDifficulty(){
        Scanner s = new Scanner(System.in);
        System.out.println("Want to change difficulty?(type y/Y)");
        String m = s.nextLine();
        if(m.equalsIgnoreCase("y")){
            System.out.println("E:10 Strikes, M:5 Strikes, H:3 Strikes");
            m=s.nextLine();
            if(m.equalsIgnoreCase("E")){
                maxStrikes = 10;
            }
            if(m.equalsIgnoreCase("M")){
                maxStrikes = 5;
            }
            if(m.equalsIgnoreCase("H")){
                maxStrikes = 3;
            }
        }else{
            System.out.println("Ok, continue playing!");
        }

    }
    //TASK 3
    //TASK 5: hint system, when user asks for help after two strikes, they can get up to a max of two hints
    private void hints(){
        //random range so user can not identify the number by looking at the middle of the range
        int range = 10 + ((level - 1) * 5);
        int mod = (int)(Math.random()*9+1);
        int mod2 = (int)(Math.random()*6+1);
        System.out.println(mod);
        if(hints == 0){
            int l = number - mod < 0 ? 0 : number - mod;
            int h = number + mod > range ? range : number + mod;
            System.out.println("Answer is in between " + (l) + " and " + (h));
            hints++;
        }else if(hints == 1){
            int l = number - mod2 < 0 ? 0 : number - mod2;
            int h = number + mod2 > range ? range : number + mod2;
            System.out.println("Answer is in between " + (l) + " and " + (h));
            System.out.println("Last Hint for this Level!");
            hints++;
        }else{
            System.out.println("No hints for you!!");
        }
        saveState();
    }
    //takes guess after converted to number and checks win/loss conditions
    private void processGuess(int guess) {
        if (guess < 0) {
            return;
        }
        System.out.println("You guessed " + guess);
        if (guess == number) {
            win();
            pickNewRandom = true;
        } else {
            System.out.println("That's wrong");
            strikes++;
            if (strikes >= maxStrikes) {
                lose();
                pickNewRandom = true;
            }
            //TASK 1: IMPLEMENT HIGHER LOWER SYSTEM
            if(guess < number && strikes != maxStrikes && strikes != 0){
                System.out.println("Higher...");
            }
            if(guess > number && strikes != maxStrikes && strikes != 0){
                System.out.println("Lower...");
            }
            //TASK 1
        }
        
        saveState();
    }
    //converts string inputs to integers
    private int strToNum(String message) {
        int guess = -1;
        if(message =="help"){
            //return guess;
        }
        try {
            guess = Integer.parseInt(message.trim());
        } catch (NumberFormatException e) {
            System.out.println("You didn't enter a number, please try again");
        } catch (Exception e2) {
            System.out.println("Null message received");
        } 
        return guess;
    }
    //runs until user quits, main game function
    public void start() {
        try (Scanner input = new Scanner(System.in);) {
            System.out.println("Welcome to NumberGuesser4.0");
            System.out.println("To exit, type the word 'quit'.");
            loadState();
            do {
                if (pickNewRandom) {
                    generateNewNumber(level);
                    saveState();
                    pickNewRandom = false;
                }
                if(strikes < 2){
                    System.out.println("Type a number and press enter");
                }else{
                    System.out.println("Type a number and press enter or type help to recieve a hint");
                }
                // we'll want to use a local variable here
                // so we can feed it into multiple functions
                String message = input.nextLine();
                // early termination check
                if (processCommands(message)) {
                    // command handled; don't proceed with game logic
                    break;
                }
                // this is just to demonstrate we can return a value and pass it into another
                // method
                if(message != null && !message.equalsIgnoreCase("help")){
                    int guess = strToNum(message);
                    processGuess(guess);
                }
                // the following line is the same as the above two lines
                // processGuess(getGuess(message));
            } while (true);
        } catch (Exception e) {
            System.out.println("An unexpected error occurred. Goodbye.");
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        System.out.println("Thanks for playing!");
    }

    public static void main(String[] args) {
        NumberGuesser4 ng = new NumberGuesser4();
        ng.start();
    }
}