package scheduler.run.branchalgorithm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.init.PatternInit;
import scheduler.io.FileParser;
import scheduler.listener.DDPListenerAdapter;
import scheduler.listener.DDPListenerAdapter.PathRecorder;
import scheduler.listener.DDPListenerAdapter.PathRecorder.SchedulingPoint;
import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.listener.DDPListenerAdapter.STATUS;
import scheduler.replay.Replay;

public class BranchAlogrithom {
	public static int MAX_TIME = 20;
	public static int DIVIDE_PART = 10;
	public static final int REPEAT_TIME = 5;
	
	public static void main(String[] args) throws Exception {
		List<String> entries = FileParser.getEntries();
		for (String entry : entries) {
			executeMainClass(entry);
		}
//		executeMainClass("hashcodetest.HashCodeTest");
	}
	
	public static void executeMainClass(String entry) throws Exception {
		
		File directory = new File("src/examples");
		File[] files = directory.listFiles();
		List<String> searchScope = new ArrayList<String>();
		for (File file : files) {
			if (file.isDirectory()) {
				searchScope.addAll(Replay.searchFiles(file));
			}
		}
		
		String[] str = new String[]{
				"+classpath=build/examples", 
				"+search.class=scheduler.search.WithoutBacktrack", 
				entry};
		Config config = new Config(str);
		
		/*

		List<List<Pattern>> successList = new ArrayList<List<Pattern>>();
		
	
		List<List<Pattern>> failedList = new ArrayList<List<Pattern>>();
		
		
		List<PathRecorder> failedPath = new ArrayList<PathRecorder>();
		*/
		
		List<Result> successResults = new ArrayList<Result>();
		List<Result> failedResults = new ArrayList<Result>();
		
		for (int i = 0; i < MAX_TIME; i++) {
			JPF jpf = new JPF(config);
			DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.RUN, null, null, null, searchScope);
			jpf.addPropertyListener(listener);
			jpf.run();
			List<Pattern> patterns = PatternInit.initPatterns();
			listener.result.matchPatterns(patterns);
			
			
//			List<Pattern> matchedPatterns = new ArrayList<Pattern>();
//			for (Pattern pattern : listener.result.matchedPatterns) {
//				matchedPatterns.add(pattern.clone());
//			}
			
			if (listener.result.pathRecorder.success == false) {
//				failedPath.add(listener.result.pathRecorder);
//				failedList.add(matchedPatterns);
				failedResults.add(listener.result);
			}
			else {
//				successList.add(matchedPatterns);
				successResults.add(listener.result);
			}
		}
		
		if (failedResults.size() == 0) {
			System.out.println("Did not find the failed run sequence ");
			return;
		}
		
		List<List<Pattern>> minPatternList = new ArrayList<List<Pattern>>();
		
		for (int i = 0; i < failedResults.size(); i++) {
			Result result = failedResults.get(i);

			List<Pattern> patterns = result.matchedPatterns;
			List<Pattern> minPatterns = null;
			for (Result successResult : successResults) {
				List<Pattern> successPatterns = successResult.matchedPatterns;
				List<Pattern> patternsCp = new ArrayList<Pattern>();
				for (Pattern p : patterns) {
					patternsCp.add(p.clone());
				}
				System.out.println("before: " + patternsCp.size());
				patternsCp.removeAll(successPatterns);
				System.out.println("after: " + patternsCp.size());
				if (minPatterns == null || minPatterns.size() > patternsCp.size()) {
					minPatterns = patternsCp;
				}
			}
			minPatternList.add(minPatterns);
		}
		
		for (List<Pattern> patterns : minPatternList) {
			System.out.println("Patterns size: " + patterns.size());
//			for (Pattern p : patterns) {
//				System.out.println(p);
//			}
		}
		
		File parentDir = new File(".\\output\\branchAlogrithm\\" + entry);
		parentDir.delete();
		parentDir.mkdir();
		

		for (int i = 0; i < Math.min(5, failedResults.size()); i++) {
			PathRecorder replayer = failedResults.get(i).pathRecorder;
			DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.REPRODUCE, replayer, null, null, searchScope);
			JPF jpf = new JPF(config);
			jpf.addPropertyListener(listener);
			jpf.run();
			
