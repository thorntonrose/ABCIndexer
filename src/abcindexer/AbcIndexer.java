package abcindexer;

import java.io.*;
import java.util.*;

/**
 * AbcIndexer generates HTML index files for sets of ABC files.
 */
public class AbcIndexer {
   private String docTitle = "Tunes";
   private String destDir = ".";
   private String baseUrl = null;
   private String srcDir = null;
   private boolean isSplit = false;
   private boolean isVerbose = false;

   // tuneLists - contains one list for each letter
   private TreeMap tuneLists = new TreeMap();

   /**
    * Construct a new AbcIndexer.
    */
   public AbcIndexer() {
   }

   //---------------------------------------------------------------------------

   /**
    * Run with the given arguments.
    */
   public void run(String[] args) throws IOException {
      // Parse arguments.
      if (parseArgs(args)) {
         // Read files.
         readDir(new File(srcDir));

         // Sort tunes.
         for (Iterator j = tuneLists.keySet().iterator(); j.hasNext(); ) {
            Vector tunes = (Vector) tuneLists.get(j.next());
            Collections.sort(tunes, new TuneTitleComparator());
         }

         // Generate docs.
         if (isSplit) {
            generateSplitIndex();
         } else {
            generateIndex();
         }
      } else {
         printUsage();
      }
   }

   /**
    * Parse given arguments. Return true if no errors.
    */
   private boolean parseArgs(String[] args) {
      boolean error = false;
      int i = 0;

      while ((! error) && (i < args.length)) {
         String arg = args[i];

         if (arg.startsWith("-")) {
            // -v (verbose)
            if (arg.equals("-v")) {
               isVerbose = true;

            // -split
            } else if (arg.equals("-split")) {
               isSplit = true;

            // -title
            } else if (arg.equals("-title")) {
               if (++i < args.length) {
                  docTitle = args[i];
               } else {
                  error = true;

               }

            // -destdir
            } else if (arg.equals("-destdir")) {
               if (++i < args.length) {
                  destDir = args[i];
               } else {
                  error = true;
               }

            // -baseurl
            } else if (arg.equals("-baseurl")) {
               if (++i < args.length) {
                  baseUrl = args[i];
               } else {
                  error = true;
               }
            }
         } else {
            srcDir = arg;
         }

         i++;
      }

      if ((i == 0) || (srcDir == null)) {
         error = true;
      }

      return (! error);
   }

   /**
    * Return version (Specification-Version in manifest).
    */
   public String getVersion() {
      return this.getClass().getPackage().getSpecificationVersion();
   }

   /**
    * Print usage information.
    */
   public void printUsage() {
      Package pkg = this.getClass().getPackage();

      System.out.println(pkg.getSpecificationTitle() + " " +
         pkg.getSpecificationVersion());
      System.out.println("Usage: " + pkg.getSpecificationTitle() +
         " [options] dir");
      System.out.println("Options:");
      System.out.println("   -v           = Verbose output.");
      System.out.println("   -split       = Generate one file per letter.");
      System.out.println("   -title title = Document title. (default = \"Tunes\")");
      System.out.println("   -destdir dir = Destination dir. (default = .");
      System.out.println("   -baseurl url = Base URL (relative or absolute).");
   }

   /**
    * Read ABC files from given directory.
    */
   private void readDir(File dir) throws IOException {
      if (isVerbose) {
         System.out.println("Reading directory " + dir + " ...");
      }

      // Get list of files.
      File[] files = dir.listFiles(new FileFilter() {
         public boolean accept(File path) {
            return path.isDirectory() ||
                   path.getName().toLowerCase().endsWith(".abc");
         }
      });

      // Process files.
      for (int i = 0; i < files.length; i ++) {
         File file = files[i];
         String filename = file.getName();

         if (file.isDirectory()) {
            readDir(file);
         } else {
            readFile(file);
         }
      }
   }

   /**
    * Read specified file.
    */
   private void readFile(File file) throws IOException {
      if (isVerbose) {
         System.out.println("Reading file " + file + " ...");
      }

      LineNumberReader reader = new LineNumberReader(new FileReader(file));

      try {
         while (true) {
            String line = reader.readLine();

            if (line == null) {
               break;
            }

            line = line.trim();

            // If NOINDEX, skip file.
            if ((reader.getLineNumber() == 0) && line.equals("%!NOINDEX!")) {
               break;
            }

            // X: - start tune
            if (line.startsWith("X:") && (line.length() > 2)) {
               String ref = getFieldValue(line);

               try {
                  readTune(reader, file, Integer.parseInt(ref));
               } catch(NumberFormatException ex) {
                  System.out.println(file +
                     ": Line " + reader.getLineNumber() +
                     ": Invalid index: X = " + ref);
               }
            }
         }
      } finally {
         reader.close();
      }
   }

