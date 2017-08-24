
public class ScheduleTest extends Thread {
	public static int count = 1;
	public int num;
	public static byte[] lock = new byte[1];
	
	public ScheduleTest(int num) {
		this.num = num;
	}
	
	public static void main(String[] args) throws InterruptedException {
		ScheduleTest t1 = new ScheduleTest(1);
		ScheduleTest t2 = new ScheduleTest(2);
		t1.start();
		t2.start();
		t1.join();
		t2.join();
	}
	
	public void run() {
		synchronized (lock) {
			ScheduleTest.count += 1;
			System.out.println(this.getName() + " line 1. count = " + ScheduleTest.count);
		}
		
		synchronized (lock) {
			if (num == 1) {
				ScheduleTest.count /= 2;
			}
			else {
				ScheduleTest.count += 1;
			}
			System.out.println(this.getName() + " line 2. count = " + ScheduleTest.count);
		}
	}
}
