package scheduler.listener;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.bytecode.FieldInstruction;
import scheduler.model.DataCollection;
import scheduler.model.ReadWriteNode;
import scheduler.model.SequenceMessage;

public class MonitorListenerAdapter extends PropertyListenerAdapter {
	
	public Result result;
	public Filter filter;
	
	public MonitorListenerAdapter() {
		result = new Result(true);
		filter = new Filter();
	}
	
	public MonitorListenerAdapter(String instanceFilter, String fieldFilter, List<String> files) {
		result = new Result(true);
		this.filter = new Filter(instanceFilter, fieldFilter, files);
	}
	
	@Override
	public void choiceGeneratorSet (VM vm, ChoiceGenerator<?> newCG) {
		if (newCG.isSchedulingPoint()) {
			newCG.randomize();
		}
	}
	
	@Override
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
			
			ReadWritePair pair = new ReadWritePair(ei.toString(), fi.getName(), type, currentThread.getName(), fins.getFileLocation());
			result.pairs.add(pair);
		}
	}
	
	@Override
	public void propertyViolated(Search search) {
		result.success = false;
	}
	
	@Override
	public void searchFinished(Search search) {
		int before = result.pairs.size();
		result.deleteRepeatePair();
		int after = result.pairs.size();
		//System.out.println("delete finished.\nbefore:" + before + " after:" + after);
	}
	
	public class Result {
		public boolean success;
		public List<ReadWritePair> pairs;
		
		public Result(boolean success) {
			this.success = success;
			pairs = new ArrayList<ReadWritePair>();
		}
		
		public boolean equals(Result result) {
			if (this.pairs.size() != result.pairs.size() || this.success != result.success) {
				return false;
			}
			else {
				for (ReadWritePair p : this.pairs) {
					if (!result.pairs.contains(p)) {
						return false;
					}
				}
				return true;
			}
		}
		
		//TODO Î´¾­¹ý²âÊÔ
		public void deleteRepeatePair() {
			int i = 0;
			
			outer:
			while (i < pairs.size()) {
				ReadWritePair base = pairs.get(i);
				for (int j = i + 1; j < pairs.size(); j++) {
					ReadWritePair pointer = pairs.get(j);
					
					
					if (!base.thread.equals(pointer.thread)) {
						continue;
					}
					
					if (!base.equals(pointer)) {
						break;
					}
					else {
						//System.out.println("remove.");
						pairs.remove(base);
						continue outer;
					}
				}
				i++;
			}
		}
	}
	
	public class ReadWritePair {
		public String instance;
		public String field;
		public String type;
		public String thread;
		public String location;
		
		public ReadWritePair(String instance, String field, String type, String thread, String location) {
			this.instance = instance;
			this.field = field;
			this.type = type;
			this.thread = thread;
			this.location = location;
		}
		
		public boolean equals(ReadWritePair pair) {
			return this.instance.equals(pair.instance) && this.field.equals(pair.field)
					&& this.type.equals(pair.type) && this.thread.equals(pair.thread)
					&& this.location.equals(pair.location);
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
}
