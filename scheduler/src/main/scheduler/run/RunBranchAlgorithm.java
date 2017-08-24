package scheduler.run;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
import org.xml.sax.SAXException;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.listener.DDPListenerAdapter;
import scheduler.listener.DDPListenerAdapter.PathRecorder;
import scheduler.listener.DDPListenerAdapter.PathRecorder.SchedulingPoint;
import scheduler.listener.DDPListenerAdapter.ReadWritePair;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.listener.DDPListenerAdapter.STATUS;

public class RunBranchAlgorithm {
	public static final int MAX_TIME = 100;
	
	public static int DIVIDE_PART = 10;
	
	public static final int REPEAT_TIME = 5;
	
	public static void main(String[] args) throws Exception {
		List<String> mainClasses = new ArrayList<String>();
		//String entry = "accountsubtype.Main";
		
		File entries = new File("src/examples/entries.txt");
		if (entries.exists()) {
			InputStreamReader read = new InputStreamReader(new FileInputStream(entries), "utf-8");
			BufferedReader bufferedReader = new BufferedReader(read);
			String lineText = null;
			while ((lineText = bufferedReader.readLine()) != null) {
				mainClasses.add(lineText);
			}
			read.close();
		}
		else {
			System.out.println("Error: file can not be found under src / examples entry.txt");
			return;
		}
		
		for (String entry : mainClasses) {
			executeMainClass(entry);
		}
		
		
		
		
		
		/*
		PathRecorder pathRecorder = fromXML("F:\\path.xml");
		
		//pathRecorder.swap(-2, -3);
		
		SchedulingPoint sp;
		while ((sp = pathRecorder.pop()) != null) {
			System.out.println(sp);
		}
		pathRecorder.reset();
		
		System.out.println("Reproduce:");
		
		String[] str = new String[]{
				"+classpath=build/examples", 
				"+search.class=scheduler.search.WithoutBacktrack", 
				"ScheduleTest"};
		
		Config config = new Config(str);
		
		JPF jpf = new JPF(config);
		DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.REPRODUCE, pathRecorder);
		jpf.addPropertyListener(listener);
		jpf.run();
		*/
	}
	
	public static void executeMainClass(String entry) throws Exception {
		File outputDir = new File("F:\\output\\" + entry);
		if (outputDir.exists()) {
			System.out.println("Output the folder " + entry + " Already exists, skip.");
			return;
		}
		else {
			outputDir.mkdirs();
		}
		
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
		
		
		PathRecorder pathRecorder = null;
		for (int i = 0; i < MAX_TIME; i++) {
			JPF jpf = new JPF(config);
			DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.RUN, null, null, null, searchScope);
			jpf.addPropertyListener(listener);
			jpf.run();
			
			if (listener.result.pathRecorder.success == false) {
				pathRecorder = listener.result.pathRecorder;
				break;
			}
		}
		
		if (pathRecorder == null) {
			System.out.println("Did not find the failed run sequence");
			return;
		}


		DDPListenerAdapter listener = new DDPListenerAdapter(STATUS.REPRODUCE, pathRecorder, null, null, searchScope);
		JPF jpf = new JPF(config);
		jpf.addPropertyListener(listener);
		jpf.run();
		toXML(listener.result, "output\\" + entry + "\\fail.xml");
		
		int pathLength = pathRecorder.sps.size();
		
		if (pathLength < DIVIDE_PART) {
			DIVIDE_PART = pathLength;
		}
		
