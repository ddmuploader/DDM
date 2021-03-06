package scheduler.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.bytecode.FieldInstruction;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;
import scheduler.init.PatternInit;
import scheduler.listener.DDPListenerAdapter.PathRecorder;
import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.ReadWritePair;
import scheduler.listener.DDPListenerAdapter.PathRecorder.SchedulingPoint;
import scheduler.listener.DDPListenerAdapter.Pattern.PatternNode;
import scheduler.replay.Replay;

public class DDPListenerAdapter extends PropertyListenerAdapter {
	

	public STATUS status;
	

	public Result result;
	
	
	public PathRecorder pathReplay;
	

	public Filter filter;
	
	private boolean inBranch;
	
	private Pattern exclude;
	
	/*

	public DDPListenerAdapter(STATUS status) throws Exception {
		this.status = status;
		if (this.status == STATUS.RUN) {
			this.pathRecorder = new PathRecorder();
			this.result = null;
			this.filter = null;
			this.inBranch = false;
		}
		else {
			new Exception("reproduce status should use another construction");
		}
	}
	*/
	
	public DDPListenerAdapter(STATUS status, PathRecorder pathRecorder, String instanceFilter, String fieldFilter, List<String> files) throws Exception {
		this.status = status;
		if (this.status == STATUS.REPRODUCE) {
			this.result = new Result(true);
			this.result.pathRecorder = new PathRecorder();
			this.pathReplay = pathRecorder;
			this.pathReplay.reset();
			
			this.filter = new Filter(instanceFilter, fieldFilter, files);
		}
		else {
			//throw new Exception("run status should use another construction");
			this.result = new Result(true);
			this.result.pathRecorder = new PathRecorder();
			this.filter = new Filter(instanceFilter, fieldFilter, files);
			this.inBranch = false;
			this.pathReplay = null;
		}
	}
	

	public DDPListenerAdapter(STATUS status, PathRecorder pathRecorder, String instanceFilter, String fieldFilter, List<String> files, int branchPoint) throws Exception {
		this.status = status;
		if (this.status == STATUS.REPRODUCE) {
			this.result = new Result(true);
			this.result.pathRecorder = new PathRecorder();
			this.pathReplay = pathRecorder;
			this.pathReplay.reset();
			this.result.branchPoint = branchPoint;
			this.filter = new Filter(instanceFilter, fieldFilter, files);
		}
		else {
			throw new Exception("run status should use another construction");
		}
	}
	
	public DDPListenerAdapter(STATUS status, List<Object> mixed, String instanceFilter, String fieldFilter, List<String> files, Pattern exclude) throws Exception {
		this.status = status;
		if (status != STATUS.INTERRUPT) {
			throw new Exception("to use this construction, status should be INTERRUPT");
		}
		
		this.result = new Result(true);
		this.result.pathRecorder = new PathRecorder();
		this.pathReplay = Replay.toPathRecorder(mixed);
		this.pathReplay.reset();
		this.filter = new Filter(instanceFilter, fieldFilter, files);
		this.exclude = exclude;
		
		this.pathReplay.boundary = Replay.findLastSchedulingPoint(pathReplay.mixed, exclude.nodes.get(0).pair);
		this.pathReplay.next_avoid_thread = exclude.nodes.get(0).pair.thread;
		this.pathReplay.next_prefer_thread = exclude.nodes.get(1).pair.thread;
	}
	
	public enum STATUS {
		RUN, REPRODUCE, INTERRUPT
	}
	
	@Override
	public void propertyViolated(Search search) {
//		if (this.status == STATUS.RUN) {
			this.result.pathRecorder.success = false;
			this.result.success = false;
//		}
//		else {
//			this.result.success = false;
//		}
	}
	
	@Override
	public void searchFinished(Search search) {
		if (this.result.pathRecorder.success == null) {
			this.result.pathRecorder.success = true;
		}
		
		//XmlParser.toXML(result.pairs, false, "F:\\before.xml");
		this.result.deleteRepeatePair();
		//XmlParser.toXML(result.pairs, false, "F:\\after.xml");
		//this.result.addId();
	}
	
