package scheduler.init;

import java.util.ArrayList;
import java.util.List;

import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.Pattern.PatternNode;

public class PatternInit {
	public static List<Pattern> initPatterns() {
		List<Pattern> patterns = new ArrayList<Pattern>();
		Pattern pattern;
		PatternNode node;
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		return patterns;
	}
	
	public static List<Pattern> initUnicornPatterns() {
		List<Pattern> patterns = new ArrayList<Pattern>();
		Pattern pattern;
		PatternNode node;
		
		/*
		 * P1
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P2
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P3
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P4
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P5
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P6
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P7
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P8
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P9
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P10
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P11
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P12
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P13
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P14
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P15
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P16
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		return patterns;
	}
	
	/**
	 * 初始化UNICORN中涉及到多变量的pattern
	 * @return
	 */
	public static List<Pattern> initConsisitencyPatterns() {
		List<Pattern> patterns = new ArrayList<Pattern>();
		Pattern pattern;
		PatternNode node;
		
		/*
		 * P9
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P10
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P11
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P12
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P13
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P14
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P15
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "READ");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		/*
		 * P16
		 */
		pattern = new Pattern();
		node = new PatternNode("x", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread2", "WRITE");
		pattern.nodes.add(node);
		node = new PatternNode("y", "thread1", "READ");
		pattern.nodes.add(node);
		node = new PatternNode("x", "thread2", "WRITE");
		pattern.nodes.add(node);
		patterns.add(pattern);
		
		return patterns;
	}
 }
