package airline;

public class Main {

    /*
     * Second parameter is the number of threads
     * Third parameter is the cushion
     */
     private static int numberThreads = 15;
     private static int cushion = 4;
	
     public static void main(String[] args) {
	if (args.length < 2){
	    //System.out.println("ERROR: Expected 2 parameters");
		new Bug("test",numberThreads,cushion);
	}else{
	    numberThreads = Integer.parseInt(args[0]);
	    cushion = Integer.parseInt(args[1]);
	    new Bug("test",numberThreads,cushion);
	}
     }
}