	@Override
	public void choiceGeneratorSet (VM vm, ChoiceGenerator<?> newCG) {
		//System.out.println("cg set.");
//		if (newCG.isSchedulingPoint()) {
		if (newCG instanceof ThreadChoiceFromSet) {
			if (this.status == STATUS.RUN) {
				newCG.randomize();
//				ThreadInfo[] threads = ((ThreadChoiceFromSet) newCG).getAllThreadChoices();
//				if (threads.length == 1) {
//					return;
//				}
//				ThreadInfo t = threads[0];
//				Instruction insn = t.getPC();
//				String type = insn.getClass().getName();
				/*
				if (!(insn instanceof FieldInstruction)) {
					type = insn.getClass().toString();
				}
				else {
					FieldInstruction fins = (FieldInstruction)insn;
					type = fins.isRead() ? "READ" : "WRITE";
				}
				*/
				
				//result.pathRecorder.addSchedulingPoint(t.getName(), insn.getFileLocation(), type, this.result.mixed);
//				result.addSchedulingPoint(t.getName(), insn.getFileLocation(), type);
				
				//System.out.println("thread: " + t.getName() + " instruction: " + insn.getFileLocation() + " type: " + type);
			}
			else if (this.status == STATUS.REPRODUCE) {
				if (this.result.branchPoint == -1 || pathReplay.pointer < this.result.branchPoint) {
					ThreadInfo[] threads = ((ThreadChoiceFromSet)newCG).getAllThreadChoices();
					if (threads.length == 1) {
						return;
					}
					
					SchedulingPoint sp = pathReplay.pop();
					
					
					for (int i = 0; i < threads.length; i++) {
						ThreadInfo t = threads[i];
						Instruction insn = t.getPC();
						String type = insn.getClass().getName();
						if (sp == null) {
							System.out.println("pathRecorder empty.");
							newCG.randomize();
							break;
						}
						else if (sp.equals(t.getName(), insn.getFileLocation(), type)) {
							break;
						}
						else {
							newCG.advance();
							if (i == threads.length - 1) {
								if (result.problemId == null) {
									result.problemId = sp.id;
									result.sp = sp;
									result.choices = new ArrayList<Choice>();
									for (ThreadInfo ti : threads) {
										result.choices.add(new Choice(ti.getName(), t.getPC().getFileLocation(), t.getPC().getClass().getName()));
									}
								}
								
								newCG.reset();
								newCG.randomize();
								System.out.println("no next choice.");
								result.finished = false;
							}
						}
					}
				}
				else if (pathReplay.pointer == this.result.branchPoint) {
					
					inBranch = true;
					
					
					newCG.randomize();
					
					ThreadInfo[] threads = ((ThreadChoiceFromSet)newCG).getAllThreadChoices();
					
					//System.out.println("last branchpoint " + threads.length);
					
					
					SchedulingPoint sp = pathReplay.pop();
					
					for (int i = 0; i < threads.length; i++) {
						ThreadInfo t = threads[i];
						Instruction insn = t.getPC();
						String type = insn.getClass().getName();
						if (sp == null) {
							System.out.println("no next choice");
							newCG.randomize();
							break;
						}
						else if (!sp.equals(t.getName(), insn.getFileLocation(), type)) {
							break;
						}
						else {
							newCG.advance();
						}
					}
				}
				else {
					newCG.randomize();
				}
			}
			
			else if (this.status == STATUS.INTERRUPT) {
				System.out.println("----------------------in interrupt area-----------------------");
				ThreadInfo[] threads = ((ThreadChoiceFromSet)newCG).getAllThreadChoices();
				if (threads.length == 1) {
					if (pathReplay.randomMode == true && pathReplay.next_prefer_thread == null && pathReplay.next_avoid_thread != null) {
						pathReplay.next_avoid_thread = null;
					}
					return;
				}
				
				
				SchedulingPoint sp = pathReplay.pop();
				
				
				if (pathReplay.randomMode == false) {
					
					
					System.out.println("boundary:" + pathReplay.boundary + "\nsp:" + sp);
					if (pathReplay.boundary == null) {
						pathReplay.randomMode = true;
						pathReplay.next_avoid_thread = pathReplay.next_prefer_thread;
						pathReplay.next_prefer_thread = null;
					}
					else if (sp.identical(pathReplay.boundary)) {
						System.out.println("*************find boundary*******************");
						pathReplay.randomMode = true;
						
						boolean contains_prefer = false;
						for (ThreadInfo t : threads) {
							if (t.getName().equals(pathReplay.next_prefer_thread)) {
								contains_prefer = true;
								System.out.println("*********contains_prefer = true ************");
								break;
							}
						}
						
						for (int i = 0; i < threads.length; i++) {
							ThreadInfo t = threads[i];
							Instruction insn = t.getPC();
							String type = insn.getClass().getName();
							if (sp == null) {
								System.out.println("Interrupt mode: pathRecorder empty.");
								newCG.randomize();
								break;
							}
							
							if (contains_prefer == true) {
								if (!sp.nextThread.equals(pathReplay.next_prefer_thread)) {
									newCG.advance();
									continue;
								}
								else {
									pathReplay.next_avoid_thread = null;
									pathReplay.next_prefer_thread = null;
									break;
								}
							}
							
							
							else if (sp.equals(t.getName(), insn.getFileLocation(), type)) {
								newCG.advance();
								continue;
							}
							else {
								break;
							}
						}
						return;
					}
					
					
					
					
					for (int i = 0; i < threads.length; i++) {
						ThreadInfo t = threads[i];
						Instruction insn = t.getPC();
						String type = insn.getClass().getName();
						if (sp == null) {
							System.out.println("pathRecorder empty.");
							newCG.randomize();
							break;
						}
						else if (sp.equals(t.getName(), insn.getFileLocation(), type)) {
							break;
						}
						else {
							newCG.advance();
							if (i == threads.length - 1) {
								if (result.problemId == null) {
									result.problemId = sp.id;
									result.sp = sp;
									result.choices = new ArrayList<Choice>();
									for (ThreadInfo ti : threads) {
										result.choices.add(new Choice(ti.getName(), t.getPC().getFileLocation(), t.getPC().getClass().getName()));
									}
								}
								
								newCG.reset();
								newCG.randomize();
								System.out.println("no next choice.");
								result.finished = false;
							}
						}
					}
				}
				
				else {
					if (pathReplay.next_avoid_thread != null && pathReplay.next_prefer_thread != null) {
						boolean contains_prefer = false;
						for (ThreadInfo t : threads) {
							if (t.getName().equals(pathReplay.next_prefer_thread)) {
								contains_prefer = true;
								break;
							}
						}
						
						for (int i = 0; i < threads.length; i++) {
							ThreadInfo t = threads[i];
							Instruction insn = t.getPC();
							String type = insn.getClass().getName();
							if (sp == null) {
								System.out.println("Interrupt mode: pathRecorder empty.");
								newCG.randomize();
								break;
							}
							
							if (contains_prefer == true) {
								if (!sp.nextThread.equals(pathReplay.next_prefer_thread)) {
									newCG.advance();
									continue;
								}
								else {
									pathReplay.next_avoid_thread = null;
									pathReplay.next_prefer_thread = null;
									break;
								}
							}
							
							
							else if (sp.nextThread.equals(pathReplay.next_avoid_thread)) {
								newCG.advance();
								continue;
							}
							else {
								break;
							}
						}
					}
					else if (pathReplay.next_avoid_thread != null && pathReplay.next_prefer_thread == null) {
						for (int i = 0; i < threads.length; i++) {
							if (sp == null) {
								System.out.println("Interrupt mode: pathRecorder empty.");
								newCG.randomize();
								break;
							}
							
							if (sp.nextThread.equals(pathReplay.next_avoid_thread)) {
								newCG.advance();
							}
							else {
								break;
							}
						}
					}
					else {
						newCG.randomize();
					}
				}
			}
		}
		else {
			//System.out.println(newCG.getClass().getName());
			newCG.randomize();
		}
	}
	
