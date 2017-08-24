import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpf.vm.Verify;
import scheduler.model.ReadWriteNode;

public class Test{
	public static int a = 0;
	
	public static void main(String[] args) throws Exception {
//		Thread t = new Thread(){
//			public void run() {
//				Test.a = 1;
//			}
//		};
//		t.start();
//		Test.a = 2;
//		t.join();
//		Test.a += 1;
//		if (Test.a == 2) {
//			throw new Exception("a should be 2. bug found.");
//		}
		System.out.println(Verify.randomBool());
	}
}
