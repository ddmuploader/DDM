package scheduler.run;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.listener.MonitorListenerAdapter;
import scheduler.listener.MonitorListenerAdapter.ReadWritePair;

public class ShareFieldTest {
	public static void main(String[] args) {
		String[] str = new String[]{
				"+classpath=build/examples", 
				"+search.class=scheduler.search.WithoutBacktrack", 
				"ShareVars"};
		Config config = new Config(str);
		JPF jpf = new JPF(config);
		MonitorListenerAdapter listener = new MonitorListenerAdapter(null, "number", null);
		jpf.addPropertyListener(listener);
		jpf.run();
		for (ReadWritePair pair : listener.result.pairs) {
			System.out.print("instance: " + pair.instance);
			System.out.print("\tfield: " + pair.field);
			System.out.print("\ttype: " + pair.type);
			System.out.print("\tthread: " + pair.thread);
			System.out.println("\tlocation: " + pair.location);
		}
	}
}
