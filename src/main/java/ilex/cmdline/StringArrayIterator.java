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
*  Revision 1.3  2004/08/30 14:50:16  mjmaloney
*  Javadocs
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

/* 
This is a utility class, not to be accessed by the application.
It encapsulates an array and an index that points to the current object 
 */ 
public class StringArrayIterator 
{ 
 public StringArrayIterator(String[] aStrings) { 
  m_index = 0; 
  m_strings = aStrings; 
 } 
  
 public boolean EOF() { 
  return m_index >= m_strings.length; 
 } 
  
 public void moveNext() { 
  m_index++; 
 } 
  
 public String get() { 
  return m_strings[m_index]; 
 } 
  
 private String[] m_strings; 
 private int      m_index; 
} 
