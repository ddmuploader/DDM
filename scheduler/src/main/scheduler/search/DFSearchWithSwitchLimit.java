package scheduler.search;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Transition;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;

/**
 * standard depth first model checking (but can be bounded by search depth
 * and/or explicit Verify.ignoreIf)
 */
public class DFSearchWithSwitchLimit extends Search {
	
	private int switchLimit;
	public int switchCount;
	
	private Stack<String> threadStack;
	

	private Map<Integer, Integer> stateSwitch;

  public DFSearchWithSwitchLimit (Config config, VM vm) {
  	super(config,vm);
  	switchCount = 0;
  	threadStack = new Stack<String>();
  	stateSwitch = new HashMap<Integer, Integer>();
  }
  
  protected void initialize( Config conf) {
	  super.initialize(conf);
	  switchLimit = conf.getInt("search.switch_limit", Integer.MAX_VALUE);
  }

  @Override
  public boolean requestBacktrack () {
    doBacktrack = true;

    return true;
  }

  /**
   * state model of the search
   *    next new  -> action
   *     T    T      forward
   *     T    F      backtrack, forward
   *     F    T      backtrack, forward
   *     F    F      backtrack, forward
   *
   * end condition
   *    backtrack failed (no saved states)
   *  | property violation (currently only checked in new states)
   *  | search constraint (depth or memory or time)
   *
   * <2do> we could split the properties into forward and backtrack properties,
   * the latter ones being usable for liveness properties that are basically
   * condition accumulators for sub-paths of the state space, to be checked when
   * we backtrack to the state where they were introduced.
   */
  @Override
  public void search () {
    boolean depthLimitReached = false;

    depth = 0;

    notifySearchStarted();

    while (!done) {
//    	Integer sc = stateSwitch.get(this.getStateId());
//    	if (!isNewState()) {
//    		if (sc == null) {
//    			requestBacktrack();
//    		}
//    		else if (sc <= this.switchCount(threadStack)) {
//    			requestBacktrack();
//    		}
//    		else {
//    			ThreadChoiceFromSet tcg = (ThreadChoiceFromSet)vm.getChoiceGenerator();
//      		  	if (tcg != null) {
//      		  		tcg.reset();
//      		  	}
//    		}
//    	}
    	
      if (checkAndResetBacktrackRequest() || !isNewState() || isEndState() || isIgnoredState() || depthLimitReached) {
        if (!backtrack()) { // backtrack not possible, done
          break;
        }
        
        depthLimitReached = false;
        depth--;
        notifyStateBacktracked();
      }

      if (forward()) {
        depth++;
        notifyStateAdvanced();

        if (currentError != null){
          notifyPropertyViolated();

          if (hasPropertyTermination()) {
            break;
          }
          // for search.multiple_errors we go on and treat this as a new state
          // but hasPropertyTermination() will issue a backtrack request
        }

        if (depth >= depthLimit) {
          depthLimitReached = true;
          notifySearchConstraintHit("depth limit reached: " + depthLimit);
          continue;
        }

        if (!checkStateSpaceLimit()) {
          notifySearchConstraintHit("memory limit reached: " + minFreeMemory);
          // can't go on, we exhausted our memory
          break;
        }

      } else { // forward did not execute any instructions
        notifyStateProcessed();
      }
    }

    notifySearchFinished();
  }
  
  	protected boolean forward () {
//	    currentError = null;
//	    
//	    Transition lastTransition = this.getTransition();
//	    String lastThread = null;
//	    if (lastTransition != null) {
//	    	lastThread = this.getTransition().getThreadInfo().getName();
//	    }
//	    boolean ret = vm.forward();
//	    Transition currentTransition = this.getTransition();
//	    String currentThread = null;
//	    if (currentTransition != null) {
//	    	currentThread = currentTransition.getThreadInfo().getName();
//	    }
//	    
//	    if (lastThread != null && currentThread != null && !currentThread.equals(lastThread)) {
//	    	switchCount++;
//	    }
//
//	    checkPropertyViolation();
//	    return ret;
  		
  		int stateId = this.getStateId();
  		stateSwitch.put(stateId, switchCount(threadStack));
//  		
//  		currentError = null;
//  		
//  		Transition lastTransition = vm.getLastTransition();
//  		Transition currentTransition = vm.getCurrentTransition();
//  		String lastThread = null;
//  		String currentThread = null;
//  		if (lastTransition != null) {
//  			lastThread = lastTransition.getThreadInfo().getName();
//  		}
//  		if (currentTransition != null) {
//  			currentThread = currentTransition.getThreadInfo().getName();
//  		}
//  		
//  		if (lastThread != null && !lastThread.equals(currentThread)) {
//  			switchCount++;
//  		}
//  		boolean ret;
//  		if (switchCount > switchLimit) {
//  			currentTransition.getChoiceGenerator().advance();
//  			switchCount--;
////  			ret = forward();
//  			ret = false;
//  		}
//  		else {
//  			ret = vm.forward();
//
//  	  	    checkPropertyViolation();
//  		}
//  		
//  	    return ret;
  		
  		ThreadChoiceFromSet tcg;
  		if (vm.getNextChoiceGenerator() != null) {
  			tcg = (ThreadChoiceFromSet)vm.getNextChoiceGenerator();
  		}
  		else {
  			tcg = (ThreadChoiceFromSet)vm.getChoiceGenerator();
  		}
  		
  		if (tcg != null && tcg.hasMoreChoices()) {
  			String nextThread = tcg.getChoice(tcg.getCount() + 1).getName();
  			threadStack.push(nextThread);
  			
  			int switchCount = switchCount(threadStack);
  			if (switchCount > switchLimit) {
  				threadStack.pop();
  				tcg.advance();
  				return false;
  			}
  		}
  		
  	 
  		currentError = null;

  	    boolean ret = vm.forward();

  	    checkPropertyViolation();
  	    
  	    assert ret == true;
  	    
  	    return ret;
	}
  	
  	protected boolean backtrack () {
  		if (this.getStateId() != -1) {
  			threadStack.pop();
  		}
  		return super.backtrack();
  	}
  	
  	private int switchCount(Stack<String> threadStack) {
  		int count = 0;
  		for (int i = 0; i < threadStack.size() - 1; i++) {
  			if (!threadStack.get(i).equals(threadStack.get(i + 1))) {
  				count++;
  			}
  		}
  		return count;
  	}


  @Override
  public boolean supportsBacktrack () {
    return true;
  }
}
