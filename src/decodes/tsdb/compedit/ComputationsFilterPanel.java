/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.6  2013/07/25 18:48:51  mmaloney
 * Don't create site list dialog every time. This is annoying to user because it resets
 * the list to the beginning. There may be hundreds of sites.
 *
 * Revision 1.5  2013/07/24 13:46:43  mmaloney
 * Removed dead code.
 *
 * Revision 1.4  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 */
package decodes.tsdb.compedit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.dbeditor.SiteSelectDialog;
import decodes.sql.DbKey;
import decodes.tsdb.*;
import decodes.gui.*;
import ilex.util.*;

import javax.swing.*;

import opendcs.dai.AlgorithmDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;


public class ComputationsFilterPanel extends JPanel
{
	public JTextField siteText;
	private JButton siteButton;
	public JTextField paramCode;
//	private JButton paramButton;
	private JButton clear;
	public JComboBox intervalBox;
	public JComboBox processBox;
	public JComboBox algorithmBox;
	private JButton refresh;
	private TimeSeriesDb mydb=null;
	public DbKey filterSiteId = Constants.undefinedId;
	boolean isDialog;
	ResourceBundle compLabels;
	private String any;
	private TopFrame parentFrame;
	private SiteSelectDialog siteSelectDialog = null;
	
	/**
	 * constructor taking a new database to filter the computations from. if
	 * the database is null then the instance of CAPEdit's database is used
	 * @param newDb
	 */
	ComputationsFilterPanel(TimeSeriesDb newDb, TopFrame parentFrame)
	{
		this.parentFrame = parentFrame;
		compLabels = CAPEdit.instance().compeditDescriptions;
		any = compLabels.getString("ComputationsFilterPanel.Any");

		this.setBorder(javax.swing.BorderFactory.createTitledBorder(
				javax.swing.BorderFactory.createLineBorder(
						java.awt.Color.gray, 2), compLabels
						.getString("ComputationsFilterPanel.Title"),
				javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
				javax.swing.border.TitledBorder.DEFAULT_POSITION,
				new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
				new java.awt.Color(51, 51, 51)));
		if(newDb==null)
		{
			isDialog=false;
		}
		else
		{
			isDialog = true;
			mydb=newDb;
		}
		siteText = new JTextField();
		siteText.setText(any);
		siteText.setEditable(false);
		paramCode = new JTextField();
		paramCode.setText(any);
		intervalBox=new JComboBox();
		intervalBox.addItem(any);
		clear = new JButton(CAPEdit.instance().genericDescriptions
				.getString("clear"));
		clear.addActionListener(
				new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e) {
						clearButtonPressed();
					}
				});
		processBox=new JComboBox();
		processBox.addItem(any);
		algorithmBox=new JComboBox();
		algorithmBox.addItem(any);
		refresh=new JButton(compLabels
				.getString("ComputationsFilterPanel.RefreshButton"));
		siteButton=new JButton(CAPEdit.instance().genericDescriptions
				.getString("select"));
		siteButton.addActionListener(
				new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e) {
						siteButtonPressed();
					}
				});
		
//		paramButton=new JButton(compLabels
//				.getString("ComputationsFilterPanel.LookupButton"));
//		paramButton.addActionListener(
//				new java.awt.event.ActionListener()
//				{
//					public void actionPerformed(ActionEvent e) {
//						paramButtonPressed();
//					}
//				});
		
		
		this.setLayout(new GridBagLayout());
		
		this.add(new JLabel(compLabels
				.getString("ComputationsFilterPanel.SiteLabel")), new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.EAST,0,new Insets(4,10,0,0),0,0));
		this.add(new JLabel(compLabels
				.getString("ComputationsFilterPanel.CodeLabel")), new GridBagConstraints(0,1,1,1,0,0,GridBagConstraints.EAST,0,new Insets(4,10,0,0),0,0));
		this.add(new JLabel(compLabels
				.getString("ComputationsFilterPanel.IntervalLabel")), new GridBagConstraints(0,2,1,1,0,0,GridBagConstraints.EAST,0,new Insets(4,10,10,0),0,0));
		this.add(siteText, new GridBagConstraints(1,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,4,0,0),0,0));
		this.add(paramCode, new GridBagConstraints(1,1,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,4,0,0),0,0));
		this.add(siteButton, new GridBagConstraints(2,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,10,0,0),0,0));
		this.add(clear, new GridBagConstraints(3,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,10,0,0),0,0));
