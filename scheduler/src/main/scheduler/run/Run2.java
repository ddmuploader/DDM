package scheduler.run;

import java.io.FileOutputStream;
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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import scheduler.listener.FalconImplListener;
import scheduler.listener.MonitorListenerAdapter;
import scheduler.listener.MonitorListenerAdapter.Filter;
import scheduler.listener.MonitorListenerAdapter.ReadWritePair;
import scheduler.listener.MonitorListenerAdapter.Result;
import scheduler.model.ReadWriteNode;
import scheduler.model.SequenceMessage;

public class Run2 {
	public static final int REPEAT_TIME = 100;
	
	public static void main(String[] args) {
		List<String> files = new ArrayList<String>();
		files.add("HashCodeTest.java");
		files.add("IntRange.java");
		files.add("NumberUtils.java");
		files.add("Range.java");
		
		String[] str = new String[]{
				"+classpath=build/examples", 
				"+search.class=scheduler.search.WithoutBacktrack", 
				"hashcodetest.HashCodeTest"};
		//MonitorListenerAdapter listener = new MonitorListenerAdapter();
		Config config = new Config(str);
		//JPF jpf = new JPF(config);
		//jpf.addPropertyListener(listener);
		//jpf.run();
		int successNumber = 0;
		int failNumber = 0;
		XmlConverter converter = new XmlConverter();
		for (int i = 0; i < REPEAT_TIME; i++) {
			MonitorListenerAdapter listener = new MonitorListenerAdapter(null, null, files);
			
			JPF jpf = new JPF(config);
			jpf.addPropertyListener(listener);
			jpf.run();
			if (listener.result.success) {
				successNumber++;
			}
			else {
				failNumber++;
			}
			System.out.println("---------pairs----------");
			for (ReadWritePair pair : listener.result.pairs) {
				System.out.print("instance: " + pair.instance);
				System.out.print("\tfield: " + pair.field);
				System.out.print("\ttype: " + pair.type);
				System.out.print("\tthread: " + pair.thread);
				System.out.println("\tlocation: " + pair.location);
			}
			
			System.out.println("------------------------");
			converter.addPairs(listener.result.pairs, listener.result.success ? "success" : "fail");
		}
		
		System.out.println("Success: " + successNumber + " Fail: " + failNumber);
		converter.toFile("ReadWritePairs.xml");
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
				System.out.println("transform succeeded!");
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
}
