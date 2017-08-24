public class MyThread extends Thread{
	
	public int publicInt;
	
	private MyThread neighbour;
	
	public MyThread(int number) {
		this.publicInt = number;
	}
	
	public void setNeighbour(MyThread thread) {
		neighbour = thread;
		int a = neighbour.publicInt;
		this.publicInt = a + 1;
	}
	
	public void run() {
		int my = publicInt;
		int n = neighbour.publicInt;
		System.out.println("in thread " + my + ", heighbour: " + n);
	}
}