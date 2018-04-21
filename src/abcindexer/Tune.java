package abcindexer;

import java.io.*;
import java.util.*;

public class Tune {
   private File file;
   private int index = 0;
   private String title = "";
   private List subtitles = new Vector();
   private String rhythm = "";
   private String meter = "";
   private String key = "";

   public File getFile() {
      return this.file;
   }

   public void setFile(File file) {
      this.file = file;
   }

   public int getIndex() {
      return this.index;
   }

   public void setIndex(int index) {
      this.index = index;
   }

   public String getTitle() {
      return this.title;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public List getSubtitles() {
      return this.subtitles;
   }

   public void addSubtitle(String subtitle) {
      this.subtitles.add(subtitle);
   }

   public String getRhythm() {
      return this.rhythm;
   }

   public void setRhythm(String rhythm) {
      this.rhythm = rhythm;
   }

   public String getMeter() {
      return this.meter;
   }

   public void setMeter(String meter) {
      this.meter = meter;
   }

   public String getKey() {
      return this.key;
   }

   public void setKey(String key) {
      this.key = key;
   }

   public Object clone() {
      Tune t = new Tune();
      t.setFile(getFile());
      t.setIndex(getIndex());
      t.setTitle(getTitle());

      for (Iterator i = getSubtitles().iterator(); i.hasNext(); ) {
         t.addSubtitle((String) i.next());
      }

      t.setRhythm(getRhythm());
      t.setKey(getKey());

      return t;
   }
}