package scheduler.run;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.listener.FalconImplListener;
import scheduler.listener.MonitorListenerAdapter;
import scheduler.listener.MonitorListenerAdapter.Filter;
import scheduler.listener.MonitorListenerAdapter.ReadWritePair;
import scheduler.listener.MonitorListenerAdapter.Result;
import scheduler.model.ReadWriteNode;
import scheduler.model.SequenceMessage;
import scheduler.run.Run2.XmlConverter;
/*
 * 
 */
public class SequenceProduce {
	public static final int REPEAT_TIME = 200;
	
	public static void main(String[] args) {
		Pattern pattern = Pattern.compile("(.*Test.*)|(.*[Mm]ain.*)|(,*Simulator.*)");
		File directory = new File("src/examples");
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
			
				
				String packageName = file.getName();
				File testFile = new File("ReadWritePairs-" + packageName + ".xml");
				if (testFile.exists()) {
					System.out.println("Skip the package£º" + packageName + ", The output file already exists");
					continue;
				}
				else {
					System.out.println("Start testing package£º" + packageName + "...");
				}
				
				String[] cls = file.list();
				List<String> match = new ArrayList<String>();
				List<String> fileFilter = new ArrayList<String>();
				for (String cl : cls) {
					fileFilter.add(cl);
					if (pattern.matcher(cl).matches()) {
						match.add(cl);
						//System.out.println(cl);
					}
				}
				
				String entry = "";
				if (file.list().length == 1) {
					entry = packageName + "." + file.list()[0];
					entry = entry.substring(0, entry.indexOf(".java"));
					System.out.println("Find the project entrance£º " + entry);
				}
				else if (match.size() == 1) {
					entry = match.get(0);
					entry = packageName + "." + entry.substring(0, entry.indexOf(".java"));
					System.out.println("Find the project entrance£º " + entry);
				}
				else {
					System.out.println("Skip the package£º" + packageName + " Failed to automatically search for entry files");
					continue;
				}
				
				String[] str = new String[]{
						"+classpath=build/examples", 
						"+search.class=scheduler.search.WithoutBacktrack", 
						entry};
				Config config = new Config(str);
				
				int successNumber = 0;
				int failNumber = 0;
				XmlConverter converter = new XmlConverter();
				for (int i = 0; i < REPEAT_TIME; i++) {
					MonitorListenerAdapter listener = new MonitorListenerAdapter(null, null, fileFilter);
					
					JPF jpf = new JPF(config);
					jpf.addPropertyListener(listener);
					jpf.run();
					if (listener.result.success) {
						successNumber++;
					}
					else {
						failNumber++;
					}
					/*
					System.out.println("---------pairs----------");
					for (ReadWritePair pair : listener.result.pairs) {
						System.out.print("instance: " + pair.instance);
						System.out.print("\tfield: " + pair.field);
						System.out.print("\ttype: " + pair.type);
						System.out.print("\tthread: " + pair.thread);
						System.out.println("\tlocation: " + pair.location);
					}
					
					System.out.println("------------------------");
					*/
					converter.addPairs(listener.result.pairs, listener.result.success ? "success" : "fail");
				}
				
				System.out.println("Success: " + successNumber + " Fail: " + failNumber);
				converter.toFile("ReadWritePairs-" + packageName + ".xml");
				System.out.println("The result has been output to" + "ReadWritePairs-" + packageName + ".xml");
			}
		}
	}
	
	public static class XmlConverter {
		Document doc;
		Element root;
		
		public XmlConverter(){
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				doc = builder.newDocument();
				
				 root = doc.createElement("ReadWritePairs");
				 doc.appendChild(root);
			} catch (ParserConfigurationException e) {
				System.out.println(e.getMessage());
			}
		}
		
		public void addPairs(List<ReadWritePair> pairs, String result) {
			Element pairsElement = doc.createElement("pairs");
			
			for (ReadWritePair pair : pairs) {
				Element pairElement = doc.createElement("pair");
				
				Element instanceElement = doc.createElement("instance");
				instanceElement.appendChild(doc.createTextNode(pair.instance));
				
				Element fieldElement = doc.createElement("field");
				fieldElement.appendChild(doc.createTextNode(pair.field));
				
				Element typeElement = doc.createElement("type");
				typeElement.appendChild(doc.createTextNode(pair.type));
				
				Element threadElement = doc.createElement("thread");
				threadElement.appendChild(doc.createTextNode(pair.thread));
				
				Element locationElement = doc.createElement("location");
				locationElement.appendChild(doc.createTextNode(pair.location));
				
				pairElement.appendChild(instanceElement);
				pairElement.appendChild(fieldElement);
				pairElement.appendChild(typeElement);
				pairElement.appendChild(threadElement);
				pairElement.appendChild(locationElement);
				
				pairsElement.appendChild(pairElement);
			}
			
			Element resultElement = doc.createElement("result");
			resultElement.appendChild(doc.createTextNode(result));
			pairsElement.appendChild(resultElement);
			
			root.appendChild(pairsElement);
		}
		
		public void toFile(String fileName) {
			TransformerFactory tf = TransformerFactory.newInstance();
			try {
				Transformer transformer = tf.newTransformer();
				DOMSource source = new DOMSource(doc);
				transformer.setOutputProperty(OutputKeys.ENCODING, "utf8");
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				PrintWriter pw = new PrintWriter(new FileOutputStream(fileName));
				StreamResult result = new StreamResult(pw);
				transformer.transform(source, result);
				//System.out.println("transform succeeded!");
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
}
