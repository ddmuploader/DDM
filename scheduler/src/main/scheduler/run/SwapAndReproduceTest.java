package scheduler.run;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.listener.DDPListenerAdapter;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.listener.DDPListenerAdapter.STATUS;
import scheduler.replay.Replay;
import scheduler.listener.DDPListenerAdapter.Choice;
import scheduler.listener.DDPListenerAdapter.PathRecorder;
import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.PathRecorder.SchedulingPoint;
import scheduler.listener.DDPListenerAdapter.Pattern.PatternNode;
import scheduler.listener.DDPListenerAdapter.ReadWritePair;

public class SwapAndReproduceTest {
	public static int MAX_TIME = 20;
	
	public static String entry = "hashcodetest.HashCodeTest";
	
	public static void main(String[] args) throws Exception {

		
		
		Result fail = null;
		Result success = null;
		while (fail == null || success == null) {
			Result r = execute();
			if (r.success) {
				success = r;
			}
			else {
				fail = r;
			}
		}
		toXML(fail, ".\\output\\fail.xml");
		toXML(success, ".\\output\\success.xml");
		
		
		List<Object> fmixed = fromXML(".\\output\\fail.xml");
		List<Object> smixed = fromXML(".\\output\\success.xml");
		PathRecorder fp = toPathRecorder(fmixed);
		PathRecorder sp = toPathRecorder(smixed);
		
		Result fr = tryRun(fp);
		Result sr = tryRun(sp);
		List<Pattern> patterns = initPatterns();
		fr.matchPatterns(patterns);
		sr.matchPatterns(patterns);
		
		List<Pattern> ans = fr.matchedPatterns;
		ans.removeAll(sr.matchedPatterns);
		
		int nullNumber = 0;
		int finishedNumber = 0;
		int i = 1;
		for (Pattern p : ans) {
			System.out.println(p);
			File dir = new File(".\\output\\p" + i);
			dir.mkdir();
			File intro = new File(".\\output\\p" + i + "\\message.txt");
			intro.createNewFile();
			FileOutputStream out = new FileOutputStream(intro, false);
			StringBuffer buffer = new StringBuffer();
			buffer.append(p.toString() + "\n");
			out.write(buffer.toString().getBytes());
			out.close();
			toXML(fr, ".\\output\\p" + i + "\\before-interrupt.xml");
			
			List<Pattern> toInterrupt = new ArrayList<Pattern>();
			toInterrupt.add(p);
//			toXML(fr, ".\\output\\mixed.xml");
			List<Object> after = Result.interruptPatterns(toInterrupt, fr.mixed);
			if (after == null) {
				System.out.println("null");
				nullNumber++;
			}
			else {
				PathRecorder pr = toPathRecorder(after);
				Result r = tryRun(pr);
				
//				toXML(after, r.finished, ".\\output\\p" + i + "\\after-interrupt-" + (r.finished ? "finished" : "notFinished") + ".xml");
				int a, b;
				for (a = 0; a < after.size(); a++) {
					if (after.get(a) instanceof SchedulingPoint) {
						SchedulingPoint s = (SchedulingPoint)after.get(a);
						if (!s.identical(fr.mixed.get(a))) {
							break;
						}
					}
					else {
						ReadWritePair rw = (ReadWritePair)after.get(a);
						if (!rw.identical(fr.mixed.get(a))) {
							break;
						}
					}
				}
				for (b = after.size() - 1; b >= 0; b--) {
					if (after.get(b) instanceof SchedulingPoint) {
						SchedulingPoint s = (SchedulingPoint)after.get(b);
						if (!s.identical(fr.mixed.get(b))) {
							break;
						}
					}
					else {
						ReadWritePair rw = (ReadWritePair)after.get(b);
						if (!rw.identical(fr.mixed.get(b))) {
							break;
						}
					}
				}
				
				File be = new File(".\\output\\p" + i + "\\before.txt");
				be.createNewFile();
				out = new FileOutputStream(be, false);
				buffer = new StringBuffer();
				
				
				
				for (int j = a; j <= b; j++) {
					buffer.append(fr.mixed.get(j).toString() + "\n");
				}
				
				out.write(buffer.toString().getBytes());
				out.close();
				
				File af = new File(".\\output\\p" + i + "\\after-" + (r.finished ? "finished" : "not-finished") + ".txt");
				af.createNewFile();
				out = new FileOutputStream(af, false);
				buffer = new StringBuffer();
				
				for (int j = a; j <= b; j++) {
					buffer.append(after.get(j).toString() + "\n");
				}
				if (r.finished == false) {
					buffer.append("There is no corresponding choice of scheduling points£º" + r.sp + "\n");
					buffer.append("A list of selection points for scheduling points: " + "\n");
//					for (Choice choice : r.choices) {
//						buffer.append("thread: " + choice.thread + " instruction: " + choice.instruction + " type: " + choice.type + "\n");
//					}
				}
				
				r.matchPatterns(patterns);
				List<Pattern> include = new ArrayList<Pattern>();
				for (Pattern pattern : ans) {
					include.add(pattern);
				}
				include.remove(p);
				System.out.println("include size: " + include.size());
				List<Pattern> exclude = new ArrayList<Pattern>();
				exclude.add(p);
				System.out.println("exclude size: " + exclude.size());
				boolean bo = Replay.expectPatterns(r.matchedPatterns, include, exclude, buffer);
				buffer.append(MessageFormat.format("pattern In line with: {0}\n", bo ? "true" : "false"));
				
				
				
				out.write(buffer.toString().getBytes());
				out.close();
				
				toXML(r, ".\\output\\p" + i + "\\after-interrupt.xml");
				
				if (r.finished) {
					finishedNumber++;
				}
			}
			i++;
		}
		
		System.out.println("nullNumber: " + nullNumber + " / " + ans.size());
		System.out.println("finishedNumber: " + finishedNumber + " / " + ans.size());
		
//		List<Object> fmixed = fromXML("F:\\output\\fail.xml");
//		List<Object> smixed = fromXML("F:\\output\\success.xml");
//		PathRecorder fp = toPathRecorder(fmixed);
//		PathRecorder sp = toPathRecorder(smixed);
//		
//		Result fr = tryRun(fp);
//		Result sr = tryRun(sp);
//		List<Pattern> patterns = initPatterns();
//		fr.matchPatterns(patterns);
//		sr.matchPatterns(patterns);
//		
//		List<Pattern> ans = fr.matchedPatterns;
//		ans.removeAll(sr.matchedPatterns);
//		
//		for (Pattern p : ans) {
//			System.out.println(p);
//		}
	}
	
