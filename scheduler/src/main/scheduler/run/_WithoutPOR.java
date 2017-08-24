package scheduler.run;

import scheduler.io.FileParser;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.replay.Replay;

public class _WithoutPOR {
	public static void main(String[] args) throws Exception {
		
		Result result = null;
		while (result == null || result.success) {
			result = Replay.runWithoutPOR("org.apache.commons.dbcp.Dbcp271");
		}
		FileParser.toXML(result.mixed, false, "su4.xml");
	}
}
