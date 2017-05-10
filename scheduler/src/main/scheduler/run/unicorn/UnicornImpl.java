package scheduler.run.unicorn;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import scheduler.init.PatternInit;
import scheduler.io.FileParser;
import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.replay.Replay;

/*
 * UNICORN implementation
 */
public class UnicornImpl {
	public static final int REPEAT_TIME = 100;
	public static final String path = ""; /* add your output path here */
	public static final String entry = "org.apache.commons.pool.impl.TestGenericObjectPool"; /* your test class */
	
	public static Map<Pattern, Integer> failedPatterns = new HashMap<Pattern, Integer>();
	public static Map<Pattern, Integer> successPatterns = new HashMap<Pattern, Integer>();
	public static int totalFailed = 0;
	public static int totalSuccess = 0;
	
	public static void main(String[] args) throws Exception {
		for (int i = 1; i <= 3; i++) {
			oneExecution(i);
		}
	}
	
	private static void oneExecution(int name) throws Exception {
		String output = "";
		for (int i = 0; i < REPEAT_TIME; i++) {
			Result result = Replay.runWithoutScope(entry);
			List<Pattern> patterns = PatternInit.initUnicornPatterns();
			result.matchPatterns(patterns);
			Map<Pattern, Integer> map;
			if (result.success) {
				map = successPatterns;
				totalSuccess++;
			}
			else {
				map = failedPatterns;
				totalFailed++;
			}
			
			for (Pattern pattern : result.matchedPatterns) {
				boolean isInSet = false;
				for (Pattern p : map.keySet()) {
//					if (p.equals(pattern)) {
					if (p.isSamePattern(pattern)) {
						isInSet = true;
						map.put(p, map.get(p) + 1);
						break;
					}
				}
				
				if (isInSet == false) {
					map.put(pattern, 1);
				}
			}
		}
//		System.out.println("success Patterns:");
//		for (Pattern p : successPatterns.keySet()) {
//			System.out.println(p + "\n" + "times: " + successPatterns.get(p));
//		}
//		
//		System.out.println("failed Patterns:");
//		for (Pattern p : failedPatterns.keySet()) {
//			System.out.println(p + "\n" + "times: " + failedPatterns.get(p));
//		}
		
		Map<Pattern, Double> suspe = new HashMap<Pattern, Double>(); 
		Map<Pattern, Double> suspeClone = new HashMap<Pattern, Double>(); 
		
		for (Pattern p : failedPatterns.keySet()) {
//			System.out.println("---------------------------------------------");
//			System.out.println(p);
			double passedP = (double)getSuccessCount(p) / totalSuccess;
			double failedP = (double)getFailedCount(p) / totalFailed;
			
			double sus = failedP / (failedP + passedP);
			suspe.put(p, sus);
//			System.out.println(MessageFormat.format("suspiciousness: ({0} / {1}) / ({0} / {1} + {2} / {3}) = {4}", 
//					getFailedCount(p), totalFailed, getSuccessCount(p), totalSuccess, sus));
		}
		List<Pattern> rank = new ArrayList<Pattern>();
		while (!suspe.isEmpty()){
			Pattern biggest = null;
			for (Pattern p : suspe.keySet()) {
				if (biggest == null || suspe.get(p) > suspe.get(biggest)) {
					biggest = p;
				}
			}
			rank.add(biggest);
			suspeClone.put(biggest, suspe.get(biggest));
			suspe.remove(biggest);
		}
		int count = 0;
		for (Pattern p : rank) {
			System.out.println("------------------------");
			System.out.println(p);
			System.out.println("sus: " + suspeClone.get(p));
			output += "--------------------\n";
			output += p + "\n";
			output += "sus: " + suspeClone.get(p) + "\n";
			if (suspeClone.get(p) >= 0.999) {
				count++;
			}
		}
		
		System.out.println("number of sus 1: " + count);
		output += "number of sus 1: " + count;
		
		toFile(path + entry, name + ".txt", output);
	}
	
	public static Integer getFailedCount(Pattern pattern) {
		for (Pattern p : failedPatterns.keySet()) {
			if (p.isSamePattern(pattern)) {
				return failedPatterns.get(p);
			}
		}
		return 0;
	}
	
	public static Integer getSuccessCount(Pattern pattern) {
		for (Pattern p : successPatterns.keySet()) {
			if (p.isSamePattern(pattern)) {
				return successPatterns.get(p);
			}
		}
		return 0;
	}
	
	public static void toFile(String path, String fileName, String content) throws IOException {
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		FileParser.stringToFile(content, path + "\\" + fileName);
	}
}
