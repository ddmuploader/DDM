package scheduler.init;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SearchScopeInit {
	public static List<String> init() {
		File directory = new File("src/examples");
		File[] files = directory.listFiles();
		List<String> searchScope = new ArrayList<String>();
		for (File file : files) {
//			if (file.isDirectory()) {
				searchScope.addAll(searchFiles(file));
//			}
		}
		return searchScope;
	}
	
	private static List<String> searchFiles(File file) {
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
