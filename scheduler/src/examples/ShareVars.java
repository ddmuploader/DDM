
public class ShareVars {
	public int number1;
	private int number2;
	
	public ShareVars() {
		number2 = 4;
	}
	
	public static void main(String[] args) {
		int number3;
		number3 = 3;
		number3--;
		ShareVars a = new ShareVars();
		a.number1 = a.number2 - 3;
	}
}
