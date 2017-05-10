package scheduler.io;

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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import scheduler.listener.DDPListenerAdapter.PathRecorder;
import scheduler.listener.DDPListenerAdapter.ReadWritePair;
import scheduler.listener.DDPListenerAdapter.PathRecorder.SchedulingPoint;

public class FileParser {
	public static void toXML(List<?> list, Boolean result, String path) {
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			
			Element root = doc.createElement("nodes");
			doc.appendChild(root);
			
			for (Object obj : list) {
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
			re.setTextContent(result.toString());
			root.appendChild(re);
			 
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			DOMSource source = new DOMSource(doc);
			transformer.setOutputProperty(OutputKeys.ENCODING, "utf8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			FileOutputStream outputStream = new FileOutputStream(path);
			PrintWriter pw = new PrintWriter(outputStream);
			StreamResult r = new StreamResult(pw);
			transformer.transform(source, r);
			outputStream.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static List<Object> getMixedFromXML(String path) throws Exception {
		List<Object> result = new ArrayList<Object>();
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(path);
		
		NodeList list = doc.getElementsByTagName("nodes").item(0).getChildNodes();
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
	
	public static void stringToFile(String str, String path) throws IOException {
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
		
		file.createNewFile();
		FileOutputStream out = new FileOutputStream(file, false);
		StringBuffer buffer = new StringBuffer();
		buffer.append(str);
		
		out.write(buffer.toString().getBytes());
		out.flush();
		out.close();
	}
	
	public static void delete(File file) {
		if (!file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] childs = file.listFiles();
			for (File child : childs) {
				delete(child);
			}
		}
		file.delete();
	}
	
	public static List<String> getEntries() throws IOException {
		File entries = new File("src/examples/entries.txt");
		if (entries.exists()) {
			InputStreamReader read = new InputStreamReader(new FileInputStream(entries), "utf-8");
			BufferedReader bufferedReader = new BufferedReader(read);
			String lineText = null;
			List<String> mainClasses = new ArrayList<String>();
			while ((lineText = bufferedReader.readLine()) != null) {
				mainClasses.add(lineText);
			}
			read.close();
			
			return mainClasses;
		}
		else {
			return null;
		}
	}
}
