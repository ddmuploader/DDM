package scheduler.run.dd;

import java.util.ArrayList;
import java.util.List;

import scheduler.init.PatternInit;
import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.replay.Replay;

public class DeltaDebugging {
	public static List<Pattern> unicornPatterns = PatternInit.initUnicornPatterns();
	public static String entry = "hashcodetest.HashCodeTest";
	public static String log = "";
	
	public static void main(String[] args) throws Exception {
		Result result = null;
		while (result == null || result.success) {
			result = Replay.run(entry);
		}
		result.matchPatterns(unicornPatterns);
		
		ResultPatternSetPair old = null;
		ResultPatternSetPair pair = reduce(entry, result, result.matchedPatterns);
		log += "iteration: " + result.matchedPatterns.size() + " -> " + pair.patterns.size() + "\n";
		while (old == null || pair.patterns.size() < old.patterns.size()) {
			old = pair;
			pair = reduce(entry, pair.result, pair.patterns);
			log += "iteration: " + old.patterns.size() + " -> " + pair.patterns.size() + "\n";
		}
		
		System.out.println("-------------------result------------------\n" + pair.patterns);
		System.out.println("before: " + result.matchedPatterns.size() + ", after: " + pair.patterns.size());
		System.out.println("------------log-------------\n" + log);
	}
	
	public static ResultPatternSetPair reduce(String entry, Result orig, List<Pattern> patternSet) throws Exception {
		List<Pattern> interruptedPatterns;
		for (Pattern p : patternSet) {
			Result after = Replay.interruptPattern(entry, orig.mixed, p);
			after.matchPatterns(unicornPatterns);
			
			interruptedPatterns = sub(patternSet, after.matchedPatterns);
			
			
			if (after.success) {
				log += "new run success after size: " + after.matchedPatterns.size() + "\n";
				log += "p:\n" + p + "\n";
//				log += "after:\n" + after.matchedPatterns + "\n";
				return new ResultPatternSetPair(orig, interruptedPatterns);
			}
		}
//		return new ResultPatternSetPair(orig, patternSet);
//		
		for (Pattern p : patternSet) {
			Result after = Replay.interruptPattern(entry, orig.mixed, p);
			after.matchPatterns(unicornPatterns);
			
			interruptedPatterns = sub(orig.matchedPatterns, after.matchedPatterns);
			
			
			if (after.success) {
				return new ResultPatternSetPair(orig, interruptedPatterns);
			}
			else {
				return new ResultPatternSetPair(after, sub(orig.matchedPatterns, interruptedPatterns));
			}
		}
		return null;
	}
	
	
	public static List<Pattern> sub(List<Pattern> left, List<Pattern> right) {
		List<Pattern> result = new ArrayList<Pattern>();
		for (Pattern p : left) {
			if (!right.contains(p)) {
				result.add(p);
			}
		}
		System.out.println("++left: \n" + left + "\n++right: \n" + right + "\n++result: \n" + result);
		return result;
	}
	
	public static class ResultPatternSetPair {
		public Result result;
		public List<Pattern> patterns;
		
		public ResultPatternSetPair(Result result, List<Pattern> patterns) {
			this.result = result;
			this.patterns = patterns;
		}
	}
}
