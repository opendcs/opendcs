/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2002/08/31 12:16:42  mjmaloney
*  Got rid of assert() statements for JDK 1.4
*
*  Revision 1.2  1999/12/01 12:15:58  mike
*  12/1/1999
*
*  Revision 1.1  1999/09/30 18:16:41  mike
*  9/30/1999
*
*
*/
package ilex.cmdline; 

//import com.ms.wfc.util.Debug.*; 
/* 
 * Java purists will want to comment out a few lines of code in this file 
 */ 

public class util 
{ 
    // If you do not tatrget the Microsoft Win32 VM 
 // comment out the following few lines. Always return 
    // "" from this function. It will just remove the 
    // env. variable functionality 
 // If you know how to get an env. variable in 
 // Pure Java pls e-mail me panos@acm.org 
    static public String GetEnvVariable(String name) { 
//        StringBuffer strBuf = new StringBuffer(200); 
//        int ret = com.ms.win32.Kernel32.GetEnvironmentVariable( 
//               name, strBuf, 200); 
//        if (ret != 0) { 
//            return strBuf.toString(); 
//        } else { 
            return ""; 
//        } 
    } 
  
    // Just comment the line out for pure java environments 
    //static public void assert(boolean cond, String msg) { 
//        com.ms.wfc.util.Debug.assert(cond, msg); 
    //} 
} 
