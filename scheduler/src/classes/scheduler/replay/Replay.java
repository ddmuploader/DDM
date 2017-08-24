package scheduler.replay;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.enumerate.TestResult;
import scheduler.init.PatternInit;
import scheduler.init.SearchScopeInit;
import scheduler.listener.CoverageAlListenerAdapter;
import scheduler.listener.DDPListenerAdapter;
import scheduler.listener.DDPListenerAdapter.PathRecorder;
import scheduler.listener.DDPListenerAdapter.PathRecorder.SchedulingPoint;
import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.Pattern.PatternNode;
import scheduler.listener.DDPListenerAdapter.ReadWritePair;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.listener.DDPListenerAdapter.STATUS;
import scheduler.run.RunDD;

public class Replay {
//	public static Resulun(PathRecorder pathRecorder, String entry) throws Exception {
//		File directory = new File("src/examples");
//		File[] files = directory.listFiles();
//		List<String> searchScope = new ArrayList<String>();
//		for (File file : files) {
//			if (file.isDirectory()) {
//				searchScope.addAll(searchFiles(file));
//			}
//		}
//		String[] str = new String[]{
//				"+classpath=build/examples", 
//				"+search.class=scheduler.search.WithoutBacktrack", 
//				entry};
//		Config config = new Config(str);
//		DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.REPRODUCE, pathRecorder, null, null, searchScope);
//		JPF jpf = new JPF(config);
//		jpf.addPropertyListener(listener);
//		jpf.run();
//		
//		return listener.result;
//	}
	
	public static SchedulingPoint getSchedulingPoint(List<Object> mixed, int index) {
		int counter = 0;
		for (Object obj : mixed) {
			if (obj instanceof SchedulingPoint) {
				if (index == counter) {
					return (SchedulingPoint)obj;
				}
				else {
					counter++;
				}
			}
		}
		
		return null;
	}
	
	public static List<Pattern> patternFilter(List<Pattern> patterns, List<Object> mixed, int successStop, int failStart) throws Exception {
		List<Pattern> result = new ArrayList<Pattern>();
		for (Pattern pattern : patterns) {
			result.add(pattern.clone());
		}
		
		if (failStart != -1) {
			for (Pattern p : patterns) {
				eachPattern:
				for (PatternNode node : p.nodes) {
					if (isAfter(mixed, node.pair, failStart)) {
						result.remove(p);
						break eachPattern;
					}
				}
			}
		}
		
		if (successStop != 0) {
			for (Pattern p : patterns) {
				boolean allBefore = true;
				for (PatternNode node : p.nodes) {
					if (!isBefore(mixed, node.pair, successStop)) {
						allBefore = false;
						break;
					}
				}
				
				if (allBefore) {
					result.remove(p);
				}
			}
		}
		
		return result;
	}
	
	private static boolean isAfter(List<Object> mixed, ReadWritePair pair, int spIndex) throws Exception {
		SchedulingPoint sp = getSchedulingPoint(mixed, spIndex);
		int i;
		for (i = 0; i < mixed.size(); i++) {
			if (sp.identical(mixed.get(i))) {
				break;
			}
		}
		if (i == mixed.size()) {
			throw new Exception("schedulingPoint not found.");
		}
		
		for (int j = i + 1; j < mixed.size(); j++) {
			if (pair.identical(mixed.get(j))) {
				return true;
			}
		}
		return false;
	}
	private static boolean isBefore(List<Object> mixed, ReadWritePair pair, int spIndex) throws Exception {
		SchedulingPoint sp = getSchedulingPoint(mixed, spIndex);
		for (int i = 0; i < mixed.size(); i++) {
			if (pair.identical(mixed.get(i))) {
				return true;
			}
			
			if (sp.identical(mixed.get(i))) {
				return false;
			}
		}
		
		throw new Exception("schedulingPoint not found.");
	}
	
