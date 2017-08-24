package scheduler.listener;

import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;

public class ConcurrentTestListener extends PropertyListenerAdapter {
	public boolean isConcurrent;
	
	public ConcurrentTestListener() {
		isConcurrent = false;
	}
	
	@Override
	public void choiceGeneratorSet (VM vm, ChoiceGenerator<?> newCG) {
		if (newCG instanceof ThreadChoiceFromSet) {
			ThreadChoiceFromSet tcg = (ThreadChoiceFromSet)newCG;
			ThreadInfo[] ti = tcg.getAllThreadChoices();
			if (ti.length > 1) {
				isConcurrent = true;
			}
		}
	}
}
