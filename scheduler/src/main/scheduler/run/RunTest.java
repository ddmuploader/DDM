package scheduler.run;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.listener.FalconImplListener;
import scheduler.listener._UseForTest;
import scheduler.model.ReadWriteNode;
import scheduler.model.SequenceMessage;

public class RunTest {
	public static void main(String[] args) {
		File directory = new File("src/example");
		File[] files = directory.listFiles();
		for (File file : files) {
			System.out.println(file.getPath());
		}
	}
	
}
