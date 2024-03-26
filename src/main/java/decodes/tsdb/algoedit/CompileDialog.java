/*
*  $Id$
*/
package decodes.tsdb.algoedit;

import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.Diagnostic.Kind;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

import ilex.util.FileUtil;
import ilex.util.LoadResourceBundle;
import decodes.gui.GuiDialog;
import decodes.util.DecodesSettings;

import ilex.util.EnvExpander;

public class CompileDialog extends GuiDialog
{
	private ResourceBundle labels = null;
	private ResourceBundle genericLabels = null;
	
	private JPanel northPanel = new JPanel();
	private JPanel centerPanel = new JPanel();
	private JPanel buttonPanel = new JPanel();

	private JLabel compOptionsLabel;
	static private JTextField compOptionsField = new JTextField();
	private JLabel classPathLabel;
	static private JTextField classPathField = new JTextField();
	static
	{
		compOptionsField.setText(
			DecodesSettings.instance().algoEditCompileOptions);
		classPathField.setText(System.getProperty("java.class.path"));
	}

	private JPanel northButtonPanel = new JPanel();
	private JButton clearButton;
	private JButton writeJavaButton;
	private JButton compileButton;

	private JScrollPane javaCodePane = new JScrollPane();
	private JTextArea javaCodeArea = new JTextArea();
	private JScrollPane resultsPane = new JScrollPane();
	private JTextArea resultsArea = new JTextArea();

	private JButton addToJarButton;
	private JButton saveClassButton;
	private JButton doneButton;
	private static JFileChooser fileChooser = new JFileChooser();

	private AlgoData algoData;
	private AlgoWriter algoWriter;
	private File algotmpdir = null;
	private File pkgdir = null;
	private File tmpFile = null;

	public CompileDialog(AlgoData algoData, AlgoWriter algoWriter)
	{
		super(AlgorithmWizard.instance().getFrame(), "", false);

		this.algoData = algoData;
		this.algoWriter = algoWriter;
		
		labels = AlgorithmWizard.getLabels();
		genericLabels = AlgorithmWizard.getGenericLabels();

		this.setTitle(labels.getString("CompileDialog.title"));
		compOptionsLabel = 
			new JLabel(labels.getString("CompileDialog.compilerOptions"));
		classPathLabel = 
			new JLabel(labels.getString("CompileDialog.classPath"));
		clearButton = new JButton(labels.getString("CompileDialog.clear"));
		writeJavaButton = 
			new JButton(labels.getString("CompileDialog.writeJavaCode"));
		compileButton = new JButton(labels.getString("CompileDialog.compile"));
		addToJarButton = 
			new JButton(labels.getString("CompileDialog.addToJarFile"));
		saveClassButton = 
			new JButton(labels.getString("CompileDialog.saveClassFile"));
		doneButton = new JButton(labels.getString("CompileDialog.done"));
		
		guiInit();
		pack();
		getRootPane().setDefaultButton(doneButton);
	}
	
	public void clear()
	{
		javaCodeArea.setText("");
		resultsArea.setText("");
		compileButton.setEnabled(false);
		addToJarButton.setEnabled(false);
		saveClassButton.setEnabled(false);
	}

	private void fill()
	{
	}

