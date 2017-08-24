package scheduler.run;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.listener.DDPListenerAdapter;
import scheduler.listener.DDPListenerAdapter.PathRecorder;
import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.ReadWritePair;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.listener.DDPListenerAdapter.STATUS;
import scheduler.listener.DDPListenerAdapter.PathRecorder.SchedulingPoint;
import scheduler.listener.DDPListenerAdapter.Pattern.PatternNode;

public class _TestPattern {
	public static boolean t;
	
	public static List getValue() throws Exception{
		
		List res = new ArrayList<>();
		
		Result r = execute();
		
		//ÕÒ³öfailµÄ
		while (r.success) {
			r = execute();
		}
		toXML(r.mixed, r.success, ".\\mixedSequence.xml");		
		List<Object> failMixed = fromXML(".\\mixedSequence.xml");
		PathRecorder pathRecorderFail = toPathRecorder(failMixed);
		Result failResult = tryRun(pathRecorderFail);
		List<Pattern> failPattern = initPatterns();
		failResult.matchPatterns(failPattern);
		
		res.add(failMixed);
		res.add(failResult);
		

		while(!r.success){
			r = execute();
		}
		toXML(r.mixed, r.success, ".\\mixedSequence.xml");		
		List<Object> successMixed = fromXML(".\\mixedSequence.xml");
		PathRecorder pathRecorderSuccess = toPathRecorder(successMixed);
		Result successResult = tryRun(pathRecorderSuccess);
		List<Pattern> successPattern = initPatterns();
		successResult.matchPatterns(successPattern);
		
		res.add(successResult);
		
		return res;
	}
	
	
	public static void main(String[] args) throws Exception {

			
		Result r = execute();
		while (r.success) {
			r = execute();
		}
		
		toXML(r.mixed, r.success, ".\\mixedSequence.xml");
		toXML(r.pairs, r.success,  ".\\failed-readWritePairs.xml");
		
		List<Object> mixed = fromXML(".\\mixedSequence.xml");
		PathRecorder pathRecorder = toPathRecorder(mixed);
		Result result = tryRun(pathRecorder);
		List<Pattern> patterns = initPatterns();
		result.matchPatterns(patterns);
		
		for (Pattern pattern : result.matchedPatterns) {
			System.out.println(pattern);
		}
		
		
		
//		
//		int count = 0,success = 0,unknown = 0;
//		
//		
//		
//		for (Pattern p : result.matchedPatterns) {
//			List<Pattern> interPatterns = new ArrayList<Pattern>();
//			interPatterns.add(p);
//			
//			List<Object> interruptPatterns = result.interruptPatterns(interPatterns);
//			PathRecorder interPathRecorder = toPathRecorder(interruptPatterns);
//			Result interResult = tryRun(interPathRecorder);
//		}
		
		
		
	}
	
	public static <T> void test(List<T> list) {
		T a = list.get(0);
		if (a instanceof String) {
			System.out.println("true");
		}
		else {
			System.out.println("false");
		}
	}
	
	public static Result execute() throws Exception {
		String entry = "account.Main";
		
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
	
	public static <T> void toXML(List<T> result, Boolean success, String name) {
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
		String entry = "account.Main";
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
