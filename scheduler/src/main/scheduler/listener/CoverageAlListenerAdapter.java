package scheduler.listener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.Step;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Transition;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.bytecode.FieldInstruction;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;

public class CoverageAlListenerAdapter extends PropertyListenerAdapter {
	
	public Set<Pair> transitions;
	public Map<Integer, Boolean> endStates;
	public Filter filter;
	
	
	//private Pair currentPair;
	private List<Object> instructions;
	public Integer lastState;
	
	public CoverageAlListenerAdapter(Filter filter) {
		lastState = null;
		transitions = new HashSet<Pair>();
		instructions = new ArrayList<Object>();
		endStates = new HashMap<Integer, Boolean>();
		this.filter = filter;
	}
	
	public void stateAdvanced (Search search) {
		int currentState = search.getStateId();
		Transition transition = search.getTransition();
		if (lastState != null) {
			List<String> instructions = new ArrayList<String>();
			Iterator<Step> iter = transition.iterator();
			while (iter.hasNext()) {
				Step step = iter.next();
				if (step == null) {
					continue;
				}
				instructions.add(step.getLineString());
			}
//			currentPair = new Pair(lastState, currentState, transition.getThreadInfo().getName());
//			transitions.add(currentPair);
			Pair currentPair = new Pair(lastState, currentState, transition.getThreadInfo().getName(), this.instructions);
			transitions.add(currentPair);
			this.instructions = new ArrayList<Object>();
		}
		lastState = currentState;
		
		if (search.isErrorState()) {
			endStates.put(search.getStateId(), false);
		}
		
		if (search.isEndState()) {
			endStates.put(search.getStateId(), !search.isErrorState());
		}
	}
	
	public void stateBacktracked(Search search) {
		lastState = search.getStateId();
	}
	
	public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction) {
		if (executedInstruction instanceof FieldInstruction) {
			FieldInstruction fins = (FieldInstruction)executedInstruction;
			FieldInfo fi = fins.getFieldInfo();
			ElementInfo ei = fins.getElementInfo(currentThread);
			
			if (filter.instanceFilter != null && !ei.toString().contains(filter.instanceFilter)) {
				return;
			}
			if (filter.fieldFilter != null && !fi.getName().contains(filter.fieldFilter)) {
				return;
			}
			if (filter.fileFilter != null) {
				boolean contains = false;
				for (String file : filter.fileFilter) {
					if (fins.getFileLocation().matches(file + ":[0-9]+")) {
						contains = true;
						break;
					}
				}
				if (!contains) {
					return;
				}
			}
			
			String type = null;
			
			if (fins.isRead()) {
				type = "READ";
			}
			else {
				type = "WRITE";
			}
			
			ReadWritePair pair = new ReadWritePair(ei.toString(), fi.getName(), type, currentThread.getName(), fins.getFileLocation(), false);
			this.instructions.add(pair);
		}
	}
	
	@Override
	public void choiceGeneratorAdvanced (VM vm, ChoiceGenerator<?> currentCG) {
		//System.out.println("cg advanced.");
		if (currentCG.isSchedulingPoint()) {
			ThreadInfo[] threads = ((ThreadChoiceFromSet) currentCG).getAllThreadChoices();
			if (threads.length == 1) {
				return;
			}
			ThreadInfo t = (ThreadInfo)currentCG.getNextChoice();
			Instruction insn = t.getPC();
			String type = insn.getClass().getName();
//			result.addSchedulingPoint(t.getName(), insn.getFileLocation(), type);
			this.instructions.add(new SchedulingPoint(t.getName(), insn.getFileLocation(), type));
		}
	}
	
	public class Pair {
		public int from;
		public int to;
		public String thread;
		public List<Object> instructions;
		
		public Pair(int from, int to, String thread, List<Object> instructions) {
			this.from = from;
			this.to= to;
			this.thread = thread;
			this.instructions = instructions;
		}
	}
	
	public static class SchedulingPoint {
		public Integer id;
		public String nextThread;
		public String nextInstruction;
		public String nextInstructionType;
		
		public SchedulingPoint(String thread, String insn, String type) {
			nextThread = thread;
			nextInstruction = insn;
			nextInstructionType = type;
			id = -1;
		}
		
		public String toString() {
			return "id: " + id + "\tnextThread: " + nextThread + "\tnextInstruction: " + nextInstruction
					+ "\tnextInstructionType: " + nextInstructionType;
		}
		
		public boolean equals(String thread, String insn, String type) {
			return this.nextThread.equals(thread) && this.nextInstruction.equals(insn) 
					&& this.nextInstructionType.equals(type);
		}
		
		public boolean equals(Object osp) {
			if (!(osp instanceof SchedulingPoint)) {
				return false;
			}
			SchedulingPoint sp = (SchedulingPoint)osp;
			return this.equals(sp.nextThread, sp.nextInstruction, sp.nextInstructionType);
		}
		
		public boolean identical(Object obj) {
			if (!(obj instanceof SchedulingPoint)) {
				return false;
			}
			
			SchedulingPoint sp = (SchedulingPoint)obj;
			return this.equals(sp.nextThread, sp.nextInstruction, sp.nextInstructionType) && this.id == sp.id;
		}
		
		public SchedulingPoint clone() {
			SchedulingPoint sp = new SchedulingPoint(this.nextThread, this.nextInstruction, this.nextInstructionType);
			sp.id = this.id;
			return sp;
		}
	}
	
	public static class ReadWritePair {
		public Integer id;
		public String instance;
		public String field;
		public String type;
		public String thread;
		public String location;
		public Boolean inBranch;
		
		public ReadWritePair(String instance, String field, String type, String thread, String location, Boolean inBranch) {
			this.id = null;
			this.instance = instance;
			this.field = field;
			this.type = type;
			this.thread = thread;
			this.location = location;
			this.inBranch = inBranch;
		}
		
		public boolean equals(Object objPair) {
			
			if (!(objPair instanceof ReadWritePair)) {
				return false;
			}
			ReadWritePair pair = (ReadWritePair)objPair;
			
			return this.instance.equals(pair.instance) && this.field.equals(pair.field)
					&& this.type.equals(pair.type) && this.thread.equals(pair.thread)
					&& this.location.equals(pair.location);
		}
		
		public boolean identical(Object obj) {
			return this.equals(obj) && this.id == ((ReadWritePair)obj).id;
		}
		
		public ReadWritePair clone() {
			ReadWritePair pair = new ReadWritePair(this.instance, this.field, this.type, this.thread, this.location, this.inBranch);
			pair.id = this.id;
			return pair;
		}
		
		public String toString() {
			String id = "";
			if (this.id != null) {
				id = "id: " + this.id.toString() + "\t";
			}
			return id + "instance: " + this.instance + "\tfield: " + this.field + "\ttype: " + this.type
					+ "\tthread: " + this.thread + "\tlocation: " + this.location + "\tinBranch: " + this.inBranch;
		}
	}
	
	public static class Filter {
		public String instanceFilter;
		public String fieldFilter;
		public List<String> fileFilter;
		
		public Filter() {
			this.instanceFilter = null;
			this.fieldFilter = null;
			this.fileFilter = null;
		}
		
		public Filter(String instanceFilter, String fieldFilter, List<String> files) {
			this.instanceFilter = instanceFilter;
			this.fieldFilter = fieldFilter;
			this.fileFilter = files;
		}
	}
}