   /**
    * Read specified tune.
    */
   private void readTune(BufferedReader reader, File tunefile, int index)
         throws IOException {
      if (isVerbose) {
         System.out.println("Reading tune #" + index + " ...");
      }

      Tune tune = new Tune();
      tune.setFile(tunefile);
      tune.setIndex(index);

      while (true) {
         String line = reader.readLine();

         if (line == null) {
            break;
         }

         line = line.trim();

         // blank line - end tune
         if (line.equals("")) {
            break;
         }

         // T: - title
         if (line.startsWith("T:")) {
            String title = getFieldValue(line);

            if (title.toLowerCase().startsWith("the ") &&
                (title.length() > 4)) {
               title = title.substring(4) + ", " + title.substring(0, 3);
            }

            if (tune.getTitle().equals("")) {
               tune.setTitle(title);
            } else {
               tune.addSubtitle(title);
            }
         }

         // R: - rhythm
         else if (line.startsWith("R:")) {
            tune.setRhythm(getFieldValue(line));
         }

         // M: - meter
         else if (line.startsWith("M:")) {
            tune.setMeter(getFieldValue(line));
         }

         // K: - end of header
         else if (line.startsWith("K:")) {
            tune.setKey(getFieldValue(line));
            break;
         }

      }

      // If tune has key and title, add it to list. Then, for each subtitle,
      // clone tune, set the title, then add clone to list.
      if ((! tune.getKey().equals("")) && (! tune.getTitle().equals(""))) {
         addTune(tune);

         for (Iterator i = tune.getSubtitles().iterator(); i.hasNext(); ) {
            Tune t = (Tune) tune.clone();
            t.setTitle((String) i.next());
            addTune(t);
         }
      }
   }

   /**
    * Add tune to list that corresponds to first letter of tune title.
    */
   private void addTune(Tune tune) {
      if (isVerbose) {
         System.out.println("Adding \"" + tune.getTitle() +
            "\" (#" + tune.getIndex() + ") ...");
      }

      String letter = tune.getTitle().toUpperCase().substring(0, 1);

      if (! tuneLists.containsKey(letter)) {
         tuneLists.put(letter, new Vector());
      }

      Vector tunes = (Vector) tuneLists.get(letter);
      tunes.add(tune);
   }

   /**
    * Get value of specified field.
    */
   private String getFieldValue(String field) {
      String value = "";

      if ((field != null) && (field.length() > 2)) {
         value = field.substring(2).trim();

         int p = value.indexOf('%');

         if (p > -1) {
            value = value.substring(0, p);
         }
      }

      return value;
   }

   /**
    * Generate single index page.
    */
   private void generateIndex() throws IOException {
      if (isVerbose) {
         System.out.println("Generating index ...");
      }

      // Build alpha links.

      String alphaLinks = "";

      for (Iterator i = tuneLists.keySet().iterator(); i.hasNext(); ) {
         String letter = (String) i.next();
         alphaLinks += "<a href=\"#" + letter + "\">" + letter + "</a> ";
      }

      // Generate page.

      String filename = indexFileName(null);

      if (isVerbose) {
         System.out.println("Generating " + filename + " ...");
      }

      PrintWriter out = new PrintWriter(new FileWriter(filename));

      try {
         startPage(out, alphaLinks);
         out.println("<table border=0>");

         for (Iterator i = tuneLists.keySet().iterator(); i.hasNext(); ) {
            String letter = (String) i.next();
            Vector tunes = (Vector) tuneLists.get(letter);

            printTuneTableHeader(out, letter);

            for (Iterator j = tunes.iterator(); j.hasNext(); ) {
               printTuneRow(out, (Tune) j.next());

            }
         }

         out.println("</table>");
         endPage(out);
      } finally {
         out.close();
      }
   }