//		this.add(paramButton, new GridBagConstraints(2,1,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,10,0,0),0,0));
		this.add(intervalBox, new GridBagConstraints(1,2,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,4,10,0),0,0));
		
		this.add(new JLabel(compLabels
				.getString("ComputationsFilterPanel.ProcessLabel")), new GridBagConstraints(5,0,1,1,0,0,GridBagConstraints.EAST,0,new Insets(4,10,0,0),0,0));
		this.add(new JLabel(compLabels
				.getString("ComputationsFilterPanel.AlgorithmLabel")), new GridBagConstraints(5,1,1,1,0,0,GridBagConstraints.EAST,0,new Insets(4,10,0,0),0,0));
		this.add(new JLabel(""), new GridBagConstraints(4,1,1,1,1,0,GridBagConstraints.EAST,0,new Insets(4,10,0,0),0,0));
		this.add(processBox, 
			new GridBagConstraints(6,0,1,1,0,0,
				GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,
				new Insets(4,4,0,10),0,0));
		this.add(algorithmBox, 
			new GridBagConstraints(6,1,1,1,0,0,
				GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,
				new Insets(4,4,0,10),0,0));
		this.add(refresh, new GridBagConstraints(5,2,2,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,4,10,10),0,0));
		TimeSeriesDb theDb;
		if(isDialog)
		{
			theDb=mydb;
		}
		else
		{
			theDb = CAPEdit.instance().theDb;
			if (theDb == null)
				return;
		}

		IntervalDAI intervalDAO = theDb.makeIntervalDAO();
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		try
		{
			for(String obj : intervalDAO.getValidIntervalCodes())
				intervalBox.addItem(obj);
			for(CompAppInfo cai : loadingAppDao.listComputationApps(false))
				processBox.addItem(cai.getAppId() + ": " + cai.getAppName());
			for(String algoName : theDb.listAlgorithms())
				algorithmBox.addItem(algoName);
		}
		catch(Exception ex)
		{
			String msg = compLabels.getString("ComputationsFilterPanel.FillException")
				+ ": " + ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		finally
		{
			loadingAppDao.close();
			intervalDAO.close();
		}
	}

	private void clearButtonPressed()
	{
		siteText.setText(any);
		filterSiteId = Constants.undefinedId;
	}
	

	public JButton getRefresh()
	{
		return refresh;
	}
	
	private void siteButtonPressed()
	{
		if (siteSelectDialog == null)
		{
			siteSelectDialog = new SiteSelectDialog();
			siteSelectDialog.setMultipleSelection(false);
		}
		parentFrame.launchDialog(siteSelectDialog);
		Site site = siteSelectDialog.getSelectedSite();
		if (site != null)
		{
			filterSiteId=site.getId();
			SiteName sn = site.getPreferredName();
			if (sn != null)
				siteText.setText(sn.getNameValue());
		}
	}

	public void setFilterParams(CompFilter compFilter, TimeSeriesDb tsdb)
	{
		compFilter.setSiteId(filterSiteId);
		String x = (String)algorithmBox.getSelectedItem();
		if (!x.equals(any))
		{
			AlgorithmDAI algorithmDAO = tsdb.makeAlgorithmDAO();
			try 
			{
				DbCompAlgorithm algo = algorithmDAO.getAlgorithm(x);
				if (algo != null)
					compFilter.setAlgoId(algo.getId());
			}
			catch(Exception ex)
			{
				Logger.instance().warning("Error seting filter params: " + ex);
			}
			finally
			{
				algorithmDAO.close();
			}
		}
		else
			compFilter.setAlgoId(Constants.undefinedId);

		x = (String)processBox.getSelectedItem();
		if (!x.equals(any))
		{
			int idx = x.indexOf(':');
			if (idx > 0)
			{
				try 
				{
					compFilter.setProcessId(DbKey.createDbKey( 
						Long.parseLong(x.substring(0, idx))));
				}
				catch(NumberFormatException ex) {}
			}
		}
		else
			compFilter.setProcessId(Constants.undefinedId);

		x = paramCode.getText().trim();
		if (x.length() > 0 && !x.equals(any))
		{
			try
			{
				DataType dt = tsdb.lookupDataType(x);
				if (dt != null)
					compFilter.setDataTypeId(dt.getId());
			}
			catch(Exception ex)
			{
				Logger.instance().warning("Error seting filter params: " + ex);
			}
		}

		x = (String)intervalBox.getSelectedItem();
		if (!x.equals(any))
			compFilter.setIntervalCode(x);
		else
			compFilter.setIntervalCode(null);
	}

}
