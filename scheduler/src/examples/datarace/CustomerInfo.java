package datarace;

public class CustomerInfo extends Thread{

	private int nAccount;
	private Account[] accounts;
	
	public CustomerInfo() {
		
	}

	public CustomerInfo(int nAccount, Account[] accounts) {
		super();
		this.nAccount = nAccount;
		this.accounts = accounts;
	}

	public void withdraw(int accountNumber, int amount){
		int temp = accounts[accountNumber].getBalance();
		temp = temp - amount;
		accounts[accountNumber].setBalance(temp);
	}
	
	public void deposit(int accountNumber, int amount){
		int temp = accounts[accountNumber].getBalance();
		temp = temp + amount;
		accounts[accountNumber].setBalance(temp);
	}

	@Override
	public void run() {
		deposit(nAccount, 50);
		withdraw(nAccount, 50);
		if(accounts[nAccount].getBalance() != 0)
			throw new RuntimeException();
	}
}
