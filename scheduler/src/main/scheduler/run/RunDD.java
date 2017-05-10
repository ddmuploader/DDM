package scheduler.run;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

import scheduler.calculation.PatternCalculation;
import scheduler.enumerate.TestResult;
import scheduler.init.PatternInit;
import scheduler.io.FileParser;
import scheduler.listener.DDPListenerAdapter.PathRecorder.SchedulingPoint;
import scheduler.listener.DDPListenerAdapter.Pattern;
import scheduler.listener.DDPListenerAdapter.ReadWritePair;
import scheduler.listener.DDPListenerAdapter.Result;
import scheduler.replay.Replay;

public class RunDD {
	static String entry = "org.apache.commons.pool.impl.TestGenericObjectPool";
	static String desktopPath = "" /* add your output path here */;
	static int MAX_RUNNING_COUNT = 100;
	public static void main(String[] args) throws Exception {
			repeatRun(10);
	}
	
	/*
	 * run DD+ algorithm for cnt times. for each time this method find a pass sequence and a 
	 * fail sequence.then it apply the pattern match algorithm and get all of the patterns
	 * which can be found in fail sequence but doesn't exists in pass sequence.
	 * Finally we use DD+ algorithm to minimize the pattern set.
	 * 
	 */
	public static void repeatRun(int cnt) throws Exception {
		File dir = new File(desktopPath + "\\" + entry);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		
		for (int i = 1; i <= cnt; i++) {
			String output = "";
			String filePath = desktopPath + "\\" + entry + "\\" + i;
			File fileDir = new File(filePath);
			if (!fileDir.exists()) {
				fileDir.mkdirs();
			}
			
			Result fail = null;
			Result pass = null;
			List<Object> f = new ArrayList<Object>();
			int count = 0;
			while ((fail == null || pass == null) && count < MAX_RUNNING_COUNT) {
				Result result = Replay.run(entry);
				if (result.success) {
					pass = result;
				}
				else {
					fail = result;
				}
				count++;
			}
			if (count == MAX_RUNNING_COUNT) {
				output += "can't find a pass or fail sequence\n";
				FileParser.stringToFile(output, filePath + "\\result.txt");
				continue;
			}
			
			f = fail.mixed;
			List<Pattern> patterns = PatternInit.initUnicornPatterns();
			fail.matchPatterns(patterns);
			pass.matchPatterns(patterns);
			
			String patternStr = "";
			FileParser.toXML(fail.mixed, false, filePath + "\\fail_sequence.xml");
			FileParser.toXML(pass.mixed, true, filePath + "\\pass_sequence.xml");
			for (Pattern p : fail.matchedPatterns) {
				patternStr += p;
			}
			FileParser.stringToFile(patternStr, filePath + "\\fail_patterns.txt");
			
			patternStr = "";
			for (Pattern p : pass.matchedPatterns) {
				patternStr += p;
			}
			FileParser.stringToFile(patternStr, filePath + "\\pass_patterns.txt");
			
			List<Pattern> sub = PatternCalculation.sub(fail.matchedPatterns, pass.matchedPatterns);
			
			List<Pattern> R = new ArrayList<Pattern>();

			output += "input patterns:\n";
			output += sub + "\nnumber  " + String.valueOf(sub.size()) + "\n\n";
			
			if (sub.size() == 0) {
				FileParser.stringToFile("pattern set is empty before filtrate", filePath + "\\result.txt");
				continue;
			}
			
			List<Pattern> ddcAlgorithm = DDCAlgorithm(sub, R, 2, f, pass);

			output += "result\n" + ddcAlgorithm + "\n";
			output += "number " +  ddcAlgorithm.size() + "\n";
			FileParser.stringToFile(output, filePath + "\\result.txt");
			System.out.println(output);
		}
	}

