package datarace;

public class Main {

	public static void main(String[] args) {
		int nAccount = 1;
		Account[] accounts = new Account[10];
		for (int i = 0; i < 10; i++) {
			accounts[i] = new Account();
		}
//		CustomerInfo ci = new CustomerInfo(nAccount, accounts);
		
		Thread a = new CustomerInfo(nAccount, accounts);
		Thread b = new CustomerInfo(nAccount, accounts);
		
		a.start();
		b.start();
	}
}