			int pathLength = replayer.sps.size();
			if (pathLength < DIVIDE_PART) {
				DIVIDE_PART = pathLength;
			}
			HashMap<Integer, Boolean> map = new HashMap<Integer, Boolean>();
			for (int j = 1; j <= DIVIDE_PART; j++) {
				int branchPoint = pathLength * j / DIVIDE_PART - 1;
				boolean allFailed = true;
				for (int k = 0; k < REPEAT_TIME; k++) {
					listener = new DDPListenerAdapter(STATUS.REPRODUCE, replayer, null, null, searchScope, branchPoint);
					jpf = new JPF(config);
					jpf.addPropertyListener(listener);
					jpf.run();
					if (listener.result.success) {
						allFailed = false;
					}
				}
				map.put(branchPoint, allFailed);
			}
			
			List<Entry<Integer, Boolean>> list = new ArrayList<Entry<Integer, Boolean>>(map.entrySet());
			Collections.sort(list, new Comparator<Entry<Integer, Boolean>>() {
				@Override
				public int compare(Entry<Integer, Boolean> o1, Entry<Integer, Boolean> o2) {
					return o1.getKey().compareTo(o2.getKey());
				}
			});
			
			for (Entry<Integer, Boolean> e : list) {
				System.out.println("key: " + e.getKey() + " value: " + e.getValue());
			}
			
			int successStop = -1;
			int failStart = 0;
			for (int j = list.size() - 1; j >= 0; j--) {
				if (!list.get(j).getValue()) {
					successStop = list.get(j).getKey();
					if (j == list.size() - 1) {
						
						failStart = -1;
					}
					else {
						failStart = list.get(j + 1).getKey();
					}
					break;
				}
			}
			if (successStop == -1) {
				
				successStop = 0;
				failStart = list.get(0).getKey();
			}
			
			File dir = new File(parentDir.getPath() + "\\error-" + (i + 1));
			dir.mkdir();
			
			String toFile = "";
			
			System.out.println("successStop: " + successStop + " failStart: " + failStart);
			toFile += "successStop: " + successStop + " failStart: " + failStart + "\n";
			
			SchedulingPoint stop = Replay.getSchedulingPoint(failedResults.get(i).mixed, successStop);
			SchedulingPoint start = Replay.getSchedulingPoint(failedResults.get(i).mixed, failStart);
			System.out.println("successStop: " + stop);
			toFile += "successStop: " + stop + "\n";
			
			System.out.println("failStart: " + start);
			toFile += "failStart: " + start + "\n";
			
			List<Pattern> result = Replay.patternFilter(failedResults.get(i).matchedPatterns, failedResults.get(i).mixed, successStop, failStart);
			System.out.println("before: " + failedResults.get(i).matchedPatterns.size() + " after: " + result.size());
			toFile += "before: " + failedResults.get(i).matchedPatterns.size() + " after: " + result.size() + "\n";
			
			for (Pattern p : result) {
				System.out.println(p);
				toFile += p.toString() + "\n";
			}
			
			FileParser.stringToFile(toFile, dir.getPath() + "\\patterns.txt");
			FileParser.toXML(listener.result.mixed, listener.result.success, dir.getPath() + "\\failedRun.xml");
		}
		
		
//		DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.REPRODUCE, pathRecorder, null, null, searchScope);
//		JPF jpf = new JPF(config);
//		jpf.addPropertyListener(listener);
//		jpf.run();
//		
//		int pathLength = pathRecorder.sps.size();
//		
//		if (pathLength < DIVIDE_PART) {
//			DIVIDE_PART = pathLength;
//		}
//		
//		for (int i = 1; i <= DIVIDE_PART; i++) {
//			int branchPoint = pathLength * i / DIVIDE_PART - 1;
//			
//			for (int j = 0; j < REPEAT_TIME; j++) {
//				listener = new DDPListenerAdapter(STATUS.REPRODUCE, pathRecorder, null, null, searchScope, branchPoint);
//				jpf = new JPF(config);
//				jpf.addPropertyListener(listener);
//				jpf.run();
//				
//				FileParser.toXML(listener.result.pairs, listener.result.success, "F:\\output\\" + entry + "\\branch-" + i + "-" + j + ".xml");
//			}
//		}
	}
}
