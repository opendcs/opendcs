package decodes.tsdb.compedit;

import java.util.HashMap;
import java.util.HashSet;

import ilex.util.AsciiUtil;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;


public class PythonStyledDocument extends DefaultStyledDocument
{
	private static HashSet<String> keyWords = new HashSet<String>();
	private static PyFuncList builtinFunctions = new PyFuncList(
		"Python Built-In", "$DCSTOOL_HOME/doc/python-builtin.xml");
	private static PyFuncList cpFunctions = new PyFuncList(
		"CP Functions", "$DCSTOOL_HOME/doc/cp-funcs.xml");
	static
	{
		String pythonKeywords[] = { 
		"False", "None", "True", "and", "as", "assert", "break", 
		"class", "continue", "def", "del", "elif", "else", "except", 
		"finally", "for", "from", "global", "if", "import", "in", "is", 
		"lambda", "nonlocal", "not", "or", "pass", "raise", "return", 
		"try", "while", "with", "yield" };
		for(String kw : pythonKeywords)
			keyWords.add(kw);
		
		builtinFunctions.readFile();
		cpFunctions.readFile();
		
//builtinFunctions.dump();
System.out.println("CP Functions:");
cpFunctions.dump();
	}
	
	
	public static boolean isKeyword(String w) { return keyWords.contains(w); }

	@Override
	public void remove(int offs, int len) throws BadLocationException
	{
System.out.println("remove(offs=" + offs + ", len=" + len + ")");
		super.remove(offs, len);

//		// Get the paraElem that the offset NOW refers to after the delete.
//		Element paraElem = this.getParagraphElement(offs);
//		
//		int lineStart = paraElem.getStartOffset();
//		if (lineStart != paraElem.getEndOffset())
//		{
//			// Retrieve, and then Remove the entire thing from the doc.
//			String line = getText(lineStart, paraElem.getEndOffset() - lineStart);
//			super.remove(lineStart, paraElem.getEndOffset() - lineStart);
//			// Now re-add it with THIS.insertString so the line gets reprocessed.
//			this.insertString(lineStart, line, getStyle(PythonTextType.NormalText.toString()));
//		}
		

	}

	enum CharState { normal, comment, quoted };
	
	@Override
	public void insertString(int offs, String str, AttributeSet a) throws BadLocationException
	{
System.out.println("insertString(" + AsciiUtil.bin2ascii(str.getBytes()) + ")");

		// First write using parent in normal attribute.
		super.insertString(offs, str, getStyle(PythonTextType.NormalText.toString()));
		
		// Back up to the start of the line to determine if we are in a 
		// comment or a quoted string, and to fill in a partial word.
		Element paraElem = this.getParagraphElement(offs);
System.out.println("--- paraElem: " + paraElem.getStartOffset() + " to " + paraElem.getEndOffset());
		CharState charState = CharState.normal;
		char q = 0;
		StringBuilder wordBuf = new StringBuilder();
		boolean escaped = false;
		for(int pos = paraElem.getStartOffset(); pos < offs; pos++)
		{
			char c = this.getText(pos, 1).charAt(0);
			switch(charState)
			{
			case normal:
				if (c == '\'')
				{
					charState = CharState.quoted;
					q = '\'';
				}
				else if (c == '"')
				{
					charState = CharState.quoted;
					q = '"';
				}
				else if (c == '#')
				{
					charState = CharState.comment;
					pos = offs; // comment goes to EOL
				}
				if (Character.isLetterOrDigit(c))
					wordBuf.append(c);
				else
					wordBuf.setLength(0);
				break;
			case quoted:
				if (c == q && !escaped)
					charState = CharState.normal;
				else if (c == '\\' && !escaped)
					escaped = true;
				else if (escaped)
					escaped = false;
				break;
			}
		}
		
		for(int idx = 0; idx < str.length(); idx++)
		{
			char c = str.charAt(idx);
			
			switch(charState)
			{
			case comment:
				if (c == '\n' || c == '\r')
					charState = CharState.normal;
				else // rewrite this char with comment style.
				{
					super.remove(offs+idx, 1);
					super.insertString(offs+idx, ""+c, this.getStyle(PythonTextType.Comment.name()));
				}
				break;
			case normal:
//				if (c == '\n' || c == '\r')
//				{
//					// starting new line
//					wordBuf.setLength(0);
//				}
				if (Character.isLetterOrDigit(c))
					wordBuf.append(c);
				else
				{
					if (wordBuf.length() > 0)
					{
						String word = wordBuf.toString();
System.out.println("End of word seen '" + word + "'");
						wordBuf.setLength(0);
						// Just finished a word. Is it a keyword?
						if (isKeyword(word))
						{
							super.remove(offs+idx-word.length(), word.length());
							super.insertString(offs+idx-word.length(), word.toString(), 
								this.getStyle(PythonTextType.Keywords.toString()));
						}
						else if (c == '(')
						{
							PyFunction func = builtinFunctions.get(word);
							if (func != null)
							{
								super.remove(offs+idx-word.length(), word.length());
								super.insertString(offs+idx-word.length(), word.toString(), 
									this.getStyle(PythonTextType.BuiltIns.toString()));

							}
							else if ((func = cpFunctions.get(word)) != null)
							{
								super.remove(offs+idx-word.length(), word.length());
								super.insertString(offs+idx-word.length(), word.toString(), 
									this.getStyle(PythonTextType.CpFunction.toString()));
							}
							
							//TODO add code for CP-defined functions here.
						}
					}
					if (c == '#')
					{
						charState = CharState.comment;
						super.remove(offs+idx, 1);
						super.insertString(offs+idx, "#", this.getStyle(PythonTextType.Comment.name()));
					}
					else if (c == '"' || c == '\'')
					{
						super.remove(offs+idx, 1);
						super.insertString(offs+idx, ""+c, this.getStyle(PythonTextType.QuotedString.name()));
						charState = CharState.quoted;
						q = c;
					}
				}
				break;
			case quoted:
				super.remove(offs+idx, 1);
				super.insertString(offs+idx, ""+c, this.getStyle(PythonTextType.QuotedString.name()));
				if (c == q && !escaped)
				{
					charState = CharState.normal;
					q = 0;
				}
				else if (c == '\n' || c == '\r') // unterminated string
				{
					charState = CharState.normal;
					q = 0;
					escaped = false;
					//TODO How to issue warning?
				}
				else if (c == '\\' && !escaped)
					escaped = true;
				else if (escaped)
					escaped = false;
				break;
			default:
				break;
			}
		}
		
		// TODO Auto-generated method stub
//		Element charElem = getCharacterElement(offs);
//System.out.println("After insert, char at " + offs + "='" + getText(offs, 1) + "'"
//	+ " Element text is '" + getText(charElem.getStartOffset(), 
//			charElem.getEndOffset()-charElem.getStartOffset()) + "'");

		//TODO Recognize the pattern: word *(
		//     And then attempt to match word to a python built-in or CCP-defined function.

		//TODO Recognize python comments that start with unquoted # and go to end of line.

	}

	public static PyFuncList getBuiltinFunctions()
	{
		return builtinFunctions;
	}

	public static PyFuncList getCpFunctions()
	{
		return cpFunctions;
	}
	

}