		for (int i = 1; i <= DIVIDE_PART; i++) {
			int branchPoint = pathLength * i / DIVIDE_PART - 1;
			
			for (int j = 0; j < REPEAT_TIME; j++) {
				listener = new DDPListenerAdapter(STATUS.REPRODUCE, pathRecorder, null, null, searchScope, branchPoint);
				jpf = new JPF(config);
				jpf.addPropertyListener(listener);
				jpf.run();
				
				toXML(listener.result, "output\\" + entry + "\\branch-" + i + "-" + j + ".xml");
			}
		}
	}
	
	public static void delete(File root) {
		if (root.isDirectory()) {
			File[] files = root.listFiles();
			for (File file : files) {
				delete(file);
			}
		}
		
		root.delete();
	}
	
	public static void toXML(PathRecorder pathRecorder, String name) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			
			Element root = doc.createElement("path");
			doc.appendChild(root);
			SchedulingPoint sp;
			while ((sp = pathRecorder.pop()) != null) {			
				Element schedulingPoint = doc.createElement("schedulingPoint");
				Element thread = doc.createElement("thread");
				thread.setTextContent(sp.nextThread);
				Element instruction = doc.createElement("instruction");
				instruction.setTextContent(sp.nextInstruction);
				Element type = doc.createElement("type");
				type.setTextContent(sp.nextInstructionType);
				 
				schedulingPoint.appendChild(thread);
				schedulingPoint.appendChild(instruction);
				schedulingPoint.appendChild(type);
				root.appendChild(schedulingPoint);
			}
			 
			Element result = doc.createElement("success");
			result.setTextContent(pathRecorder.success.toString());
			root.appendChild(result);
			 
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
			pathRecorder.reset();
		}
	}
	
	public static void toXML(Result result, String name) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			
			Element root = doc.createElement("pairs");
			doc.appendChild(root);
			
			Element success = doc.createElement("success");
			success.setTextContent(result.success.toString());
			root.appendChild(success);
			Element branchPoint = doc.createElement("branchPoint");
			branchPoint.setTextContent(result.branchPoint.toString());
			root.appendChild(branchPoint);
			
			for (ReadWritePair pair : result.pairs) {
				Element pairElem = doc.createElement("pair");
				Element id = doc.createElement("id");
				id.setTextContent(pair.id.toString());
				Element instance = doc.createElement("instance");
				instance.setTextContent(pair.instance);
				Element field = doc.createElement("field");
				field.setTextContent(pair.field);
				Element thread = doc.createElement("thread");
				thread.setTextContent(pair.thread);
				Element type = doc.createElement("type");
				type.setTextContent(pair.type);
				Element location = doc.createElement("location");
				location.setTextContent(pair.location);
				Element inBranch = doc.createElement("inBranch");
				inBranch.setTextContent(pair.inBranch.toString());
				
				pairElem.appendChild(id);
				pairElem.appendChild(instance);
				pairElem.appendChild(field);
				pairElem.appendChild(thread);
				pairElem.appendChild(type);
				pairElem.appendChild(location);
				pairElem.appendChild(inBranch);
				
				root.appendChild(pairElem);
			}
			 
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
		}
	}
	
	public static PathRecorder fromXML(String filePath) throws ParserConfigurationException, SAXException, IOException {
		PathRecorder pathRecorder = new PathRecorder();
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(filePath);
		
		NodeList spList = doc.getElementsByTagName("schedulingPoint");
		for (int i = 0; i < spList.getLength(); i++) {
			Element sp = (Element)spList.item(i);
			
			Element idElem = (Element)sp.getElementsByTagName("id").item(0);
			Element threadElem = (Element)sp.getElementsByTagName("thread").item(0);
			Element insnElem = (Element)sp.getElementsByTagName("instruction").item(0);
			Element typeElem = (Element)sp.getElementsByTagName("type").item(0);
			int id = Integer.parseInt(idElem.getTextContent());
			String thread = threadElem.getTextContent();
			String insn = insnElem.getTextContent();
			String type = typeElem.getTextContent();
			
			pathRecorder.addSchedulingPoint(id, thread, insn, type);
		}
		
		String success = doc.getElementsByTagName("success").item(0).getTextContent();
		pathRecorder.addResult(success.equals("true") ? true : false);
		
		return pathRecorder;
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
}
