package datarace;

public class CustomerInfo {

	private int nAccount;
	private Account[] accounts;
	
	public CustomerInfo() {
		super();
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
}
