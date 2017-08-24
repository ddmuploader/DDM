package scheduler.init;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class EntryInit {
	public static Set<String> init(String path) throws IOException {
		Set<String> mainClasses = new HashSet<String>();
		File entries = new File(path);
		if (entries.exists()) {
			InputStreamReader read = new InputStreamReader(new FileInputStream(entries), "utf-8");
			BufferedReader bufferedReader = new BufferedReader(read);
			String lineText = null;
			while ((lineText = bufferedReader.readLine()) != null) {
				mainClasses.add(lineText);
			}
			read.close();
			
			return mainClasses;
		}
		else {
			System.out.println("Error: Can not find file " + path);
			return null;
		}
	}
}