	public static List<String> searchFiles(File file) {
		List<String> result = new ArrayList<String>();
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				result.addAll(searchFiles(f));
			}
		}
		else {
			String path = file.getPath().replaceAll("D:\\\\eclipse src\\\\Dbcp4\\\\src\\\\", "")
					.replaceAll("\\\\", "/");
			result.add(path);
		}
		return result;
	}
	
	public static boolean expectPatterns(List<Pattern> patterns, List<Pattern> include, List<Pattern> exclude, StringBuffer buffer) {
		boolean result = true;
		for (Pattern pattern : include) {
			if (!patterns.contains(pattern)) {
				result = false;
			}
		}
		if (buffer != null) {
			buffer.append("patterns:\n-------------------------------------------\n");
			for (Pattern p : patterns) {
				buffer.append(p.toString() + "\n");
			}
			buffer.append("include:\n-------------------------------------------\n");
			for (Pattern p : include) {
				buffer.append(p.toString() + "\n");
			}
			
			buffer.append(MessageFormat.format("is include: {0}\n", result));
			
			buffer.append("exclude: size=" + exclude.size() + "\n-------------------------------------------\n");
		}
		
		for (Pattern pattern : exclude) {
			if (patterns.contains(pattern)) {
				result = false;
			}
		}
		if (buffer != null) {
			for (Pattern p : exclude) {
				buffer.append(p.toString() + "\n");
			}
			buffer.append(MessageFormat.format("is exclude: {0}\n", result));
		}
		
		return result;
	}
	
	public static TestResult test(String entry, List<Pattern> exclude, List<Pattern> include, List<Object> mixed, Result pass) throws Exception {
		
		List<Object> after = new ArrayList<Object>();
		try {
			
			 after = Result.interruptPatterns(exclude, mixed);
			
			
		} catch (Exception e) {
			System.out.println("unknwon");
			return TestResult.UNKNOWN;
		}
		
		
		if (after == null) {
			System.out.println("unknown");
			return TestResult.UNKNOWN;
		}
		

		PathRecorder replayer = toPathRecorder(after);
	
		Result result = tryRun(entry, replayer);
		
		if(result.finished == false){
			System.out.println("unknown");
			return TestResult.UNKNOWN;
		}
		
		List<Pattern> patterns = PatternInit.initPatterns();

		result.matchPatterns(patterns);
		

		List<Pattern> u = new ArrayList<Pattern>();
		for (Pattern pattern : exclude) {
			u.add(pattern);
		}
		for (Pattern pattern : include) {
			u.add(pattern);
		}
		

		List<Pattern> f = new ArrayList<Pattern>();
		for (Pattern pattern : u) {
			f.add(pattern);
		}
		f.removeAll(result.matchedPatterns);
		
		boolean flag = true;
		if(f.size() >= exclude.size())
			flag = false;
		for (Pattern pattern : f) {
			if(!exclude.equals(pattern)){
				flag = false;
			}
		}
		TestResult testResult = TestResult.UNKNOWN;
		List<Pattern> R = new ArrayList<Pattern>();
		try {

			if(flag == true){
				System.out.println("unknown");
				if(result.success == true){

					List<Pattern> complement = RunDD.Complement(u, result.matchedPatterns);
					testResult.result.matchedPatterns = complement;
					return testResult;
				}
				else{

					List<Pattern> complement = RunDD.Complement(result.matchedPatterns, pass.matchedPatterns);
					testResult.result.matchedPatterns = complement;
					return testResult;
				}
			}
		} catch (Exception e) {
			System.out.println("unknown");
			return TestResult.UNKNOWN;
		}
		
		
		boolean flag2 = true;
		if(exclude.size() >= f.size())
			flag2 = false;
		for (Pattern pattern : exclude) {
			if(!f.equals(pattern))
				flag2 = false;
		}
		
		if(flag2 == true){
			System.out.println("unknown");
			return TestResult.UNKNOWN;
		}
		
		if(result.success == false){
			System.out.println("fail");
			return TestResult.FAIL;
		}
			
		else{
			System.out.println("success");
			return TestResult.SUCCESS; 
		}
		
		
		/*boolean accord = Replay.expectPatterns(result.matchedPatterns, include, exclude, null);
		if (accord == false) {
			TestResult testResult = TestResult.UNKNOWN;
			testResult.result = result;
			return testResult;
		}
		else {
			return result.success ? TestResult.SUCCESS : TestResult.FAIL;
		}*/
	}
	

	public static PathRecorder toPathRecorder(List<Object> mixed) {
		PathRecorder pathRecorder = new PathRecorder();
		pathRecorder.mixed = mixed;
		for (Object obj : mixed) {
			if (obj instanceof SchedulingPoint) {
				SchedulingPoint sp = (SchedulingPoint)obj;
				pathRecorder.addSchedulingPoint(sp.id, sp.nextThread, sp.nextInstruction, sp.nextInstructionType);
			}
			else if (obj instanceof CoverageAlListenerAdapter.SchedulingPoint) {
				CoverageAlListenerAdapter.SchedulingPoint sp = (CoverageAlListenerAdapter.SchedulingPoint)obj;
				pathRecorder.addSchedulingPoint(sp.id, sp.nextThread, sp.nextInstruction, sp.nextInstructionType);
			}
		}
		return pathRecorder;
	}
	
	public static List<ReadWritePair> toReadWritePairs(List<Object> mixed) {
		List<ReadWritePair> pairs = new ArrayList<ReadWritePair>();
		for (Object obj : mixed) {
			if (obj instanceof ReadWritePair) {
				ReadWritePair pair = (ReadWritePair)obj;
				pairs.add(pair);
			}
		}
		return pairs;
	}
	
	public static Result run(String entry) throws Exception {
		File directory = new File("Dbcp4/src/org/apache/commons/dbcp");
		File[] files = directory.listFiles();
		List<String> searchScope = new ArrayList<String>();
		for (File file : files) {
			if (file.isDirectory()) {
				searchScope.addAll(searchFiles(file));
			}
		}
		String[] str = new String[]{
//				"+classpath=build/examples", 
				"+classpath= Dbcp4/bin;"
				+ "Dbcp4/realLib/javaee.jar;"
				+ "Dbcp4/realLib/jacontebe-1.0.jar;"
				+ "Dbcp4/jdmkrt.jar;"
				+ "Dbcp4/realLib/coring-1.4.jar;"
				+ "Dbcp4/realLib/commons-collections-2.1.jar;"
				+ "Dbcp4/realLib/commons-pool-1.2.jar;"
				+ "Dbcp4/realLib/mockito-all-1.9.5.jar",
				"+search.class=scheduler.search.WithoutBacktrack", 
				entry};
		Config config = new Config(str);
		DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.RUN, null, null, null, searchScope);
		JPF jpf = new JPF(config);
		jpf.addPropertyListener(listener);
		jpf.run();
		
		return listener.result;
	}
	
	public static Result runWithoutPOR(String entry) throws Exception {
		File directory = new File("Dbcp4/src/org/apache/commons/dbcp");
		File[] files = directory.listFiles();
		List<String> searchScope = new ArrayList<String>();
		for (File file : files) {
			if (file.isDirectory()) {
				searchScope.addAll(searchFiles(file));
			}
		}
		String[] str = new String[]{
//				"+classpath=build/examples", 
				"+classpath= Dbcp4/bin;"
				+ "Dbcp4/realLib/javaee.jar;"
				+ "Dbcp4/realLib/jacontebe-1.0.jar;"
				+ "Dbcp4/jdmkrt.jar;"
				+ "Dbcp4/realLib/coring-1.4.jar;"
				+ "Dbcp4/realLib/commons-collections-2.1.jar;"
				+ "Dbcp4/realLib/commons-pool-1.2.jar;"
				+ "Dbcp4/realLib/mockito-all-1.9.5.jar",
				"+search.class=scheduler.search.WithoutBacktrack", 
				"+vm.scheduler.sharedness.class=gov.nasa.jpf.vm.GlobalSharednessPolicy",
				entry};
		Config config = new Config(str);
		DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.RUN, null, null, null, searchScope);
		JPF jpf = new JPF(config);
		jpf.addPropertyListener(listener);
		jpf.run();
		
		return listener.result;
	}
	
	public static Result tryRun(String entry, PathRecorder pathRecorder) throws Exception {
		List<String> searchScope = SearchScopeInit.init();

		String[] str = new String[]{
//				"+classpath=build/examples", 
				"+classpath= Dbcp4/bin;"
				+ "Dbcp4/realLib/javaee.jar;"
				+ "Dbcp4/realLib/jacontebe-1.0.jar;"
				+ "Dbcp4/jdmkrt.jar;"
				+ "Dbcp4/realLib/coring-1.4.jar;"
				+ "Dbcp4/realLib/commons-collections-2.1.jar;"
				+ "Dbcp4/realLib/commons-pool-1.2.jar;"
				+ "Dbcp4/realLib/mockito-all-1.9.5.jar",
				"+search.class=scheduler.search.WithoutBacktrack", 
				entry};
		Config config = new Config(str);
		DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.REPRODUCE, pathRecorder, null, null, /*searchScope*/null);
		JPF jpf = new JPF(config);
		jpf.addPropertyListener(listener);
		jpf.run();
		
		System.out.println("Whether to follow the original path");
		System.out.println(listener.result.finished);