	@Override
	public void choiceGeneratorAdvanced (VM vm, ChoiceGenerator<?> currentCG) {
		//System.out.println("cg advanced.");
//		if (currentCG.isSchedulingPoint()) {
		if (currentCG instanceof ThreadChoiceFromSet) {
			ThreadInfo[] threads = ((ThreadChoiceFromSet) currentCG).getAllThreadChoices();
			if (threads.length == 1) {
				return;
			}
			ThreadInfo t = (ThreadInfo)currentCG.getNextChoice();
			Instruction insn = t.getPC();
			String type = insn.getClass().getName();
			result.addSchedulingPoint(t.getName(), insn.getFileLocation(), type);
		}
	}
	
	@Override
	public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction) {
		/*
		if (this.status == STATUS.RUN) {
			
			return;
		}
		*/
		
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
			String eiString = ei == null ? "null" : ei.toString();
			String fiName = fi.getName();
			ReadWritePair pair = new ReadWritePair(eiString, fi.getName(), type, currentThread.getName(), fins.getFileLocation(), inBranch);
			//result.pairs.add(pair);
			result.addPair(pair);
		}
	}
	
	
	public static class PathRecorder {
		
		public List<Object> mixed;
		public List<SchedulingPoint> sps;
		public Boolean success;
		public int pointer;
		public boolean randomMode;
		public String next_prefer_thread;
		public String next_avoid_thread;
		public SchedulingPoint boundary;
		
		public PathRecorder() {
			mixed = null;
			sps = new ArrayList<SchedulingPoint>();
			success = null;
			pointer = 0;
			randomMode = false;
			next_prefer_thread = null;
			next_avoid_thread = null;
			boundary = null;
		}
		
		public void addSchedulingPoint(int id, String thread, String insn, String type) {
			SchedulingPoint sp = new SchedulingPoint(thread, insn, type);
			sp.id = id;
			sps.add(sp);
		}
		
//		public void addSchedulingPoint(String thread, String insn, String type, List<Object> mixed) {
//			SchedulingPoint sp = new SchedulingPoint(thread, insn, type);
//			sps.add(sp);
//			mixed.add(sp);
//		}
		
		public void addResult(boolean success) {
			this.success = success;
		}
		
		public SchedulingPoint pop() {
			if (pointer == sps.size()) {
				return null;
			}
			
			return sps.get(pointer++);
		}
		
		public void reset() {
			pointer = 0;
			randomMode = false;
		}
		
		public void swap(int index1, int index2) throws Exception {
			int length = sps.size();
			if (Math.abs(index1) >= length || Math.abs(index2) >= length) {
				throw new Exception("Out of bound.");
			}
			else {
				index1 = index1 < 0 ? length + index1 : index1;
				index2 = index2 < 0 ? length + index2 : index2;
				
				if (index1 == index2) {
					return;
				}
				
				SchedulingPoint temp1 = sps.get(index1).clone();
				SchedulingPoint temp2 = sps.get(index2).clone();
				sps.remove(index1);
				sps.add(index1, temp2);
				sps.remove(index2);
				sps.add(index2, temp1);
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
	}
	
	public static class Result {
		public Boolean success;
		public Boolean finished;
		
		public Integer problemId;
		public List<Choice> choices;
		public SchedulingPoint sp;
		
		public List<ReadWritePair> pairs;
		public Integer branchPoint;
		public List<Pattern> matchedPatterns;
		
		
		
		public PathRecorder pathRecorder;
		
		public List<Object> mixed;
		
		private int nextRWId;
		private int nextSPId;
		
		public Result(boolean success) {
			this.success = success;
			this.finished = true;
			problemId = null;
			choices = null;
			pairs = new ArrayList<ReadWritePair>();
			branchPoint = -1;
			this.matchedPatterns = new ArrayList<Pattern>();
			this.mixed = new ArrayList<Object>();
			this.nextRWId = 0;
			this.nextSPId = 0;
		}
		
		public void addPair(ReadWritePair pair) {
			pair.id = nextRWId++;
			this.pairs.add(pair);
			mixed.add(pair);
		}
		
		public void addSchedulingPoint(String thread, String insn, String type) {
			SchedulingPoint sp = new SchedulingPoint(thread, insn, type);
			sp.id = this.nextSPId++;
			this.pathRecorder.sps.add(sp);
			mixed.add(sp);
		}
		
		public List<Object> interruptPatterns(List<Pattern> patterns) throws Exception {
			return interruptPatterns(patterns, this.mixed);
		}
		
		public static List<Object> interruptPatterns(List<Pattern> patterns, List<Object> origin) throws Exception {
			
			List<Pattern> toMatch = PatternInit.initPatterns();
			List<Object> result = origin;
			
			for (Pattern pattern : patterns) {
				List<ReadWritePair> rwPairs = Replay.toReadWritePairs(result);
				List<Pattern> matched = Result.matchPatterns(rwPairs, toMatch);
				
				if (matched.contains(pattern)) {
					result = interruptPattern1(pattern, result);
				}
				
				
				if(result == null){
					result = origin;
					result = interruptPattern2(pattern, result);
				}
			}
			
			return result;
		}
		
		
		private static List<Object> interruptPattern1(Pattern pattern, List<Object> origin) throws Exception {
			if (!pattern.match) {
				throw new Exception("pattern is not matched");
			}
			
			
			
			
			
			List<Object> result = null;
			
			List<SchedulingPoint> patternsSP = new ArrayList<SchedulingPoint>();
			
			SchedulingPoint currentSP = null;
			
			int indexOfPattern = 0;
			
			
			outer:
			for (int i = 0; i < origin.size(); i++) {
				if (origin.get(i) instanceof SchedulingPoint) {
					currentSP = (SchedulingPoint)origin.get(i);
				}
				else {
					ReadWritePair pair = (ReadWritePair)origin.get(i);
					for (int j = 0; j < pattern.nodes.size(); j++) {
						if (pattern.nodes.get(indexOfPattern).pair.id == pair.id){
							patternsSP.add(currentSP);
							indexOfPattern++;
							if (indexOfPattern == pattern.nodes.size()) {
								
								break outer;
							}
						}
					}
				}
			}
			
			if (patternsSP.size() != pattern.nodes.size()) {
				throw new Exception("There is an element in the pattern that does not find the corresponding schedulingPoint.");
			}
			
			for (int i = 0; i < patternsSP.size() - 1; i++) {
				if (patternsSP.get(i) != null && patternsSP.get(i) != patternsSP.get(i + 1)) {
					result = swapSchedulingPoint(origin, patternsSP.get(i), patternsSP.get(i + 1));
					break;
				}
			}
			
			return result;
		}
		
		
		private static List<Object> swapSchedulingPoint(List<Object> origin, SchedulingPoint sp1, SchedulingPoint sp2) {
			System.out.println("to swap:\nsp1:\n" + sp1 + "\nsp2:\n" + sp2);
			
			List<Object> result = new ArrayList<Object>();
			Queue<Object> buffer = new LinkedList<Object>();
			
			int i;
			
			for (i = 0; i < origin.size(); i++) {
				if ((origin.get(i) instanceof SchedulingPoint) && sp1.id == ((SchedulingPoint)origin.get(i)).id) {
					break;
				}

				if (origin.get(i) instanceof SchedulingPoint) {
					result.add(((SchedulingPoint)origin.get(i)).clone());
				}
				else {
					result.add(((ReadWritePair)origin.get(i)).clone());
				}
			}
			
		
			buffer.offer(((SchedulingPoint)origin.get(i)).clone());
			boolean sameThread = false;
			for (i = i + 1; i < origin.size(); i++) {
				if ((origin.get(i) instanceof SchedulingPoint) && sp2.id == ((SchedulingPoint)origin.get(i)).id) {
					break;
				}
				if (sameThread) {
					if (origin.get(i) instanceof SchedulingPoint) {
						SchedulingPoint sp = (SchedulingPoint)origin.get(i);
						if (!sp.nextThread.equals(sp1.nextThread) && sp.nextThread.equals(sp2.nextThread)) {
							
							result.add(sp);
						}
						else {
							
							sameThread = false;
							buffer.offer(sp);
							continue;
						}
					}
					else {
						result.add(((ReadWritePair)origin.get(i)).clone());
					}
				}
				else {
					if (origin.get(i) instanceof SchedulingPoint) {
						SchedulingPoint sp = (SchedulingPoint)origin.get(i);
						if (!sp.nextThread.equals(sp1.nextThread) && sp.nextThread.equals(sp2.nextThread)) {
							
							sameThread = true;
							result.add(sp.clone());
							continue;
						}
						else {
							
							buffer.offer(sp.clone());
						}
					}
					else {
						buffer.offer(((ReadWritePair)origin.get(i)).clone());
					}
				}
				
//				if (origin.get(i) instanceof SchedulingPoint) {
//					buffer.offer(((SchedulingPoint)origin.get(i)).clone());
//				}
//				else {
//					buffer.offer(((ReadWritePair)origin.get(i)).clone());
//				}
			}
			
			
			result.add(((SchedulingPoint)origin.get(i)).clone());
			for (i = i + 1; i < origin.size(); i++) {
				if (origin.get(i) instanceof SchedulingPoint) {
					break;
				}
				if (origin.get(i) instanceof SchedulingPoint) {
					result.add(((SchedulingPoint)origin.get(i)).clone());
				}
				else {
					result.add(((ReadWritePair)origin.get(i)).clone());
				}
			}
			
			for (Object obj = buffer.poll(); obj != null; obj = buffer.poll()) {
				result.add(obj);
			}
			
			if (i == origin.size()) {
				return result;
			}
			
			result.add(((SchedulingPoint)origin.get(i)).clone());
			for (i = i + 1; i < origin.size(); i++) {
				if (origin.get(i) instanceof SchedulingPoint) {
					result.add(((SchedulingPoint)origin.get(i)).clone());
				}
				else {
					result.add(((ReadWritePair)origin.get(i)).clone());
				}
			}
			
			return result;
		}
		
		
		private static List<Object> interruptPattern2(Pattern pattern, List<Object> origin) throws Exception {
			if (!pattern.match) {
				throw new Exception("pattern is not matched");
			}
			
			List<Object> result = new ArrayList<Object>();
			
			Queue<Object> buffer = new LinkedList<Object>();
			
			String var = pattern.nodes.get(0).pair.instance + "," + pattern.nodes.get(0).pair.field;
			
			String thr = pattern.nodes.get(0).pair.thread;
			
			
			for (int i = 0; i < origin.size() && origin.get(i) instanceof ReadWritePair; i++) {
				Object obj = origin.get(i);
				result.add(((ReadWritePair)obj).clone());
				
			}
			
			for (int i = 0; i < origin.size(); i++) {
				if (!(origin.get(i) instanceof SchedulingPoint)) {
					continue;
				}
				
				
				List<ReadWritePair> pairs = new ArrayList<ReadWritePair>();
				for (int j = i + 1; j < origin.size() && origin.get(j) instanceof ReadWritePair; j++) {
					pairs.add((ReadWritePair)origin.get(j));
				}
				
				if (buffer.isEmpty()) {
					boolean needToInterrupt = false;
					for (ReadWritePair pair : pairs) {
						ReadWritePair lastPairOfPattern = pattern.nodes.get(pattern.nodes.size() - 1).pair;
						if (pair.id == lastPairOfPattern.id) {
							
							needToInterrupt = true;
							break;
						}
					}
					
					if (needToInterrupt) {
						buffer.offer(((SchedulingPoint)origin.get(i)).clone());
						for (ReadWritePair pair : pairs) {
							buffer.offer(pair.clone());
						}
					}
					else {
						result.add(((SchedulingPoint)origin.get(i)).clone());
						for (ReadWritePair pair : pairs) {
							result.add(pair.clone());
						}
					}
				}
				else {
					
					boolean canInterrupt = false;
					result.add(((SchedulingPoint)origin.get(i)).clone());
					for (ReadWritePair pair : pairs) {
						result.add(pair.clone());
						String pairVar = pair.instance + "," + pair.field;

						String pairThr = pair.thread;
						if (var.equals(pairVar) && !thr.equals(pairThr)) {
							
							canInterrupt = true;
						}
					}
					
					if (canInterrupt) {
						
						for (Object obj = buffer.poll(); obj != null; obj = buffer.poll()) {
							result.add(obj);
						}
					}
					else {
						
						continue;
					}
				}
			}
			
			if (!buffer.isEmpty()) {
				
				return null;
			}
			
			return result;
		}
		
		
		public boolean isOnSchedulingPoint(ReadWritePair pair) {
			for (int i = 0; i < this.mixed.size(); i++) {
				if (pair.equals(this.mixed.get(i)) && pair.id == ((ReadWritePair)this.mixed.get(i)).id) {
					if (i == 0) {
						return false;
					}
					Object pri = this.mixed.get(i - 1);
					
					if (!(pri instanceof SchedulingPoint)) {
						return false;
					}
					
					if (((SchedulingPoint)pri).nextThread.equals(pair.thread)
							&& ((SchedulingPoint)pri).nextInstruction.equals(pair.location)
							&& pair.type.equals("READ") ? 
									((SchedulingPoint)pri).nextInstructionType.contains("GET") 
									: ((SchedulingPoint)pri).nextInstructionType.contains("PUT")) {
						return true;
					}
					else {
						return false;
					}
				}
			}
			return false;
		}
		
		
		public List<Pattern> getMatchedPatternsCopy() {
			List<Pattern> copy = new ArrayList<Pattern>();
			for (Pattern pattern : this.matchedPatterns) {
				copy.add(pattern.clone());
			}
			
			return copy;
		}
		
		public boolean equals(Object objResult) {
			if (!(objResult instanceof Result)) {
				return false;
			}
			Result result = (Result)objResult;
			if (this.pairs.size() != result.pairs.size() || this.success != result.success) {
				return false;
			}
			else {
				for (int i = 0; i < this.pairs.size(); i++) {
					if (!this.pairs.get(i).equals(result.pairs.get(i))) {
						return false;
					}
				}
				return true;
			}
		}
		

		public void deleteRepeatePair() {
			List<ReadWritePair> afterDelete = new ArrayList<ReadWritePair>();
			
			for (int i = 0; i < pairs.size(); i++) {
				ReadWritePair current = pairs.get(i);
				
				boolean needDelete = false;
				for (int j = i + 1; j < pairs.size(); j++) {

					if (!pairs.get(j).thread.equals(current.thread)) {
						continue;
					}
					
					if (pairs.get(j).equals(current)) {
						needDelete = true;
					}
					
					break;
				}
				
				if (!needDelete) {
					afterDelete.add(current);
				}
			}
			
			pairs = afterDelete;
			
		
			List<Object> mixedAfterDelete = new ArrayList<Object>();
			for (int i = 0; i < mixed.size(); i++) {
				if (mixed.get(i) instanceof SchedulingPoint) {
					mixedAfterDelete.add(mixed.get(i));
					continue;
				}
				
				ReadWritePair current = (ReadWritePair)mixed.get(i);
				
				boolean needDelete = false;
				for (int j = i + 1; j < mixed.size(); j++) {
					if (mixed.get(j) instanceof SchedulingPoint) {
						continue;
					}
					ReadWritePair rwp = (ReadWritePair)mixed.get(j);
					
					
					if (!rwp.thread.equals(current.thread)) {
						continue;
					}
					
					if (rwp.equals(current)) {
						needDelete = true;
					}
					
					break;
				}
				
				if (!needDelete) {
					mixedAfterDelete.add(current);
				}
			}
			
			mixed = mixedAfterDelete;
			
			
			
		public static List<Pattern> matchPatterns(List<ReadWritePair> pairs, List<Pattern> patterns) {
			List<Pattern> result = new ArrayList<Pattern>();
			for (Pattern pattern : patterns) {
				
				for (ReadWritePair first : Result.matchFirst(pairs, pattern)) {
					Pattern matchedPattern = pattern.clone();
					matchedPattern.nodes.get(0).pair = first;
					int pointer = 1;
					Map<String, String> existVars = new HashMap<String, String>();
					Map<String, String> existThreads = new HashMap<String, String>();
					
				
					existVars.put(matchedPattern.nodes.get(0).var, first.instance + "," + first.field);
					existThreads.put(matchedPattern.nodes.get(0).thread, first.thread);
					
					while (pointer < matchedPattern.nodes.size()) {
						if (pointer == 2) {
							//System.out.println("to match third.");
						}
						
						ReadWritePair nextPair = null;
						PatternNode nextPatternNode = matchedPattern.nodes.get(pointer);
						
						if (existVars.get(nextPatternNode.var) != null) {
							if (existThreads.get(nextPatternNode.thread) != null) {
								
								nextPair = Result.nextPair(pairs, matchedPattern.nodes.get(pointer - 1).pair, 
										existVars.get(nextPatternNode.var), existThreads.get(nextPatternNode.thread));
							}
							else {
								List<String> notInThreads = new ArrayList<String>();
								for (String key : existThreads.keySet()) {
									notInThreads.add(existThreads.get(key));
								}
								
//								if (matchedPattern.nodes.get(pointer - 1).pair.id == 4) {
//									System.out.println("find it.");
//								}
								
								nextPair = Result.nextPair(pairs, matchedPattern.nodes.get(pointer - 1).pair, 
										existVars.get(nextPatternNode.var), notInThreads);
							}
						}
						else {
							if (existThreads.get(nextPatternNode.thread) != null) {
								List<String> notInVars = new ArrayList<String>();
								for (String key : existVars.keySet()) {
									notInVars.add(existVars.get(key));
								}
								
								nextPair = Result.nextPair(pairs, matchedPattern.nodes.get(pointer - 1).pair, 
										notInVars, existThreads.get(nextPatternNode.thread));
							}
							else {
								List<String> notInVars = new ArrayList<String>();
								for (String key : existVars.keySet()) {
									notInVars.add(existVars.get(key));
								}
								List<String> notInThreads = new ArrayList<String>();
								for (String key : existThreads.keySet()) {
									notInThreads.add(existThreads.get(key));
								}
								
								nextPair = Result.nextPair(pairs, matchedPattern.nodes.get(pointer - 1).pair, 
										notInVars, notInThreads);
							}
						}
						
						if (nextPair == null) {
							break;
						}
						
						if (!nextPair.type.equals(matchedPattern.nodes.get(pointer).type)) {
							break;
						}
						
						existVars.put(matchedPattern.nodes.get(pointer).var, nextPair.instance + "," + nextPair.field);
						existThreads.put(matchedPattern.nodes.get(pointer).thread, nextPair.thread);
						
						matchedPattern.nodes.get(pointer).pair = nextPair;
						pointer++;
					}
					
					if (pointer == matchedPattern.nodes.size()) {
						matchedPattern.match = true;
						result.add(matchedPattern);
					}
				}
			}
			return result;
		}
		
		public static List<Pattern> matchPatterns2(List<ReadWritePair> pairs, List<Pattern> patterns) {
			List<Pattern> result = new ArrayList<Pattern>();
			for (Pattern pattern : patterns) {
				
				result.addAll(matchNext(pairs, 0, pattern, null, null));
			}
			return result;
		}
		
		private static List<Pattern> matchNext(List<ReadWritePair> pairs, int pointer, Pattern pattern, 
				Map<String, String> existVars, Map<String, String> existThreads) {
			if (pointer == pattern.nodes.size()) {
				pattern.match = true;
				List<Pattern> patterns = new ArrayList<Pattern>();
				patterns.add(pattern);
				return patterns;
			}
			if (pointer == 0) {
				List<Pattern> result = new ArrayList<Pattern>();
				for (ReadWritePair first : Result.matchFirst(pairs, pattern)) {
					Pattern newPattern = pattern.clone();
					existVars = new HashMap<String, String>();
					existThreads = new HashMap<String, String>();
					newPattern.nodes.get(0).pair = first;
					existVars.put(pattern.nodes.get(0).var, first.instance + "," + first.field);
					existThreads.put(pattern.nodes.get(0).thread, first.thread);
					List<Pattern> sub = matchNext(pairs, 1, newPattern, existVars, existThreads);
					if (sub != null) {
						result.addAll(sub);
					}
				}
				return result;
			}
			
			String nextVar = existVars.get(pattern.nodes.get(pointer).var);
			
			
			String nextThread = existThreads.get(pattern.nodes.get(pointer).thread);
			
			if (nextVar == null) {
				if (nextThread == null) {
					List<Pattern> result = new ArrayList<Pattern>();
					Set<String> VisitedVars = new HashSet<String>();
					for (int i = Result.indexOf(pairs, pattern.nodes.get(pointer - 1).pair) + 1; i < pairs.size(); i++) {
						ReadWritePair pair = pairs.get(i);
						String pairVar = pair.instance + "," + pair.field;
						if (!existVars.containsValue(pairVar) && !existThreads.containsValue(pair.thread) && !VisitedVars.contains(pairVar)) {
							if (!pair.type.equals(pattern.nodes.get(pointer).type)) {
								continue;
							}
							
							Pattern newPattern = pattern.clone();
							
							newPattern.nodes.get(pointer).pair = pair;
							VisitedVars.add(pairVar);
							Map<String, String> newExistVars = new HashMap<String, String>();
							Map<String, String> newExistThreads = new HashMap<String, String>();
							for (String key : existVars.keySet()) {
								newExistVars.put(key, existVars.get(key));
							}
							for (String key : existThreads.keySet()) {
								newExistThreads.put(key, existThreads.get(key));
							}
							
							newExistVars.put(pattern.nodes.get(pointer).var, pairVar);
							newExistThreads.put(pattern.nodes.get(pointer).thread, pair.thread);
							
							List<Pattern> sub = matchNext(pairs, pointer + 1, newPattern, newExistVars, newExistThreads);
							if (sub != null) {
								result.addAll(sub);
							}
						}
					}
					return result;
				}
				else {
					List<Pattern> result = new ArrayList<Pattern>();
					Set<String> VisitedVars = new HashSet<String>();
					for (int i = Result.indexOf(pairs, pattern.nodes.get(pointer - 1).pair) + 1; i < pairs.size(); i++) {
						ReadWritePair pair = pairs.get(i);
						String pairVar = pair.instance + "," + pair.field;
						if (!existVars.containsValue(pairVar) && nextThread.equals(pair.thread) && !VisitedVars.contains(pairVar) && pair.type.equals(pattern.nodes.get(pointer).type)) {
							if (!pair.type.equals(pattern.nodes.get(pointer).type)) {
								continue;
							}
							
							Pattern newPattern = pattern.clone();
							
							newPattern.nodes.get(pointer).pair = pair;
							VisitedVars.add(pairVar);
							Map<String, String> newExistVars = new HashMap<String, String>();
							for (String key : existVars.keySet()) {
								newExistVars.put(key, existVars.get(key));
							}
							
							newExistVars.put(pattern.nodes.get(pointer).var, pairVar);
							
							List<Pattern> sub = matchNext(pairs, pointer + 1, newPattern, newExistVars, existThreads);
							if (sub != null) {
								result.addAll(sub);
							}
						}
					}
					return result;
				}
			}
			else {
				if (nextThread == null) {
					List<String> notInThreads = new ArrayList<String>();
					for (String key : existThreads.keySet()) {
						notInThreads.add(existThreads.get(key));
					}
					ReadWritePair nextPair = Result.nextPair(pairs, pattern.nodes.get(pointer - 1).pair, nextVar, notInThreads);
					if (nextPair == null) {
						return null;
					}
					if (!nextPair.type.equals(pattern.nodes.get(pointer).type)) {
						return null;
					}
					pattern.nodes.get(pointer).pair = nextPair;
					Map<String, String> newExistThreads = new HashMap<String, String>();
					for (String key : existThreads.keySet()) {
						newExistThreads.put(key, existThreads.get(key));
					}
					newExistThreads.put(pattern.nodes.get(pointer).thread, nextPair.thread);
					return matchNext(pairs, pointer + 1, pattern, existVars, newExistThreads);
				}
				else {
					ReadWritePair nextPair = Result.nextPair(pairs, pattern.nodes.get(pointer - 1).pair, nextVar, nextThread);
					if (nextPair == null) {
						return null;
					}
					if (!nextPair.type.equals(pattern.nodes.get(pointer).type)) {
						return null;
					}
					pattern.nodes.get(pointer).pair = nextPair;
					return matchNext(pairs, pointer + 1, pattern, existVars, existThreads);
				}
			}
		}
		
		
		public void matchPatterns(List<Pattern> patterns) {
			this.matchedPatterns = Result.matchPatterns2(this.pairs, patterns);
		}
		
		public void matchPatterns2(List<Pattern> patterns) {
			this.matchPatterns(patterns);
			
			
			List<Pattern> resultDelete = new ArrayList<Pattern>();
			
			for (int i = 0;i < matchedPatterns.size(); i++) {
				if(matchedPatterns.get(i).nodes.size() == 2){
					List<ReadWritePair> li = new ArrayList<>();
					li.add(matchedPatterns.get(i).nodes.get(0).pair.clone());
					li.add(matchedPatterns.get(i).nodes.get(1).pair.clone());
					for (ReadWritePair readWritePair : li) {
						readWritePair.id = 0;
					}
					for(int j = i + 1;j < matchedPatterns.size();j++){
						if(matchedPatterns.get(j).nodes.size() >= 3){
							List<ReadWritePair> lj = new ArrayList<>();
							for (PatternNode patternNode : matchedPatterns.get(j).nodes) {
								lj.add(patternNode.pair.clone());
							}
							for (ReadWritePair readWritePair : lj) {
								readWritePair.id = 0;
							}
							if(lj.containsAll(li))
								resultDelete.add(matchedPatterns.get(i));
						}
					}
				}
			}

			for (Pattern pattern : resultDelete) {
				matchedPatterns.remove(pattern);
			}
		}
		
//		private List<ReadWritePair> matchFirst(Pattern pattern) {
//			return Result.matchFirst(this.pairs, pattern);
//		}
		
		private static List<ReadWritePair> matchFirst(List<ReadWritePair> pairs, Pattern pattern) {
			List<ReadWritePair> matchedFirst = new ArrayList<ReadWritePair>();
			for (ReadWritePair pair : pairs) {
				if (pair.type.equals(pattern.nodes.get(0).type)) {
					matchedFirst.add(pair);
				}
			}
			return matchedFirst;
		}
		
		/*
		
		public ReadWritePair nextPair(ReadWritePair start, boolean sameVar, boolean sameThread) {
			for (int i = start.id + 1; i < this.pairs.size(); i++) {
				ReadWritePair pair = this.pairs.get(i);
				if (sameVar) {
					if (!(pair.instance.equals(start.instance) && pair.field.equals(start.field))) {
						continue;
					}
				}
				else {
					if (pair.instance.equals(start.instance) && pair.field.equals(start.field)) {
						continue;
					}
				}
				
				if (sameThread) {
					if (!pair.thread.equals(start.thread)) {
						continue;
					}
				}
				else {
					if (pair.thread.equals(start.thread)) {
						continue;
					}
				}
				return pair;
			}
			return null;
		}
		*/
		
		private static int indexOf(List<ReadWritePair> pairs, ReadWritePair pair) {
			for (int i = 0; i < pairs.size(); i++) {
				if (pairs.get(i).id == pair.id) {
					return i;
				}
			}
			return -1;
		}
		
		private int indexOf(ReadWritePair pair) {
			return Result.indexOf(this.pairs, pair);
		}
		
		public static ReadWritePair nextPair(List<ReadWritePair> pairs, ReadWritePair start, String var, String thread) {
			for (int i = Result.indexOf(pairs, start) + 1; i < pairs.size(); i++) {
				ReadWritePair pair = pairs.get(i);
				String pairVar = pair.instance + "," + pair.field;
				if (pairVar.equals(var) && pair.thread.equals(thread)) {
					return pair;
				}
				
				if (pairVar.equals(var) && pair.type.equals("WRITE")) {
					return null;
				}
			}
			return null;
		}
		
		
		public ReadWritePair nextPair(ReadWritePair start, String var, String thread) {
			return Result.nextPair(this.pairs, start, var, thread);
		}
		
		public static ReadWritePair nextPair(List<ReadWritePair> pairs, ReadWritePair start, String var, List<String> threads) {
			for (int i = Result.indexOf(pairs, start) + 1; i < pairs.size(); i++) {
				ReadWritePair pair = pairs.get(i);
				String pairVar = pair.instance + "," + pair.field;
				if (pairVar.equals(var) && !threads.contains(pair.thread)) {
					return pair;
				}
				
				if (pairVar.equals(var) && pair.type.equals("WRITE")) { 
					return null;
				}
			}
			return null;
		}
		
		
		public ReadWritePair nextPair(ReadWritePair start, String var, List<String> threads) {
			return Result.nextPair(this.pairs, start, var, threads);
		}
		
		public static ReadWritePair nextPair(List<ReadWritePair> pairs, ReadWritePair start, List<String> vars, String thread) {
			for (int i = Result.indexOf(pairs, start) + 1; i < pairs.size(); i++) {
				ReadWritePair pair = pairs.get(i);
				String pairVar = pair.instance + "," + pair.field;
				if (!vars.contains(pairVar) && pair.thread.equals(thread)) {
					return pair;
				}
			}
			return null;
		}
		
		
		public ReadWritePair nextPair(ReadWritePair start, List<String> vars, String thread) {
			return Result.nextPair(this.pairs, start, vars, thread);
		}
		
		public static ReadWritePair nextPair(List<ReadWritePair> pairs, ReadWritePair start, List<String> vars, List<String> threads) {
			for (int i = Result.indexOf(pairs, start) + 1; i < pairs.size(); i++) {
				ReadWritePair pair = pairs.get(i);
				String pairVar = pair.instance + "," + pair.field;
				if (!vars.contains(pairVar) && !threads.contains(pair.thread)) {
					return pair;
				}
			}
			return null;
		}
		
		
		public ReadWritePair nextPair(ReadWritePair start, List<String> vars, List<String> threads) {
			return Result.nextPair(this.pairs, start, vars, threads);
		}
		
		public String toString() {
			String result = "pairs\tresult=" + this.success.toString();
			result += "\n{\n";
			for (ReadWritePair pair : this.pairs) {
				result += "\t" + pair.toString() + "\n";
			}
			result += "}\n";
			
			return result;
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
	
	public class Filter {
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
	
	public class Choice {
		public String thread;
		public String instruction;
		public String type;
		
		public Choice(String thread, String instruction, String type) {
			this.thread = thread;
			this.instruction = instruction;
			this.type = type;
		}
	}
	
	public static class Pattern {
		
		public List<PatternNode> nodes;
		public boolean match;
		
		public Pattern() {
			this.nodes = new ArrayList<PatternNode>();
			this.match = false;
		}
		
		public Pattern clone() {
			Pattern pattern = new Pattern();
			for (PatternNode node : this.nodes) {
				pattern.nodes.add(node.clone());
			}
			pattern.match = this.match;
			
			return pattern;
		}
		
		public Pattern cloneNotMatch() {
			Pattern pattern = new Pattern();
			for (PatternNode node : this.nodes) {
				pattern.nodes.add(node.cloneWithoutPair());
			}
			pattern.match = false;
			
			return pattern;
		}
		
		public boolean isSamePattern(Pattern pattern) {
			boolean result = this.match == pattern.match && this.nodes.size() == pattern.nodes.size();
			
			for (int i = 0; i < this.nodes.size(); i++) {
				result = result && this.nodes.get(i).isSamePatternNode(pattern.nodes.get(i));
			}
			return result;
		}
		
		public static class PatternNode {
			public String var;
			public String thread;
			public String type;
			public ReadWritePair pair;
			
			public PatternNode(String var, String thread, String type) {
				this.var = var;
				this.thread = thread;
				this.type = type;
				this.pair = null;
			}
			
			public PatternNode clone() {
				PatternNode node = new PatternNode(this.var, this.thread, this.type);
				node.pair = this.pair == null ? null : this.pair.clone();
				return node;
			}
			
			public PatternNode cloneWithoutPair() {
				return new PatternNode(this.var, this.thread, this.type);
			}
			
			public boolean equals(Object objNode) {
				if (!(objNode instanceof PatternNode)) {
					return false;
				}
				PatternNode node = (PatternNode)objNode;
				
				boolean result = this.var.equals(node.var) && this.thread.equals(node.thread)
						&& this.type.equals(node.type);
				if (this.pair == null) {
					result = result && node.pair == null;
				}
				else {
					result = result && this.pair.equals(node.pair);
				}
				
				return result;
			}
			
			public boolean isSamePatternNode(PatternNode node) {
				boolean result = this.var.equals(node.var) && this.thread.equals(node.thread)
						&& this.type.equals(node.type);
				if (this.pair == null) {
					result = result && node.pair == null;
				}
				else {
					result = result && this.pair.instance.equals(node.pair.instance) && this.pair.field.equals(node.pair.field)
							&& this.pair.type.equals(node.pair.type) && this.pair.location.equals(node.pair.location);
				}
				
				return result;
			}
		}
		
		public String toString() {
			String result = "pattern {\n";
			for (PatternNode node : nodes) {
				result += "\tvar: " + node.var + "\tthread: " + node.thread + "\ttype: " + node.type + "\n";
			}
			result += "}\n";
			if (this.match) {
				result += "matched: {\n";
				for (PatternNode node : nodes) {
					result += "\t" + node.pair.toString() + "\n";
				}
				result += "}\n";
			}
			
			return result;
		}
		
		public boolean equals(Object objPattern) {
			if (!(objPattern instanceof Pattern)) {
				return false;
			}
			Pattern pattern = (Pattern)objPattern;
			
			boolean result = this.match == pattern.match && this.nodes.size() == pattern.nodes.size();
			
			for (int i = 0; i < this.nodes.size(); i++) {
				result = result && this.nodes.get(i).equals(pattern.nodes.get(i));
			}
			return result;
		}
	}
}
