package scheduler.run;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.listener.DDPListenerAdapter;
import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.Pattern.PatternNode;
import scheduler.listener.DDPListenerAdapter.ReadWritePair;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.listener.DDPListenerAdapter.STATUS;

public class RunDDP {
	
	public static final int REPEAT_TIME = 10;
	
	public static void main(String[] args) throws Exception {
		

		List<Pattern> patterns = initPatterns();
		
		
		List<Result> results = RunOneEntry("hashcodetest.HashCodeTest", REPEAT_TIME);
		List<Result> successedResult = new ArrayList<Result>();
		List<Result> failedResult = new ArrayList<Result>();
		for (Result result : results) {
			result.matchPatterns(patterns);
			if (result.success) {
				successedResult.add(result);
			}
			else {
				failedResult.add(result);
			}
			

		}
		System.out.println("failed run: " + failedResult.size());
		for (Result failed : failedResult) {
			System.out.println("-------------------------------------------------------------");
			
//			for (ReadWritePair pair : failed.pairs) {
//				System.out.println(pair);
//			}
			
			List<Pattern> min = null;
			int f = 0, s = 0;
			for (Result successed : successedResult) {
				List<Pattern> current = failed.getMatchedPatternsCopy();
//				Pattern p1 = current.get(0);
//				Pattern p2 = failed.matchedPatterns.get(0);
//				System.out.println(p1);
//				System.out.println(p2);
//				System.out.println(p1.equals(p2));
				
				current.removeAll(successed.matchedPatterns);
				if (min == null || current.size() < min.size()) {
					min = current;
					f = failed.matchedPatterns.size();
					s = successed.matchedPatterns.size();
				}
				
			}
			
//			for (Object obj : failed.mixed) {
//				System.out.println(obj);
//			}
			
			for (Pattern pattern : min) {
				System.out.println(pattern);
//				for (PatternNode node : pattern.nodes) {
//					System.out.println("id:" + node.pair.id + " " + failed.isOnSchedulingPoint(node.pair));
//				}
			}
			System.out.println("f : " + f + " s: " + s + " count: " + min.size());
		}
	}
	
	public static List<Pattern> initPatterns() {
		List<Pattern> patterns = new ArrayList<Pattern>();
		Pattern pattern;
		PatternNode node;
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		return patterns;
	}
	
	public static List<Result> RunOneEntry(String entry, int count) throws Exception {
		File directory = new File("src/examples");
		File[] files = directory.listFiles();
		List<String> searchScope = new ArrayList<String>();
		for (File file : files) {
			if (file.isDirectory()) {
				searchScope.addAll(searchFiles(file));
			}
		}
		
		List<Result> results = new ArrayList<Result>();
		for (int i = 0; i < count; i++) {
			String[] str = new String[]{
					"+classpath=build/examples", 
					"+search.class=scheduler.search.WithoutBacktrack", 
					entry};
			Config config = new Config(str);
			DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.RUN, null, null, null, searchScope);
			JPF jpf = new JPF(config);
			jpf.addPropertyListener(listener);
			jpf.run();
			
			if (!results.contains(listener.result)) {
				results.add(listener.result);
			}
		}
		
		return results;
	}
	
	public static List<String> searchFiles(File file) {
		List<String> result = new ArrayList<String>();
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				result.addAll(searchFiles(f));
			}
		}
		else {
			
			String path = file.getPath().replaceAll("^src\\\\examples\\\\", "")
					.replaceAll("\\\\", "/");
			result.add(path);
		}
		return result;
	}
}
