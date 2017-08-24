package datarace;

public class Main {

	public static void main(String[] args) {
		int nAccount = 100;
		Account[] accounts = new Account[10];
		for (int i = 0; i < 10; i++) {
			accounts[i] = new Account();
		}
		CustomerInfo ci = new CustomerInfo(nAccount, accounts);
		
		ThreadRun a = new ThreadRun(ci);
		ThreadRun b = new ThreadRun(ci);
		
		a.start();
		b.start();
	}
}