	public static Result execute() throws Exception {
		File directory = new File("src/examples");
		File[] files = directory.listFiles();
		List<String> searchScope = new ArrayList<String>();
		for (File file : files) {
			if (file.isDirectory()) {
				searchScope.addAll(searchFiles(file));
			}
		}
	
		String[] str = new String[]{
				"+classpath=build/examples", 
				"+search.class=scheduler.search.WithoutBacktrack", 
				entry};
		Config config = new Config(str);
		DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.RUN, null, null, null, searchScope);
		JPF jpf = new JPF(config);
		jpf.addPropertyListener(listener);
		jpf.run();
		return listener.result;
	}
	
	public static List<String> searchFiles(File file) {
		List<String> result = new ArrayList<String>();
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				result.addAll(searchFiles(f));
			}
		}
		else {
		
			String path = file.getPath().replaceAll("^src\\\\examples\\\\", "")
					.replaceAll("\\\\", "/");
			result.add(path);
		}
		return result;
	}
	
	public static void toXML(List<Object> result, Boolean success, String name) {
		File file = new File(name);
		if (file.exists()) {
			file.delete();
		}
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			
			Element root = doc.createElement("mixed");
			doc.appendChild(root);
//			SchedulingPoint sp;
//			while ((sp = pathRecorder.pop()) != null) {			
//				Element schedulingPoint = doc.createElement("schedulingPoint");
//				Element thread = doc.createElement("thread");
//				thread.setTextContent(sp.nextThread);
//				Element instruction = doc.createElement("instruction");
//				instruction.setTextContent(sp.nextInstruction);
//				Element type = doc.createElement("type");
//				type.setTextContent(sp.nextInstructionType);
//				 
//				schedulingPoint.appendChild(thread);
//				schedulingPoint.appendChild(instruction);
//				schedulingPoint.appendChild(type);
//				root.appendChild(schedulingPoint);
//			}
			
			for (Object obj : result) {
				if (obj instanceof SchedulingPoint) {
					SchedulingPoint sp = (SchedulingPoint)obj;
					Element schedulingPoint = doc.createElement("schedulingPoint");
					Element id = doc.createElement("id");
					id.setTextContent(sp.id.toString());
					Element thread = doc.createElement("thread");
					thread.setTextContent(sp.nextThread);
					Element instruction = doc.createElement("instruction");
					instruction.setTextContent(sp.nextInstruction);
					Element type = doc.createElement("type");
					type.setTextContent(sp.nextInstructionType);
					 
					schedulingPoint.appendChild(id);
					schedulingPoint.appendChild(thread);
					schedulingPoint.appendChild(instruction);
					schedulingPoint.appendChild(type);
					root.appendChild(schedulingPoint);
				}
				else if (obj instanceof ReadWritePair) {
					ReadWritePair pair = (ReadWritePair)obj;
					Element readWritePair = doc.createElement("readWritePair");
					Element id = doc.createElement("id");
					id.setTextContent(pair.id.toString());
					Element instance = doc.createElement("instance");
					instance.setTextContent(pair.instance);
					Element field = doc.createElement("field");
					field.setTextContent(pair.field);
					Element type = doc.createElement("type");
					type.setTextContent(pair.type);
					Element thread = doc.createElement("thread");
					thread.setTextContent(pair.thread);
					Element location = doc.createElement("location");
					location.setTextContent(pair.location);
					Element inBranch = doc.createElement("inBranch");
					inBranch.setTextContent(pair.inBranch.toString());
					
					readWritePair.appendChild(id);
					readWritePair.appendChild(instance);
					readWritePair.appendChild(field);
					readWritePair.appendChild(type);
					readWritePair.appendChild(thread);
					readWritePair.appendChild(location);
					readWritePair.appendChild(inBranch);
					root.appendChild(readWritePair);
				}
				else {
					throw new Exception("unknown object type.");
				}
			}
			 
			Element re = doc.createElement("success");
			re.setTextContent(success.toString());
			root.appendChild(re);
			 
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			DOMSource source = new DOMSource(doc);
			transformer.setOutputProperty(OutputKeys.ENCODING, "utf8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			PrintWriter pw = new PrintWriter(new FileOutputStream(name));
			StreamResult r = new StreamResult(pw);
			transformer.transform(source, r);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			//pathRecorder.reset();
		}
	}
	
	public static void toXML(Result result, String name) {
		File file = new File(name);
		if (file.exists()) {
			file.delete();
		}
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			
			Element root = doc.createElement("mixed");
			doc.appendChild(root);
//			SchedulingPoint sp;
//			while ((sp = pathRecorder.pop()) != null) {			
//				Element schedulingPoint = doc.createElement("schedulingPoint");
//				Element thread = doc.createElement("thread");
//				thread.setTextContent(sp.nextThread);
//				Element instruction = doc.createElement("instruction");
//				instruction.setTextContent(sp.nextInstruction);
//				Element type = doc.createElement("type");
//				type.setTextContent(sp.nextInstructionType);
//				 
//				schedulingPoint.appendChild(thread);
//				schedulingPoint.appendChild(instruction);
//				schedulingPoint.appendChild(type);
//				root.appendChild(schedulingPoint);
//			}
			
			for (Object obj : result.mixed) {
				if (obj instanceof SchedulingPoint) {
					SchedulingPoint sp = (SchedulingPoint)obj;
					Element schedulingPoint = doc.createElement("schedulingPoint");
					Element id = doc.createElement("id");
					id.setTextContent(sp.id.toString());
					Element thread = doc.createElement("thread");
					thread.setTextContent(sp.nextThread);
					Element instruction = doc.createElement("instruction");
					instruction.setTextContent(sp.nextInstruction);
					Element type = doc.createElement("type");
					type.setTextContent(sp.nextInstructionType);
					 
					schedulingPoint.appendChild(id);
					schedulingPoint.appendChild(thread);
					schedulingPoint.appendChild(instruction);
					schedulingPoint.appendChild(type);
					root.appendChild(schedulingPoint);
				}
				else if (obj instanceof ReadWritePair) {
					ReadWritePair pair = (ReadWritePair)obj;
					Element readWritePair = doc.createElement("readWritePair");
					Element id = doc.createElement("id");
					id.setTextContent(pair.id.toString());
					Element instance = doc.createElement("instance");
					instance.setTextContent(pair.instance);
					Element field = doc.createElement("field");
					field.setTextContent(pair.field);
					Element type = doc.createElement("type");
					type.setTextContent(pair.type);
					Element thread = doc.createElement("thread");
					thread.setTextContent(pair.thread);
					Element location = doc.createElement("location");
					location.setTextContent(pair.location);
					Element inBranch = doc.createElement("inBranch");
					inBranch.setTextContent(pair.inBranch.toString());
					
					readWritePair.appendChild(id);
					readWritePair.appendChild(instance);
					readWritePair.appendChild(field);
					readWritePair.appendChild(type);
					readWritePair.appendChild(thread);
					readWritePair.appendChild(location);
					readWritePair.appendChild(inBranch);
					root.appendChild(readWritePair);
				}
				else {
					throw new Exception("unknown object type.");
				}
			}
			 
			Element re = doc.createElement("success");
			re.setTextContent(result.success.toString());
			root.appendChild(re);
			 
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			DOMSource source = new DOMSource(doc);
			transformer.setOutputProperty(OutputKeys.ENCODING, "utf8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			PrintWriter pw = new PrintWriter(new FileOutputStream(name));
			StreamResult r = new StreamResult(pw);
			transformer.transform(source, r);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			//pathRecorder.reset();
		}
	}
	
	public static List<Object> fromXML(String filePath) throws Exception {
		List<Object> result = new ArrayList<Object>();
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(filePath);
		
//		NodeList spList = doc.getElementsByTagName("schedulingPoint");
//		for (int i = 0; i < spList.getLength(); i++) {
//			Element sp = (Element)spList.item(i);
//			
//			Element threadElem = (Element)sp.getElementsByTagName("thread").item(0);
//			Element insnElem = (Element)sp.getElementsByTagName("instruction").item(0);
//			Element typeElem = (Element)sp.getElementsByTagName("type").item(0);
//			String thread = threadElem.getTextContent();
//			String insn = insnElem.getTextContent();
//			String type = typeElem.getTextContent();
//			
//			pathRecorder.addSchedulingPoint(thread, insn, type, null);
//		}

		NodeList list = doc.getElementsByTagName("mixed").item(0).getChildNodes();
		for (int i = 0; i < list.getLength() - 1; i++) {
			try {
				Element elem = (Element)list.item(i);
				
				if (elem.getTagName().equals("schedulingPoint")) {
					Element threadElem = (Element)elem.getElementsByTagName("thread").item(0);
					Element insnElem = (Element)elem.getElementsByTagName("instruction").item(0);
					Element typeElem = (Element)elem.getElementsByTagName("type").item(0);
					String thread = threadElem.getTextContent();
					String insn = insnElem.getTextContent();
					String type = typeElem.getTextContent();
					
					result.add(new SchedulingPoint(thread, insn, type));
				}
				else if (elem.getTagName().equals("readWritePair")) {
					Element idElem = (Element)elem.getElementsByTagName("id").item(0);
					Element instanceElem = (Element)elem.getElementsByTagName("instance").item(0);
					Element fieldElem = (Element)elem.getElementsByTagName("field").item(0);
					Element typeElem = (Element)elem.getElementsByTagName("type").item(0);
					Element threadElem = (Element)elem.getElementsByTagName("thread").item(0);
					Element locationElem = (Element)elem.getElementsByTagName("location").item(0);
					Element inBranchElem = (Element)elem.getElementsByTagName("inBranch").item(0);
					
					int id = Integer.parseInt(idElem.getTextContent());
					String instance = instanceElem.getTextContent();
					String field = fieldElem.getTextContent();
					String type = typeElem.getTextContent();
					String thread = threadElem.getTextContent();
					String location = locationElem.getTextContent();
					boolean inBranch = Boolean.parseBoolean(inBranchElem.getTextContent());
					
					ReadWritePair pair = new ReadWritePair(instance, field, type, thread, location, inBranch);
					pair.id = id;
					result.add(pair);
				}
				else if (elem.getTagName().equals("success")) {
					continue;
				}
				else {
					throw new Exception("unknown element." + elem.getTagName());
				}
			}
			catch (ClassCastException e) {
				continue;
			}
		}
		
		return result;
	}
	
	public static PathRecorder toPathRecorder(List<Object> mixed) {
		PathRecorder pathRecorder = new PathRecorder();
		for (Object obj : mixed) {
			if (obj instanceof SchedulingPoint) {
				SchedulingPoint sp = (SchedulingPoint)obj;
				pathRecorder.addSchedulingPoint(sp.id, sp.nextThread, sp.nextInstruction, sp.nextInstructionType);
			}
		}
		return pathRecorder;
	}
	
	public static Result tryRun(PathRecorder pathRecorder) throws Exception {
		File directory = new File("src/examples");
		File[] files = directory.listFiles();
		List<String> searchScope = new ArrayList<String>();
		for (File file : files) {
			if (file.isDirectory()) {
				searchScope.addAll(searchFiles(file));
			}
		}
		String[] str = new String[]{
				"+classpath=build/examples", 
				"+search.class=scheduler.search.WithoutBacktrack", 
				entry};
		Config config = new Config(str);
		DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.REPRODUCE, pathRecorder, null, null, searchScope);
		JPF jpf = new JPF(config);
		jpf.addPropertyListener(listener);
		jpf.run();
		
		return listener.result;
	}
	
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
}
