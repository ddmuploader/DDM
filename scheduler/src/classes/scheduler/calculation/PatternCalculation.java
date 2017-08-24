package scheduler.calculation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.Pattern.PatternNode;
import scheduler.listener.DDPListenerAdapter.ReadWritePair;

public class PatternCalculation {
	public static List<Pattern> sub(List<Pattern> left, List<Pattern> right) {
		List<Pattern> result = new ArrayList<Pattern>();
		for (Pattern p : left) {
			if (!isIn(p, result) && !isIn(p, right)) {
				result.add(p);
			}
		}
		return result;
	}
	
	private static boolean isIn(Pattern pattern, List<Pattern> list) {
		for (Pattern p : list) {
			if (p.isSamePattern(pattern)) {
				return true;
			}
		}
		return false;
	}
}
