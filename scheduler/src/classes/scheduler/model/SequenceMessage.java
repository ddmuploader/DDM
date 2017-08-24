package scheduler.model;

import java.util.ArrayList;
import java.util.List;

	public class SequenceMessage {
		public List<ReadWriteNode> RWNodes;
		public List<ChoiceGeneratorNode> CGNodes;
		public boolean isSuccess;
		public String errorMessage;
		public List<Pattern> patterns;
		
		public SequenceMessage() {
			RWNodes = null;
			CGNodes = null;
			isSuccess = false;
			errorMessage = null;
			patterns = null;
		}
		
		public List<ReadWriteNode> RWNodesfilter(String element, String field, String type, String thread, String line) {
			List<ReadWriteNode> result = new ArrayList<ReadWriteNode>();
			for (ReadWriteNode node : RWNodes) {
				boolean in = true;

				if (element != null && !node.element.contains(element)) {
					in = false;
				}
				else if (field != null && !node.field.contains(field)) {
					in = false;
				}
				else if (type != null && !node.type.equals(type)) {
					in = false;
				}
				else if (thread != null && !node.thread.equals(thread)) {
					in = false;
				}
				else if (line != null && !node.line.contains(line)) {
					in = false;
				}
				
				if (in) {
					result.add(node);
				}
			}
			return result;
		}
		
		private int getIndexOfNextRWNode(int index) {
			ReadWriteNode node = RWNodes.get(index);
			for (int i = index + 1; i < RWNodes.size(); i++) {
				ReadWriteNode p = RWNodes.get(i);
				if (node.element.equals(p.element) && node.field.equals(p.field) 
						&& node.thread.equals(p.thread) && node.type.equals(p.type) && node.line.equals(p.line)) {
					return i;
				}
			}
			return -1;
		}
		
		public void removeDeprecatedRWNodes() {

			start:
			while (true) {
				for (int i = 0; i < RWNodes.size(); i++) {
					ReadWriteNode node = RWNodes.get(i);
					if (node.type.equals("WRITE") && node.changed == false) {
						RWNodes.remove(i);
						continue start;
					}
					if (node.type.equals("READ") && this.getIndexOfNextRWNode(i) != -1) {
						RWNodes.remove(i);
						continue start;
					}
				}
				break;
			}
		}
		

		public List<Pattern> containInterleavings(List<Pattern> interleavings) {
			
			List<Pattern> patterns = new ArrayList<Pattern>();
			
			List<ReadWriteNode> vars = getAllVariables();
			for (ReadWriteNode var : vars) {
				List<ReadWriteNode> nodes = this.RWNodesfilter(var.element, var.field, null, null, null);
				for (Pattern interleaving : interleavings) {
					int length = 3;
					int start = 0;
					while (start <= nodes.size() - length) {
						if (!(interleaving.nodes.get(0).type.equals(nodes.get(start).type) 
								&& interleaving.nodes.get(1).type.equals(nodes.get(start + 1).type)
								&& interleaving.nodes.get(2).type.equals(nodes.get(start + 2).type))) {
							start++;
							continue;
						}
						
						if (!(interleaving.nodes.get(0).thread.equals(interleaving.nodes.get(1).thread) 
								== nodes.get(start).thread.equals(nodes.get(start + 1).thread)
							&& interleaving.nodes.get(1).thread.equals(interleaving.nodes.get(2).thread) 
								== nodes.get(start + 1).thread.equals(nodes.get(start + 2).thread)
							&& interleaving.nodes.get(0).thread.equals(interleaving.nodes.get(2).thread) 
								== nodes.get(start).thread.equals(nodes.get(start + 2).thread))) {
							start++;
							continue;
						}
						//fits = true;
						Pattern pattern = new Pattern();
						pattern.nodes.add(nodes.get(start));
						pattern.nodes.add(nodes.get(start + 1));
						pattern.nodes.add(nodes.get(start + 2));
						patterns.add(pattern);
						
						start++;
					}
				}
			}
			/*
			for (Pattern interleaving : interleavings) {
				int length = 3;
				int start = 0;
				while (start <= RWNodes.size() - length) {
					
					if (!(interleaving.nodes.get(0).type.equals(RWNodes.get(start).type) 
							&& interleaving.nodes.get(1).type.equals(RWNodes.get(start + 1).type)
							&& interleaving.nodes.get(2).type.equals(RWNodes.get(start + 2).type))) {
						start++;
						continue;
					}
					
					if (!(interleaving.nodes.get(0).thread.equals(interleaving.nodes.get(1).thread) 
							== RWNodes.get(start).thread.equals(RWNodes.get(start + 1).thread)
						&& interleaving.nodes.get(1).thread.equals(interleaving.nodes.get(2).thread) 
							== RWNodes.get(start + 1).thread.equals(RWNodes.get(start + 2).thread)
						&& interleaving.nodes.get(0).thread.equals(interleaving.nodes.get(2).thread) 
							== RWNodes.get(start).thread.equals(RWNodes.get(start + 2).thread))) {
						start++;
						continue;
					}
					//fits = true;
					Pattern pattern = new Pattern();
					pattern.nodes.add(RWNodes.get(start));
					pattern.nodes.add(RWNodes.get(start + 1));
					pattern.nodes.add(RWNodes.get(start + 2));
					patterns.add(pattern);
					
					start++;
				}
			}
			*/
			return patterns;
		}
		
		
		public List<ReadWriteNode> getAllVariables() {
			List<ReadWriteNode> result = new ArrayList<ReadWriteNode>();
			nextNode:
			for (ReadWriteNode node : RWNodes) {
				for (int i = 0; i < result.size(); i++) {
					if (node.element.equals(result.get(i).element) && node.field.equals(result.get(i).field)) {
						continue nextNode;
					}
				}
				result.add(new ReadWriteNode(node.element, node.field, null, null, null, false));
			}
			return result;
		}
		
		public boolean containsPattern(Pattern pattern) {
			if (patterns == null) {
				List<Pattern> interleavings = Pattern.getInterleavings();
				patterns = containInterleavings(interleavings);
			}
			for (Pattern p : patterns) {
				if (p.equals(pattern)) {
					return true;
				}
			}
			return false;
		}
	}
