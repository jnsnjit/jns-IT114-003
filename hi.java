class hi{
    public static void main(String[] args){
        System.out.println("hello world");
        System.out.println(recursion(5));
        //main branch
    }
    public static int recursion(int x){
        if(x==0){
            return 1;
        }
        return x * recursion(x-1);
    }
}
//comment, class hi, just a test

