package Module4.Part3HW;
import java.util.Random;

public class Coin {
    private String name;
    private String result;
    public Coin(String name){
        this.name = name;
        this.result = flipCoin();
    }
    public String flipCoin(){
        Random r = new Random();
        return (r.nextInt(2) == 1)?"Heads":"Tails";
    }
    public String output(){
        return name + " flipped a coin and got " + result;
    }
}