   /**
    * Generate set of index pages, one for each letter.
    */
   private void generateSplitIndex() throws IOException {
      if (isVerbose) {
         System.out.println("Generating split index ...");
      }

      // Build alpha links.

      String alphaLinks = "";

      for (Iterator i = tuneLists.keySet().iterator(); i.hasNext(); ) {
         String letter = (String) i.next();
         alphaLinks += "<a href=\"" + indexFileName(letter) + "\">" +
            letter + "</a> ";
      }

      // Generate pages.

      String filename = indexFileName(null);

      if (isVerbose) {
         System.out.println("Generating " + filename + " ...");
      }

      PrintWriter out = new PrintWriter(new FileWriter(filename));

      try {
         // Generate first page.

         Vector tunes = (Vector) tuneLists.get(tuneLists.firstKey());
         Tune tune = (Tune) tunes.get(0);
         String letter = tune.getTitle().substring(0, 1).toUpperCase();
         out.println("<meta http-equiv=\"refresh\" content=\"0; URL=" +
            indexFileName(letter) + "\">");
         out.close();

         // Generate index pages.

         for (Iterator i = tuneLists.keySet().iterator(); i.hasNext(); ) {
            letter = (String) i.next();
            tunes = (Vector) tuneLists.get(letter);

            filename = indexFileName(letter);

            if (isVerbose) {
               System.out.println("Generating " + filename + " ...");
            }

            out = new PrintWriter(new FileWriter(filename));

            startPage(out, alphaLinks);
            out.println("<table border=0>");
            printTuneTableHeader(out, letter);

            for (Iterator j = tunes.iterator(); j.hasNext(); ) {
               printTuneRow(out, (Tune) j.next());
            }

            out.println("</table>");
            endPage(out);
            out.close();
         }
      } finally {
         out.close();
      }
   }

   /**
    * Start page.
    */
   private void startPage(PrintWriter out, String alphaLinks) {
      out.println("<html>");
      out.println("<head>");
      out.println("<title>" + docTitle + "</title>");
      out.println("</head>");
      out.println("<body>");
      out.println("<font size=\"+2\"><b>" + docTitle + "</b></font>");
      out.println("<br>");
      out.println(alphaLinks);
      out.println("<hr>");
   }

   /**
    * End page.
    */
   private void endPage(PrintWriter out) {
      out.println("</body>");
      out.println("</html>");
   }

   /**
    * Print tune table header.
    */
   private void printTuneTableHeader(PrintWriter out, String letter) {
      out.println("   <tr><td colspan=5><br>" +
         "<a name=\"" + letter + "\"></a>" +
         "<font size=\"+1\"><b>" + letter + "</b></font></td></tr>");
      out.println("   <tr>");
      out.println("      <td><b>Title</b></td>");
      out.println("      <td><b>Index</b></td>");
      out.println("      <td><b>Key</b></td>");
      out.println("      <td><b>Meter</b></td>");
      out.println("      <td><b>Rhythm</b></td>");
      out.println("      <td><b>File</b></td>");
      out.println("   </tr>");
   }

   /**
    * Print tune row.
    */
   private void printTuneRow(PrintWriter out, Tune tune) {
      File pdf = new File(tune.getFile().getPath() + ".pdf");

      out.println("   <tr>");
      out.println("      <td>" + tune.getTitle() + "</td>");
      out.println("      <td>" + tune.getIndex() + "</td>");
      out.println("      <td>" + tune.getKey() + "</td>");
      out.println("      <td>" + tune.getMeter() + "</td>");
      out.println("      <td>" + tune.getRhythm() + "</td>");
      out.println("      <td>" +
         "<a href=\"" + toUrl(tune.getFile()) + "\">ABC</a>" +
         (pdf.exists() ? " <a href=\"" + toUrl(pdf) + "\">PDF</a>" : "") +
         "</td>");
      out.println("   </tr>");
   }

   /**
    * Convert given path to URL.
    */
   private String toUrl(File path) {
      String url = path.getPath().replace('\\','/');

      if (url.startsWith("./")) {
         url = url.substring(2);
      }

      return (baseUrl == null ? "" : baseUrl + "/") + url;
   }

   /**
    * Returns an index file name.
    */
   private String indexFileName(String letter) {
      return destDir + "/" + docTitle.replace(' ', '-') +
         (letter == null ? "" : "-" + letter) +
         "-Index.html";
   }

   //---------------------------------------------------------------------------

   /**
    * Start application.
    */
   public static void main(String args[]) {
      try {
         (new AbcIndexer()).run(args);
      } catch(Exception ex) {
         System.out.println(ex);
         System.exit(-1);
      }

      System.exit(0);
   }
}