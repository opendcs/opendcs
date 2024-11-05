package decodes.tsdb.compedit;

import java.util.HashMap;
import java.util.HashSet;

import ilex.util.AsciiUtil;
import ilex.util.Logger;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;


public class PythonStyledDocument extends DefaultStyledDocument
{
	private JTextPane pane = null;
	
	private static HashSet<String> keyWords = new HashSet<String>();
	private static PyFuncList builtinFunctions = new PyFuncList(
		"Python Built-In", "$DCSTOOL_HOME/python/python-builtin.xml");
	private static PyFuncList cpFunctions = new PyFuncList(
		"CP Functions", "$DCSTOOL_HOME/python/cp-funcs.xml");
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
//System.out.println("CP Functions:");
//cpFunctions.dump();
	}
	
	public PythonStyledDocument()
	{
		super();
		putProperty("IgnoreCharsetDirective", Boolean.TRUE);
	}
	
	public static boolean isKeyword(String w) { return keyWords.contains(w); }

private void pause() { /* try { Thread.sleep(500L); } catch(InterruptedException ex) {} */}
	
	@Override
	public void remove(int offs, int len) throws BadLocationException
	{
Logger.instance().debug1("remove(offs=" + offs + ", len=" + len + ")"); pause();
		super.remove(offs, len);

		// Get the paraElem that the offset NOW refers to after the delete.
		Element paraElem = this.getParagraphElement(offs);
		
		int lineStart = paraElem.getStartOffset();
		int lineEnd = paraElem.getEndOffset()-1;
//Logger.instance().debug1("After removal, offs=" + offs + " line start=" + lineStart + ", end=" + lineEnd); pause();
		if (lineStart < lineEnd)
		{
			// Retrieve, and then Remove the entire thing from the doc.
			String line = getText(lineStart, lineEnd - lineStart);
			int rmlen = lineEnd - lineStart;
//Logger.instance().debug1("calling super.remove(" + lineStart + ", " + rmlen + ") to remove '" + line + "'"); pause();

			try { super.remove(lineStart, rmlen); }
			catch(BadLocationException ex)
			{
				Logger.instance().warning("PythonStyledDocument.remove thrown in super.remove: " + ex
					+ "offsetRequested=" + ex.offsetRequested());
				return;
			}
			// Now re-add it with THIS.insertString so the line gets reprocessed.
//Logger.instance().debug1("Re-inserting '" + line + "' at position " + lineStart); pause();
			this.insertString(lineStart, line, getStyle(PythonTextType.NormalText.toString()));
		}
		pane.setCaretPosition(offs);
	}

	enum CharState { normal, comment, quoted };
	
	@Override
	public void insertString(int offs, String str, AttributeSet a) throws BadLocationException
	{
//Logger.instance().debug1("insertString(" + AsciiUtil.bin2ascii(str.getBytes()) + ")"); pause();

		// First write using parent in normal attribute.
		super.insertString(offs, str, getStyle(PythonTextType.NormalText.toString()));
		
		// Get the chars from the beginning of the line containing start and the end of the
		// line containing end. Then process the chars and set context colors.
		Element startElem = this.getParagraphElement(offs);
		int startOffset = startElem.getStartOffset();
		Element endElem = this.getParagraphElement(offs + str.length());
		int endOffset = endElem.getEndOffset() -1;
		
		this.processChars(startOffset, endOffset);
		
		pane.setCaretPosition(offs + str.length());



		//TODO Recognize the pattern: word *(
		//     And then attempt to match word to a python built-in or CCP-defined function.

		//TODO Recognize python comments that start with unquoted # and go to end of line.

	}

	private void processChars(int startOffset, int endOffset) 
		throws BadLocationException
	{
Logger.instance().debug1("processChars(" + startOffset + ", " + endOffset + ")");	
		CharState charState = CharState.normal;
		char q = 0;
		StringBuilder wordBuf = new StringBuilder();
		boolean escaped = false;
		
		String str = this.getText(startOffset, endOffset - startOffset);
Logger.instance().debug1("processChars inserting: " + AsciiUtil.bin2ascii(str.getBytes())); pause();

		int offs = startOffset;
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
//Logger.instance().debug1("case comment");
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
//System.out.println("End of word seen '" + word + "'");
						wordBuf.setLength(0);
						// Just finished a word. Is it a keyword?
						if (isKeyword(word))
						{
//Logger.instance().debug1("isKeyword"); pause();
							super.remove(offs+idx-word.length(), word.length());
							super.insertString(offs+idx-word.length(), word.toString(), 
								this.getStyle(PythonTextType.Keywords.toString()));
						}
						else if (c == '(')
						{
							PyFunction func = builtinFunctions.get(word);
							if (func != null)
							{
//Logger.instance().debug1("builtinFunctions"); pause();
								super.remove(offs+idx-word.length(), word.length());
								super.insertString(offs+idx-word.length(), word.toString(), 
									this.getStyle(PythonTextType.BuiltIns.toString()));

							}
							else if ((func = cpFunctions.get(word)) != null)
							{
//Logger.instance().debug1("cpFunctions"); pause();
								super.remove(offs+idx-word.length(), word.length());
								super.insertString(offs+idx-word.length(), word.toString(), 
									this.getStyle(PythonTextType.CpFunction.toString()));
							}
							
							//TODO add code for CP-defined functions here.
						}
					}
					if (c == '#')
					{
//Logger.instance().debug1("#comment");
						charState = CharState.comment;
						super.remove(offs+idx, 1);
						super.insertString(offs+idx, "#", this.getStyle(PythonTextType.Comment.name()));
					}
					else if (c == '"' || c == '\'')
					{
//Logger.instance().debug1("start of quote " + c); pause();
						super.remove(offs+idx, 1);
						super.insertString(offs+idx, ""+c, this.getStyle(PythonTextType.QuotedString.name()));
						charState = CharState.quoted;
						q = c;
					}
				}
				break;
			case quoted:
//Logger.instance().debug1("quoted2");
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
	}

	public static PyFuncList getBuiltinFunctions()
	{
		return builtinFunctions;
	}

	public static PyFuncList getCpFunctions()
	{
		return cpFunctions;
	}

	public void setPane(JTextPane pane)
	{
		this.pane = pane;
	}
	

}
