// Copyright (c) 2003 Webware Consulting
package newSim;
import java.io.*;
import java.text.*;
import java.util.*;

public class Logger {
   //private String preFix ="/logs/";
   private String preFix = "clogs\\";
   private String lineSeparator;
   private String fileName;
   private BufferedWriter out;
   private File file;
   public Logger(String fileName){
      lineSeparator = System.getProperty("line.separator");
      this.fileName = preFix +  fileName + ".txt";
      //file = new File(this.fileName);
      //if(!file.exists())
      //   try{
      //      file.createNewFile();
      //   }catch(IOException ix){
      //      System.out.println(ix);
      //   }
      //try{
      //   out = new BufferedWriter(new FileWriter(file));
      //}catch(IOException ix){
      //  System.out.println("*****************" +ix);
      //}
   }
   public  void write(String message){
      //SimpleDateFormat formatter = new SimpleDateFormat();
      //Date date = new Date();
      //String today = formatter.format(date);
      //try{
      //   out.write(message + lineSeparator);
      //}catch (IOException ie){
      //   System.out.println("Not Logged " + message);
      //}
   }
   public void close(){
      //if(out != null)
      //   try{
      //      out.close();
      //   }catch(IOException ix){}
   }
}
 