//		System.out.println("The results of this operation");
//		System.out.println(listener.result.success);
		
		return listener.result;
	}
	
	
	public static SchedulingPoint findLastSchedulingPoint(List<Object> mixed, ReadWritePair in) {
		SchedulingPoint result = null;
		for (Object obj : mixed) {
			if (obj instanceof SchedulingPoint) {
				result = (SchedulingPoint)obj;
			}
			else {
				ReadWritePair rw = (ReadWritePair)obj;
				//TODO modified
//				if (rw.identical(in)) {
				if (rw.equals(in)) {
					return result;
				}
			}
		}
		return null;
	}
	
	public static Result interruptPattern(String entry, List<Object> mixed, Pattern pattern) throws Exception {
		DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.INTERRUPT, mixed, null, null, null, pattern);
		String[] str = new String[]{
//				"+classpath=build/examples", 
				"+classpath= Dbcp4/bin;"
				+ "Dbcp4/realLib/javaee.jar;"
				+ "Dbcp4/realLib/jacontebe-1.0.jar;"
				+ "Dbcp4/jdmkrt.jar;"
				+ "Dbcp4/realLib/coring-1.4.jar;"
				+ "Dbcp4/realLib/commons-collections-2.1.jar;"
				+ "Dbcp4/realLib/commons-pool-1.2.jar;"
				+ "Dbcp4/realLib/mockito-all-1.9.5.jar",
				"+search.class=scheduler.search.WithoutBacktrack",  
				entry};
		Config config = new Config(str);
		JPF jpf = new JPF(config);
		jpf.addPropertyListener(listener);
		jpf.run();
		
		return listener.result;
		
//		listener.result.matchPatterns(patterns);
//		System.out.println("-------------------------------");
//		System.out.println(listener.result.matchedPatterns);
	}
	
	public static Result interruptPatterns(String entry, List<Object> mixed, List<Pattern> patterns) throws Exception {
		Result result = null;
		for (Pattern pattern : patterns) {
			if (result != null) {
				result.matchPatterns(PatternInit.initUnicornPatterns());
				if (contains(result.matchedPatterns, pattern)) {
					result = interruptPattern(entry, result.mixed, pattern);
				}
			}
			else {
				result = interruptPattern(entry, mixed, pattern);
			}
		}
		return result;
	}
	
	public static boolean contains(List<Pattern> patterns, Pattern pattern) {
		for (Pattern p : patterns) {
			if (pattern.equals(p)) {
				return true;
			}
		}
		return false;
	}
}
