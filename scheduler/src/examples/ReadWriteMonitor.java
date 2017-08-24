
public class ReadWriteMonitor {
	public static int num1 = 1;
	public int num2;
	private int num3;
	public static void main(String[] args) throws InterruptedException {
		
		Thread t = new Thread() {
			public void run() {
				num1 = 2;
				//int b = num1;
			}
		};
		t.start();
		
		int b = 2 / num1;
		t.join();
	}
}