	private void guiInit()
	{
		getContentPane().setLayout(new BorderLayout());
		getContentPane().setPreferredSize(new Dimension(500, 600));
		getContentPane().add(northPanel, BorderLayout.NORTH);
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		northPanel.setLayout(new GridBagLayout());
		northPanel.add(compOptionsLabel,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(15, 10, 5, 2), 0, 0));
		northPanel.add(compOptionsField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(15, 0, 5, 10), 0, 0));
		northPanel.add(classPathLabel,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 2), 0, 0));
		northPanel.add(classPathField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 10), 0, 0));

		northButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
		northPanel.add(northButtonPanel,
			new GridBagConstraints(0, 2, 2, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(8, 20, 8, 20), 0, 0));
		Dimension buttonDim = writeJavaButton.getSize();
		buttonDim.setSize(120, (int)buttonDim.getHeight());
		//writeJavaButton.setPreferredSize(buttonDim);
		northButtonPanel.add(clearButton);
		northButtonPanel.add(writeJavaButton);
		//compileButton.setPreferredSize(buttonDim);
		northButtonPanel.add(compileButton);

		clearButton.addActionListener(
			new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) { clear(); }
			});
		clearButton.setEnabled(true);
		writeJavaButton.addActionListener(
			new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) { doWriteJava(); }
			});
		writeJavaButton.setEnabled(true);
		compileButton.addActionListener(
			new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) { doCompile(); }
			});
		compileButton.setEnabled(false);

		centerPanel.setLayout(new GridBagLayout());

		javaCodePane.setBorder(
			BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.gray, 2),
				labels.getString("CompileDialog.javaCode")));
		javaCodePane.setViewportView(javaCodeArea);
		javaCodeArea.setTabSize(4);
		Font oldfont = javaCodeArea.getFont();
		Font newfont = new Font("Monospaced",Font.PLAIN,oldfont.getSize());
		javaCodeArea.setFont(newfont);
		javaCodeArea.setBorder(
			BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		centerPanel.add(javaCodePane,
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.6,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(8, 5, 8, 5), 0, 0));

		resultsPane.setBorder(
			BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.gray, 2), 
				labels.getString("CompileDialog.compileResults")));
		resultsPane.setViewportView(resultsArea);
		resultsArea.setBorder(
			BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		centerPanel.add(resultsPane,
			new GridBagConstraints(0, 1, 1, 1, 1.0, 0.6,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(8, 5, 8, 5), 0, 0));

		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
		addToJarButton.setEnabled(false);
		//addToJarButton.setPreferredSize(buttonDim);
		buttonPanel.add(addToJarButton);
		saveClassButton.setEnabled(false);
		//saveClassButton.setPreferredSize(buttonDim);
		buttonPanel.add(saveClassButton);
		//doneButton.setPreferredSize(buttonDim);
		buttonPanel.add(doneButton);
		addToJarButton.addActionListener(
			new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) { doAddToJar(); }
			});
		saveClassButton.addActionListener(
			new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) { doSaveClass(); }
			});
		doneButton.addActionListener(
			new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) { doDone(); }
			});
	}

	private void doWriteJava()
	{
		algotmpdir = new File(
			EnvExpander.expand("$DCSTOOL_USERDIR/tmp/algo"));
		if (!algotmpdir.isDirectory())
			algotmpdir.mkdirs();
		StringBuilder pkgnm = new StringBuilder(algoData.getJavaPackage());
		for(int i = 0; i < pkgnm.length(); i++)
			if (pkgnm.charAt(i) == '.')
				pkgnm.setCharAt(i, '/');
		if (pkgnm.length() > 0)
		{
			pkgdir = new File(algotmpdir, pkgnm.toString());
			if (!pkgdir.isDirectory())
				pkgdir.mkdirs();
		}
		else
			pkgdir = algotmpdir;
		tmpFile = new File(pkgdir, algoData.getJavaClassName() + ".java");
		javaCodeArea.setText("");
		resultsArea.setText(
				LoadResourceBundle.sprintf(
				labels.getString("CompileDialog.saveToTempFileMsg"),
				tmpFile.getPath()) + "\n");
		try { algoWriter.saveToTheFile(tmpFile, algoData); }
		catch(AlgoIOException ex)
		{
			resultsArea.append(
				labels.getString("CompileDialog.cannotWriteJavaFileErr") + 
				ex.getMessage() + '\n');
			return;
		}
		LineNumberReader lnr = null;
		try
		{
			lnr = new LineNumberReader(new FileReader(tmpFile));
			String line = null;
			while((line = lnr.readLine()) != null)
				javaCodeArea.append("" + lnr.getLineNumber() + ": " + line + 
					"\n");
		}
		catch(IOException ex)
		{
			resultsArea.append(
				labels.getString("CompileDialog.cannotReadJavaFileErr")
				+ ex.getMessage() + '\n');
			return;
		}
		finally
		{
			if (lnr != null)
				try { lnr.close(); } catch(Exception ex) {}
		}
		resultsArea.setText(labels.getString("CompileDialog.savedJavaFileInfo"));
		resultsArea.append(
				labels.getString("CompileDialog.pressingCompileInfo"));
		compileButton.setEnabled(true);
	}

	/**
	 *
	 */
	public void doCompile()
	{
		File logFile = new File(algotmpdir, "compile.log");
		saveClassButton.setEnabled(false);
		addToJarButton.setEnabled(false);
		try (PrintWriter logWriter = new PrintWriter(logFile))
		{
			final List<String> args = new ArrayList<String>();
			StringTokenizer st = new StringTokenizer(
				compOptionsField.getText().trim());
			while(st.hasMoreTokens())
			{
				args.add(st.nextToken());
			}
			String x = classPathField.getText().trim();
			if (x.length() > 0)
			{
				args.add("-classpath");
				args.add(x);
			}
			args.add("-source"); args.add("1.8");
			args.add("-target"); args.add("1.8");
			//args.add(tmpFile.getPath());
			args.add("-d");
			args.add(algotmpdir.getPath());
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			if (compiler == null)
			{
				resultsArea.append(
					labels.getString("CompileDialog.cannotLoadCompilerErr"));
				return;
			}
			StandardJavaFileManager fm = compiler.getStandardFileManager(null, getLocale(), Charset.forName("UTF-8"));
			Iterable<? extends JavaFileObject> javaFileObjects = fm.getJavaFileObjects(tmpFile);
			final DiagnosticListener<javax.tools.JavaFileObject> listener = new DiagnosticListener<javax.tools.JavaFileObject>()
			{
				@Override
				public void report(Diagnostic<? extends JavaFileObject> diagnostic)
				{
					String msg = diagnostic.getMessage(getLocale());
					long line = diagnostic.getLineNumber();
					long column = diagnostic.getColumnNumber();
					Kind kind = diagnostic.getKind();
					JavaFileObject jfo = diagnostic.getSource();
					String fileName = "textarea";
					if (jfo != null)
					{
						fileName = diagnostic.getSource().toUri().toString();
					}
					String userMsg = (jfo == null )
								   ? String.format("%s %s%s", kind.toString(), msg, System.lineSeparator())
								   : String.format("%s %s:%d%d: %s%s", kind.toString(), fileName, line, column, msg, System.lineSeparator());
					resultsArea.append(userMsg);
				}
			};
			JavaCompiler.CompilationTask task = compiler.getTask(logWriter, fm, listener, args, null, javaFileObjects);
			if (!task.call())
			{
				resultsArea.append("Compilation failed.");
			}
			else
			{
				resultsArea.append("Compilation succeeded.");
				saveClassButton.setEnabled(true);
				addToJarButton.setEnabled(true);
			}
		}
		catch(Exception ex)
		{
			resultsArea.append(
				labels.getString("CompileDialog.failedToCompileErr")
				+ ex.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			resultsArea.append(sw.toString());
			return;
		}
	}

	public void doSaveClass()
	{
		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			File file = fileChooser.getSelectedFile();
			if (file != null)
			{
				String classFileName = tmpFile.getPath();
				int idx = classFileName.lastIndexOf(".java");
				if (idx > 0)
					classFileName = classFileName.substring(0, idx);
				classFileName = classFileName + ".class";
				try { FileUtil.copyFile(new File(classFileName), file); }
				catch(IOException ex)
				{
					showError(
							LoadResourceBundle.sprintf(
							labels.getString("CompileDialog.cannotSaveErr"),
							file.getPath()) + ex);
				}
			}
		}
	}
	
	public void doAddToJar()
	{
		File jarFile = null;
		if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION
		 || (jarFile = fileChooser.getSelectedFile()) == null)
		{
			resultsArea.append(
					labels.getString("CompileDialog.noJarSelectedInfo"));
			return;
		}

		List<JarEntryWithData> existingEntries = new ArrayList<>();
		Manifest manifest = null;
		if (jarFile.exists())
		{
			try (JarFile jf = new JarFile(jarFile))
			{
				manifest = jf.getManifest();
				Enumeration<JarEntry> entries = jf.entries();
				while(entries.hasMoreElements())
				{
					JarEntry entry = entries.nextElement();
					if (!entry.getName().equals("META-INF/MANIFEST.MF"))
					{
						InputStream is = jf.getInputStream(entry);
						byte[] data = new byte[(int)entry.getSize()];
						is.read(data, 0, (int)entry.getSize());
						existingEntries.add(new JarEntryWithData(entry, data));
					}
				}
			}
			catch (IOException ex)
			{
				resultsArea.append("Unable to Read in existing Jar File contents: " + ex.getLocalizedMessage());
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				ex.printStackTrace(pw);
				resultsArea.append(sw.toString());
				return;
			}
		}
		if (manifest == null)
		{
			manifest = new Manifest();
			Attributes attr = manifest.getMainAttributes();
			attr.put(Attributes.Name.MANIFEST_VERSION, "1.0");
			String createdBy = String.format("AlgoEdit with %s",ToolProvider.getSystemJavaCompiler().getClass().getName());
			attr.putValue("Created-By", createdBy);
		}

		String classFileName;
		if (algotmpdir != pkgdir)
		{
			StringBuilder sb = new StringBuilder(
				algoData.getJavaPackage() + "." + algoData.getJavaClassName());
			for(int i=0; i<sb.length(); i++)
				if (sb.charAt(i) == '.')
					sb.setCharAt(i, '/');
			sb.append(".class");
			classFileName = sb.toString();
		}
		else
		{
			classFileName = algoData.getJavaClassName() + ".class";
		}
		
		try
		{
			File tmpJarFile = File.createTempFile(jarFile.getName(), ".jar");
			tmpJarFile.deleteOnExit();
			try(
				OutputStream os = new FileOutputStream(tmpJarFile);
				JarOutputStream jos = new JarOutputStream(os, manifest);)
			{
				for (JarEntryWithData entry: existingEntries)
				{
					if (!entry.entry.getName().equals(classFileName))
					{
						jos.putNextEntry(entry.entry);
						jos.write(entry.data); // write bytes here.
						jos.closeEntry();
					}
				}
				File classFile = new File(algotmpdir, classFileName);
				JarEntry algoEntry = new JarEntry(classFileName);
				jos.putNextEntry(algoEntry);
				byte data[] = FileUtil.getfileBytes(classFile);
				jos.write(data);
				jos.closeEntry();
			}
			FileUtil.copyFile(tmpJarFile, jarFile);
		}
		catch (IOException ex)
		{
			resultsArea.append("Unable to add class file to jar:" + ex.getLocalizedMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			resultsArea.append(sw.toString());
		}
	}

	public void doDone()
	{
		setVisible(false);
	}

	public static String path2url(String path)
	{
		StringBuilder sb = new StringBuilder(path);
		for(int i=0; i < sb.length(); i++)
		{
			if (sb.charAt(i) == '\\')
				sb.setCharAt(i, '/');
			else if (sb.charAt(i) == ' ')
			{
				sb.setCharAt(i, '%');
				sb.insert(++i, "20");
			}
		}
		if (sb.charAt(0) != '/')
			sb.insert(0, "file:///");
		else 
			sb.insert(0, "file://");
		return sb.toString();
	}

	private static class JarEntryWithData
	{
		public final JarEntry entry;
		public final byte[] data;

		public JarEntryWithData(JarEntry entry, byte[] data)
		{
			this.entry = entry;
			this.data = data;
		}
	}
}
