package scheduler.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.bytecode.FieldInstruction;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;
import scheduler.enumerate.ListenerState;

public class _UseForTest extends PropertyListenerAdapter{

	
	private int oneSlot;
	//private long twoSlot;
	
	@Override
	public void executeInstruction(VM vm, ThreadInfo currentThread, Instruction instructionToExecute) {
		if (instructionToExecute instanceof FieldInstruction && !((FieldInstruction) instructionToExecute).isRead()) {
			FieldInstruction fins = (FieldInstruction)instructionToExecute;
			FieldInfo fi = fins.getFieldInfo();
			ElementInfo ei = fins.getElementInfo(currentThread);
			if (fi.is1SlotField())
				oneSlot = ei.get1SlotField(fi);
			//twoSlot = ei.get2SlotField(fi);
		}
	}
	
	@Override
	public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction) {
		if (executedInstruction instanceof FieldInstruction && !((FieldInstruction) executedInstruction).isRead()) {
			FieldInstruction fins = (FieldInstruction)executedInstruction;
			FieldInfo fi = fins.getFieldInfo();
			ElementInfo ei = fins.getElementInfo(currentThread);
			if (fi.is1SlotField()) {
				System.out.print(fins.getFileLocation());
				if (ei.get1SlotField(fi) == oneSlot) {
					System.out.println("\tnot changed");
				}
				else {
					System.out.println("\tchanged");
				}
			}
		}
	}
}