	public static List<Pattern> DDCAlgorithm(List<Pattern> U,List<Pattern> R,int N,List<Object> f, Result pass) throws Exception{
		if(U.size() == 1)
			return U;
		
		//divide U into N parts
		int nNum = (U.size() / N);
		
		ObjectPattern[] x = new ObjectPattern[N];
		for (int i = 0; i < x.length; i++) {
			x[i] = new ObjectPattern();
		}
		int index = 0;
		for(int i = 0,round = 0;i < U.size();i++){
			if((i + 1 - round) > nNum && (index + 1) != N){
				index++;
				round = round + nNum;
			}
			x[index].p.add(U.get(i));
		}
		//output whether the division succeeded or not
//		for (ObjectPattern objectPattern : x) {
//			System.out.println(objectPattern.p);
//			System.out.println("+++++++examine division part+++++++++");
//		}
		List<Pattern> U1 = new ArrayList<Pattern>();
		List<Pattern> R1 = new ArrayList<Pattern>();
		for(int i =0;i <= index;i++){
			if(Replay.test(entry, union(x[i].p, R), Complement(U,union(x[i].p, R)), f, pass) == TestResult.SUCCESS){
				System.out.println(111);
				return DDCAlgorithm(x[i].p, R, 2, f, pass);
			}
			
			if((Replay.test(entry, union(x[i].p, R), Complement(U,union(x[i].p, R)), f, pass) == TestResult.FAIL) && (Replay.test(entry, union(Complement(U,x[i].p), R), Complement(U, union(Complement(U,x[i].p), R)), f, pass) == TestResult.FAIL)){
				System.out.println(222);
				return union(DDCAlgorithm(x[i].p, union(Complement(U, x[i].p), R), 2, f, pass), DDCAlgorithm(Complement(Complement(U, x[i].p), R), union(x[i].p, R), 2, f, pass));
			}
			
			if((Replay.test(entry, union(x[i].p, R), Complement(U,union(x[i].p, R)), f, pass) == TestResult.UNKNOWN) && (Replay.test(entry, union(Complement(U,x[i].p), R), Complement(U, union(Complement(U,x[i].p), R)), f, pass) == TestResult.FAIL)){
				System.out.println(333);
				TestResult testResult = Replay.test(entry, union(x[i].p, R), Complement(U,union(x[i].p, R)), f, pass);
				if(testResult.result != null){
					System.out.println(444);
					return DDCAlgorithm(testResult.result.matchedPatterns, R1, 2, f, pass);
				}
				System.out.println(555);
				return DDCAlgorithm(x[i].p, union(Complement(U, x[i].p), R), 2, f, pass);
			}
			
			//10th row of algorithm
			U1.clear();
			for (Pattern pattern : U) {
				U1.add(pattern);
			}
			if(Replay.test(entry, Complement(Complement(U, x[i].p), R), Complement(U, Complement(Complement(U, x[i].p), R)), f, pass) == TestResult.SUCCESS){
				U1 = intersect(U1, Complement(U1, union(x[i].p, R)));
			}
			
			//11th of algorithm
			if(Replay.test(entry, union(x[i].p, R), Complement(U, union(x[i].p, R)), f, pass) == TestResult.FAIL){
				R1 = union(R, x[i].p);
			}
			
			if( N < U.size()){
				return DDCAlgorithm(U1, R1, Math.min(U1.size(), 2 * N), f, pass);
			}
		}
	
		
		return U1;
		
	}

	public static List<Pattern> Complement(List<Pattern> ls, List<Pattern> ls2) {
		List list = new ArrayList(Arrays.asList(new Object[ls.size()])); 
	    Collections.copy(list, ls); 
	    list.removeAll(ls2); 
	    return list; 
	}
	
	public static List union(List ls, List ls2) { 
        List list = new ArrayList(Arrays.asList(new Object[ls.size()])); 
        Collections.copy(list, ls); 
        list.addAll(ls2); 
        return list; 
    }

	 public static List intersect(List ls, List ls2) { 
         List list = new ArrayList(Arrays.asList(new Object[ls.size()])); 
         Collections.copy(list, ls); 
         list.retainAll(ls2); 
         return list; 
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
			}
		}
		
		public static List<Object> fromXML(String filePath) throws Exception {
			List<Object> result = new ArrayList<Object>();
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(filePath);
			
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
	 
	
}
class ObjectPattern{
	List<Pattern> p = new ArrayList<Pattern>();


	public ObjectPattern() {
		super();
	}


	public ObjectPattern(List<Pattern> p) {
		super();
		this.p = p;
	}
	
}
