package decodes.db;

import decodes.util.*;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * This Java program generates an HTML file which has tables of the
 * Engineering units supported by DECODES.
 * The HTML file is named 'EUList.html', and is generated in the current
 * working directory.  Any old instance of this file is overwritten without
 * a warning.  This report page has four tables of units.  Each table lists
 * the same engineering units, but each table has them sorted in its own
 * way.  They are sorted by:
 *   - name, 
 *   - abbreviation, 
 *   - unit family, then name, and finally 
 *   - measured quantity, then name.
 *
 * This program uses the editable database's EngineeringUnitList object to
 * generate the tables.  It should be executed from a shell window that
 * has been set up for DECODES.  For example:
 *     . decodes-env
 *     java decodes.db.MakeUnitsTable -P decodes.properties
 */
public class MakeUnitsTable
{
    /**
     * This is the output file.
     */
      PrintWriter _out;

    /**
     * The EngineeringUnitList.
     */
      EngineeringUnit[] _euList;


    /**
     * The main program entry point.
	  @param arg the arguments
     */
      public static void main(String[] arg)
          throws Exception
      {
          MakeUnitsTable mut = new MakeUnitsTable(arg);
          mut.makeTableFile();
      }

    /**
     * Construct with a set of command line arguments.
     * This doesn't cause any output to be generated yet.
	  @param arg the arguments
     */
      public MakeUnitsTable(String[] arg)
          throws Exception
      {
          CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "util.log");
          cmdLineArgs.parseArgs(arg);

          DecodesSettings settings = DecodesSettings.instance();

          // Read the edit database into memory
          Database db = new decodes.db.Database();
          Database.setDb(db);
          DatabaseIO editDbio = 
              DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode,
                                        settings.editDatabaseLocation);
          db.setDbIo(editDbio);
          db.read();

          // Initialize the array of EngineeringUnit objects

          EngineeringUnitList eul = db.engineeringUnitList;
          _euList = new EngineeringUnit[eul.size()];

          int index = 0;
          Iterator i = eul.iterator();
          while (i.hasNext()) 
          {
              _euList[index++] = (EngineeringUnit) i.next();
          }
      }

      class TableDescriptor
      {
          public String abbr;
          public String title;
          public Comparator comp;
          public TableDescriptor(String a, String t, Comparator c) {
              abbr = a;
              title = t;
              comp = c;
          }
      }

      TableDescriptor tableDesc[] = {
          new TableDescriptor(
              "by_name", "Sorted By Name",
              new Comparator() {
                  public int compare(Object obj0, Object obj1) {
                      EngineeringUnit eu0 = (EngineeringUnit) obj0;
                      EngineeringUnit eu1 = (EngineeringUnit) obj1;
                      return eu0.getName().compareToIgnoreCase(eu1.getName());
                  }
              }
          ),
          new TableDescriptor(
              "by_abbr", "Sorted By Abbreviation",
              new Comparator() {
                  public int compare(Object obj0, Object obj1) {
                      EngineeringUnit eu0 = (EngineeringUnit) obj0;
                      EngineeringUnit eu1 = (EngineeringUnit) obj1;
                      return eu0.abbr.compareToIgnoreCase(eu1.abbr);
                  }
              }
          ),
          new TableDescriptor(
              "by_fam", "Sorted By Family, Name",
              new Comparator() {
                  public int compare(Object obj0, Object obj1) {
                      EngineeringUnit eu0 = (EngineeringUnit) obj0;
                      EngineeringUnit eu1 = (EngineeringUnit) obj1;
                      int stat = eu0.family.compareToIgnoreCase(eu1.family);
                      if (stat != 0) return stat;
                      return eu0.getName().compareToIgnoreCase(eu1.getName());
                  }
              }
          ),
          new TableDescriptor(
              "by_meas", "Sorted By Measured Quantity, Name",
              new Comparator() {
                  public int compare(Object obj0, Object obj1) {
                      EngineeringUnit eu0 = (EngineeringUnit) obj0;
                      EngineeringUnit eu1 = (EngineeringUnit) obj1;
                      int stat = eu0.measures.compareToIgnoreCase(eu1.measures);
                      if (stat != 0) return stat;
                      return eu0.getName().compareToIgnoreCase(eu1.getName());
                  }
              }
          ),
      };


    /**
     * Generate the output report file.
     */
      public void makeTableFile()
          throws IOException
      {
          File f = new File("EUList.html");
          _out = new PrintWriter(new FileWriter(f));
          _out.print("<html>\n" +
                     "  <head>\n" +
                     "    <title>DECODES Engineering Units</title>\n" +
                     "  </head>\n" +
                     "  <body>\n" +
                     "    <h1>DECODES Engineering Units</h1>\n\n");

          outContents();
          for (int i = 0; i < tableDesc.length; ++i) {
              outSection(tableDesc[i]);
          }


          _out.print("  </body>\n" +
                    "</html>\n");
          _out.close();
      }

    /**
     * Print out the table of contents.
     */
      public void outContents()
      {
          _out.print("    <h2>Contents</h2>\n" +
                     "    <ul>\n");

          for (int i = 0; i < tableDesc.length; ++i) {
              TableDescriptor td = tableDesc[i];
              _out.print("      <a href='#" + td.abbr + "'>" +
                         td.title + "</a><br>\n");
          }

          _out.print("    </ul>\n\n");
      }

    /**
     * Print out one section.
	  @param td the TableDescriptor
     */
      public void outSection(TableDescriptor td)
      {
          _out.print("    <a name='" + td.abbr + "'></a>\n" +
                     "    <h2>" + td.title + "</h2>\n" +
                     "    <ul>\n");

          java.util.Arrays.sort(_euList, td.comp);

          outTable();

          _out.print("    </ul>\n\n");
      }

	/** output the table. */
      public void outTable()
      {
          _out.print("      <p>\n" +
                     "        <table border='1'>\n" +
                     "          <tr>\n" +
                     "            <th>Name</th>\n" +
                     "            <th>Abbr</th>\n" +
                     "            <th>Family</th>\n" +
                     "            <th>Measures</th>\n" +
                     "          </tr>\n");

          for (int i = 0; i < _euList.length; ++i)
          {
              EngineeringUnit eu = _euList[i];
              _out.print("          <tr>\n");
              _out.print("            <td>" + eu.getName() + "</td>\n" +
                         "            <td>" + eu.abbr + "</td>\n" +
                         "            <td>" + eu.family + "</td>\n" +
                         "            <td>" + eu.measures + "</td>\n" +
                         "          </tr>\n");
          }

          _out.print("        </table>\n" +
                     "      </p>\n\n");
      }
}
