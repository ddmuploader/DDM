package scheduler.run;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import scheduler.calculation.Concurrency;
import scheduler.enumerate.TestResult;
import scheduler.init.EntryInit;
import scheduler.init.PatternInit;
import scheduler.io.FileParser;
import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.replay.Replay;

public class _Test {
	public static String entry = "critical.Critical";
	static List<Pattern> include = new ArrayList<Pattern>();
	static List<Pattern> exclude = new ArrayList<Pattern>();
	static Result fail = null;
	static Result pass = null;
	static int MAX_RUNNING_COUNT = 100;
	
	public static void main(String[] args) throws Exception {
		System.out.println(Concurrency.isConcurrent("org.apache.catalina.ha.deploy.FileMessageFactory",".settings/output"));
		
		
	}
	
	
	private static void SubSetAlo(List a, int[] flag, List subset, int n, int start) throws Exception {

		if(start >= n)
			return;
		

		if(subset.size() !=0 && subset.size() != n){
			for(int j = 0; j < subset.size(); j++)
				exclude.add((Pattern) subset.get(j));
			for(int j = 0;j < a.size(); j++)
				include.add((Pattern) a.get(j));
			include.removeAll(exclude);
			System.out.println("exclude" + exclude);
			System.out.println("include" + include);

			exclude.clear();
			include.clear();
		} 
		
		System.out.println();
		System.out.println("-------");
		for(int i = 0; i < n; i++){
			if(flag[i] == 0){
				flag[i] = 1;
				subset.add(a.get(i));
				SubSetAlo(a, flag, subset, n, i + 1);
				flag[i] = 0;
				subset.remove(a.get(i));
			}
				
		}
	}
}
