package scheduler.run;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.init.PatternInit;
import scheduler.init.SearchScopeInit;
import scheduler.io.FileParser;
import scheduler.listener.CoverageAlListenerAdapter;
import scheduler.listener.CoverageAlListenerAdapter.Filter;
import scheduler.listener.CoverageAlListenerAdapter.Pair;
import scheduler.listener.DDPListenerAdapter.PathRecorder;
import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.replay.Replay;


public class RunCoverage {
	public static Set<Pair> pairs;
	public static Map<Integer, Boolean> endStates;
	
	public static String testClass = "defbeforeuse.Main";
	public static String dotFileName = "b.dot";
	public static String gifFileName = "b.gif";
	public static int switchLimit = 400;
	
	public static void main(String[] args) throws Exception {
		String[] str = new String[]{
				"+classpath=build/examples", 
//				"+search.class=scheduler.search.DFSearchWithSwitchLimit", 
				"+search.multiple_errors=true",
//				"+search.switch_limit=" + switchLimit,
//				"+vm.storage.class=",
				testClass};
		Config config = new Config(str);
		CoverageAlListenerAdapter listener = new CoverageAlListenerAdapter(new Filter(null, null, SearchScopeInit.init()));
		JPF jpf = new JPF(config);
		jpf.addPropertyListener(listener);
		jpf.run();
		
		String output = "digraph G {\n";
		for (Integer endState : listener.endStates.keySet()) {
			output += "\t" + endState + "[label=\"" + endState + " " + listener.endStates.get(endState).toString() + "\"]\n";
		}
		for (Pair pair : listener.transitions) {
			output += "\t" + pair.from + " -> " + pair.to;
			
//			String ins = "";
//			if (pair.instructions.size() > 5) {
//				ins += pair.instructions.get(0) + " - " + pair.instructions.get(pair.instructions.size() - 1);
//			}
//			else {
//				for (String s : pair.instructions) {
//					ins += s + ";\\n";
//				}
//			}
//			output += "[label=\"" + pair.thread + ":" + ins + "\"];\n";
			
			output += "[label=\"" +pair.thread + "\"];\n";
//			output += "\n";
			
		}
		output += "}";
		FileParser.stringToFile(output, dotFileName);
		Runtime.getRuntime().exec("D:\\graphviz-2.38\\bin\\dot.exe -T gif " + dotFileName + " -o " + gifFileName);
		
	}
	
	private static Pair findPair(int from, int to) {
		for (Pair pair : pairs) {
			if (pair.from == from && pair.to == to) {
				return pair;
			}
		}
		return null;
	}
	
	private static List<Object> constructMixed(Sequence seq) throws Exception {
		List<Object> result = new ArrayList<Object>();
		for (int i = 0; i < seq.states.size() - 1; i++) {
			Pair pair = findPair(seq.states.get(i), seq.states.get(i + 1));
			if (pair == null) {
				throw new Exception("can't find a transition");
			}
			result.addAll(pair.instructions);
		}
		return result;
	}
	
	public static Set<Sequence> step(int entryState) {
		List<Integer> start = new ArrayList<Integer>();
		start.add(entryState);
		return step(start, "main", 0);
	}
	
	public static Set<Sequence> step(List<Integer> pre, String currentThread, int switchCount) {
		Set<Sequence> sequences = new HashSet<Sequence>();
		if (switchCount > switchLimit) {
//			System.out.println("touch limit.");
			return sequences;
		}
		
		int lastState = pre.get(pre.size() - 1);
		
		for (Pair pair : pairs) {
			if (pair.from == lastState) {
				List<Integer> current = new ArrayList<Integer>();
				current.addAll(pre);
				current.add(pair.to);
				String nextThread = pair.thread;
				sequences.addAll(step(current, nextThread, nextThread.equals(currentThread) ? switchCount : switchCount + 1));
			}
		}
		if (sequences.size() == 0 && endStates.keySet().contains(lastState)) {
			sequences.add(new Sequence(pre, endStates.get(lastState)));
//			System.out.println("one sequence find!");
		}
		return sequences;
	}
	
	public static class Sequence {
		public List<Integer> states;
		public Boolean result;
		
		public Sequence(List<Integer> states, Boolean result) {
			this.states = states;
			this.result = result;
		}
		
		public String toString() {
			String output = "Sequence: ";
			for (Integer t : states) {
				output += t.toString() + " ";
			}
			output += "result: " + result;
			
			return output;
		}
	}
}
