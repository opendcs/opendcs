/*
*  $Id$
*/
package decodes.tsdb.algoedit;

import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;

import ilex.util.FileUtil;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import decodes.gui.GuiDialog;
import decodes.tsdb.algo.RoleTypes;
import decodes.tsdb.BadTimeSeriesException;
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

	public void doCompile()
	{
		File logFile = new File(algotmpdir, "compile.log");
		PrintWriter logWriter = null;
		int errorCode = 0;
		try
		{
			logWriter = new PrintWriter(logFile);
			ArrayList<String> args = new ArrayList<String>();
			StringTokenizer st = new StringTokenizer(
				compOptionsField.getText().trim());
			while(st.hasMoreTokens())
				args.add(st.nextToken());
			String x = classPathField.getText().trim();
			if (x.length() > 0)
			{
				args.add("-cp");
				args.add(x);
			}
			args.add(tmpFile.getPath());
			args.add("-d");
			args.add(algotmpdir.getPath());
			String arga[] = new String[args.size()];
			try
			{
				errorCode = com.sun.tools.javac.Main.compile(
					args.toArray(arga), new PrintWriter(logFile));
			}
			catch(NoClassDefFoundError ex)
			{
				String javaToolsUrl = findToolsJar();
				if (javaToolsUrl == null)
				{
					resultsArea.append(
						labels.getString("CompileDialog.cannotLoadCompilerErr"));
					String exMsg = ex.toString();
					resultsArea.append(exMsg);
					System.err.println(exMsg);
					ex.printStackTrace();
					return;
				}

				Logger.instance().info("Looking for tools.jar at '"
					+ javaToolsUrl + "'");
				URL urls[] = new URL[1];
				urls[0] = new URL(javaToolsUrl);
				URLClassLoader ucl = new URLClassLoader(urls, 
					ClassLoader.getSystemClassLoader());
				Class cls = ucl.loadClass("com.sun.tools.javac.Main");
				if (cls == null)
				{
					resultsArea.append(
					labels.getString("CompileDialog.cannotFindCompilerErr"));
					return;
				}
				Logger.instance().info("Have class '" + cls.getName() + "'");
				try
				{
					// We have to manually load the class with an URLClassLoader
					// and then invoke the compile method through the reflection
					// mechanism.
					Object javaco = cls.newInstance();
					args.toArray(arga);
					PrintWriter pw = new PrintWriter(logFile);
					
					Method method = cls.getMethod("compile", arga.getClass(), 
						pw.getClass());
					Object errorCodeObj = method.invoke(javaco, arga, pw);
					errorCode = ((Integer)errorCodeObj).intValue();
				}
				catch(Throwable ex2)
				{
					resultsArea.append(
					labels.getString("CompileDialog.cannotLoadCompilerErr"));
					String exMsg = ex2.toString();
					resultsArea.append(exMsg);
					System.err.println(exMsg);
					ex2.printStackTrace();
					return;
				}
			}
			if (errorCode != 0)
			{
				resultsArea.append(
					labels.getString("CompileDialog.javacFailedErr")
					+ errorCode
					+ "\n");
			}
		}
		catch(Exception ex)
		{
			resultsArea.append(
				labels.getString("CompileDialog.failedToCompileErr")
				+ ex.getMessage());
			return;
		}
		finally
		{
			if (logWriter != null)
				try { logWriter.close(); } catch(Exception ex) {}
		}
		
		resultsArea.append(
			labels.getString("CompileDialog.compileLogLabel"));
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new FileReader(logFile));
			String line = null;
			while((line = br.readLine()) != null)
				resultsArea.append(line + "\n");
		}
		catch(Exception ex)
		{
			resultsArea.append(
					labels.getString("CompileDialog.cannotReadLogFileErr")
					+ ex.getMessage());
			return;
		}
		finally
		{
			if (br != null)
				try { br.close(); } catch(Exception ex) {}
		}
		if (errorCode == 0)
		{
			resultsArea.append(
					labels.getString("CompileDialog.compileSuccessfulInfo"));
			resultsArea.append(
					labels.getString("CompileDialog.saveResultingClassInfo"));
			addToJarButton.setEnabled(true);
			saveClassButton.setEnabled(true);
		}
		else
		{
			resultsArea.append(
					labels.getString("CompileDialog.compileFailedErr"));
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

		ArrayList<String> args = new ArrayList<String>();
		if (jarFile.exists())
			args.add("uvf");
		else
			args.add("cvf");
		args.add(jarFile.getPath());
		args.add("-C");
		args.add(algotmpdir.getPath());

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
			classFileName = algoData.getJavaClassName() + ".class";

		Logger.instance().info("Adding '" + classFileName + "' to jar file.");
		args.add(classFileName);

		File logFile = new File(algotmpdir, "jar.log");
		PrintStream logStream = null;
		try
		{
			logStream = new PrintStream(logFile);
			String arga[] = new String[args.size()];
			logStream = new PrintStream(logFile);
			sun.tools.jar.Main jartool = 
				new sun.tools.jar.Main(logStream, logStream, "jar");
			if (!jartool.run(args.toArray(arga)))
				resultsArea.append(
						labels.getString("CompileDialog.jarFailedErr"));
			else
				resultsArea.append(
					labels.getString("CompileDialog.jarSucceededInfo"));
		}
		catch(Exception ex)
		{
			resultsArea.append(
					labels.getString("CompileDialog.jarFailedExErr")
					+ ex.getMessage());
			return;
		}
		finally
		{
			if (logStream != null)
				try { logStream.close(); } catch(Exception ex) {}
		}
		
		resultsArea.append(
				labels.getString("CompileDialog.jarLogInfo"));
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new FileReader(logFile));
			String line = null;
			while((line = br.readLine()) != null)
				resultsArea.append(line + "\n");
		}
		catch(Exception ex)
		{
			resultsArea.append(
				labels.getString("CompileDialog.cannotReadLogFileErr")
				+ ex.getMessage());
			return;
		}
		finally
		{
			if (br != null)
				try { br.close(); } catch(Exception ex) {}
		}
		resultsArea.append(
		labels.getString("CompileDialog.jarSuccessfulInfo"));
	}

	public void doDone()
	{
		setVisible(false);
	}
	
	/**
	 * If "tools.jar" is not in classpath, this method will be called to find
	 * it. It will look for "jdk" directories at java.home and the parent to
	 * java.home.
	 */
	private static String findToolsJar()
	{
		StringBuilder sb = new StringBuilder(System.getProperty("java.home"));
		for(int i=0; i < sb.length(); i++)
			if (sb.charAt(i) == '\\')
				sb.setCharAt(i, '/');
		int idx = sb.lastIndexOf("/");
		if (idx != -1)
			sb.setLength(idx);
		String javaHomePath = sb.toString();

		String testpath = javaHomePath + "/lib/tools.jar";
		File toolsjar = new File(testpath);
		if (toolsjar.exists())
			return path2url(testpath);


		File javaHomeFile = new File(System.getProperty("java.home"));
		File jhParent = javaHomeFile.getParentFile();
       	File listFile[] = jhParent.listFiles();
		if (listFile == null)
			return null;

		// Find the JDK with latest version
		String latestRevName = null;
		File latestToolsJar = null;
		for(int i=0; i < listFile.length; i++)
		{
			File f = listFile[i];
			String jdkDirName = f.getName();
			if(jdkDirName.startsWith("jdk")) 
			{
				File lib = new File(f, "lib");
				toolsjar = new File(lib, "tools.jar");

		   		if(toolsjar.exists())
				{
					if (latestRevName == null 
					 || latestRevName.compareTo(jdkDirName) < 0)
					{
						latestRevName = jdkDirName;
						latestToolsJar = toolsjar;
					}
				}
		  	}
		}

		if (latestToolsJar == null)
			return null;
		return path2url(latestToolsJar.getPath());
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

//	public static void main(String args[])
//	{
//		System.out.println(findToolsJar());
//	}
}
