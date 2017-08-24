import java.io.File;

import edu.illinois.jacontebe.framework.Reporter;

/**
 * Bug URL:https://bugs.openjdk.java.net/browse/JDK-4742723 This is a race.
 * Reproduce environment: JDK 1.6.0 and Windows. This bug is fixed in 1.6.0_02
 * 
 * @collector Ziyi Lin
 *
 */
public class Test4742723 {
	
	static String projectBase = "D:/eclipse src/Jdk62";
	
    public static void main(String[] args) throws Exception {
        Reporter.reportStart("jdk4742723", 0, "race");
//        Reporter.printWarning("1.6.0","1.6.0_02","Windows");
        int i = Math.abs((int) System.currentTimeMillis() % 100);
        if(i % 2 != 0)
        	RunOne();
        else
        	RunTwo();
    }

	private static void RunTwo() throws Exception {
        final String dirA = projectBase + "/base/a";
        final String dirB = projectBase + "/base/b";
        Thread A = new Thread() {
            public void run() {
                File file = new File(dirA);
                file.mkdirs();
                if (file.exists()) {
                    System.out.println("directory A exists");
                }
            }
        };
        Thread B = new Thread() {
            public void run() {
                File file = new File(dirB);
                file.mkdirs();
                if (file.exists()) {
                    System.out.println("directory B exists");
                }
            }
        };
        A.start();
        B.start();
        try {
            A.join();
            B.join();
        } catch (InterruptedException e) {
            // ignore;
        }
        // Check if there are two directories
        File fileA = new File(dirA);
        File fileB = new File(dirB);
        boolean buggy;
        if (!(fileA.exists() && fileB.exists())) {
            buggy = true;
            System.out
                    .println("Two directories are expected to be created, but only one is created.");
            System.out.println("Missing a directory");
            throw new Exception();
        } else {
            buggy = false;
        }
        // clean all the created directories.
        File fileBase = new File(projectBase + "/base");
        fileA.delete();
        fileB.delete();
        fileBase.delete();
        
        Reporter.reportEnd(buggy);
	}

	private static void RunOne() throws Exception {
		 
	        final String dirA = projectBase + "/suc/base/a";
	        System.out.println(projectBase);
	        System.out.println(dirA);
	        File file = new File(dirA);
            file.mkdirs();
            boolean buggy;
            if (file.exists()) {
            	buggy = false;
            } else{
            	buggy = true;
	            throw new Exception();
            }
            Reporter.reportEnd(buggy);
//	        // clean all the created directories.
//	        File fileBase = new File(projectBase + "/base");
//	        file.delete();
//	        fileBase.delete();
	       
	}

    
}
