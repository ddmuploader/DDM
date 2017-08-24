package scheduler.enumerate;

import java.util.List;

import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.Result;

public enum TestResult {
	SUCCESS,
	FAIL,
	UNKNOWN;
	
	public Result result;

	TestResult(){
		this.result = null;
	}
	
	
}
