package scheduler.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.init.PatternInit;
import scheduler.listener.DDPListenerAdapter;
import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.listener.DDPListenerAdapter.STATUS;
import scheduler.replay.Replay;

public class TestInterruptMode {
	public static String entry = "account.Main";
	
	public static void main(String[] args) throws Exception {
		
		
		int s = 0;
		int f = 0;
		for (int i = 0; i < 100; i++) {
//			boolean r = randomRun();
//			boolean r = interruptOne();
			boolean r = interruptTwo();
			if (!r) {
				s++;
			}
			else {
				f++;
			}
		}
		double rate = ((double)s / (s + f)) * 100;
		System.out.println("Success rate: " + rate + "%");
		
//		DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.INTERRUPT, result.mixed, null, null, null, result.matchedPatterns.get(0));
//		String[] str = new String[]{
//				"+classpath=build/examples", 
//				"+search.class=scheduler.search.WithoutBacktrack", 
//				entry};
//		Config config = new Config(str);
//		JPF jpf = new JPF(config);
//		jpf.addPropertyListener(listener);
//		jpf.run();
//		
//		listener.result.matchPatterns(patterns);
//		System.out.println("-------------------------------");
//		System.out.println(listener.result.matchedPatterns);
	}
	
	public static boolean interruptOne() throws Exception {
		Result result = null;
		while (result == null || result.success == false) {
			String[] str = new String[]{
					"+classpath=build/examples", 
					"+search.class=scheduler.search.WithoutBacktrack", 
					entry};
			Config config = new Config(str);
			DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.RUN, null, null, null, null);
			JPF jpf = new JPF(config);
			jpf.addPropertyListener(listener);
			jpf.run();
			result = listener.result;
		}
//		
		List<Pattern> patterns = PatternInit.initUnicornPatterns();
		result.matchPatterns(patterns);
		System.out.println("result: " + result.matchedPatterns);
//		System.out.println(result.matchedPatterns);
		
//		Random random = new Random();
		
		List<Pattern> interrupts = new ArrayList<Pattern>();
		interrupts.add(result.matchedPatterns.get(result.matchedPatterns.size() - 1));
		
		Result re = Replay.interruptPatterns(entry, result.mixed, interrupts);
		
		re.matchPatterns(patterns);
		System.out.println("re: " + re.matchedPatterns);
		
		boolean contains = false;
		
		for (Pattern p : re.matchedPatterns) {
			if (p.equals(interrupts.get(0))) {
				contains = true;
			}
		}
		
		System.out.println("contains: " + contains);
		return contains;
	}
	
	public static boolean interruptTwo() throws Exception {
		List<Pattern> patterns = PatternInit.initUnicornPatterns();
		Result result = null;
		
		while (result == null || result.matchedPatterns.size() < 2) {
			result = randomRun();
			result.matchPatterns(patterns);
		}
		System.out.println("result: " + result.matchedPatterns);
		
		List<Pattern> toInterrupt = new ArrayList<Pattern>();
		toInterrupt.add(result.matchedPatterns.get(result.matchedPatterns.size() - 1));
		toInterrupt.add(result.matchedPatterns.get(result.matchedPatterns.size() - 2));
		
		Result r = Replay.interruptPatterns(entry, result.mixed, toInterrupt);
		r.matchPatterns(patterns);
		System.out.println("r: " + r.matchedPatterns);
		boolean contains = false;
		for (Pattern p : r.matchedPatterns) {
			if (p.equals(toInterrupt.get(0)) || p.equals(toInterrupt.get(1))) {
				contains = true;
				break;
			}
		}
		return contains;
	}
	
	public static Result randomRun() throws Exception {
		Result result = null;
		while (result == null || result.success == false) {
			String[] str = new String[]{
					"+classpath=build/examples", 
					"+search.class=scheduler.search.WithoutBacktrack", 
					entry};
			Config config = new Config(str);
			DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.RUN, null, null, null, null);
			JPF jpf = new JPF(config);
			jpf.addPropertyListener(listener);
			jpf.run();
			result = listener.result;
		}
//		
		return result;
	}
}
