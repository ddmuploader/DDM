package scheduler.calculation;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.listener.ConcurrentTestListener;

public class Concurrency {
	public static boolean isConcurrent(String entry,String classPath) {
		String[] str = new String[]{
				"+classpath=" + classPath, 
				"+search.class=scheduler.search.WithoutBacktrack", 
				entry};
		Config config = new Config(str);
		JPF jpf = new JPF(config);
		ConcurrentTestListener listener = new ConcurrentTestListener();
		jpf.addPropertyListener(listener);
		jpf.run();
		
		return listener.isConcurrent;
	}
}
