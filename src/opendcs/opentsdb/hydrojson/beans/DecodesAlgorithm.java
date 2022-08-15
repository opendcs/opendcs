package opendcs.opentsdb.hydrojson.beans;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompAlgorithmScript;

public class DecodesAlgorithm
{
	/** Surrogate key for this algorithm in the time series database.  */
	private Long algorithmId = null;

	/** Name of this algorithm */
	private String name = null;

	/** Fully qualified Java class name to execut this algorithm. */
	private String execClass = null;

	/** Free form multi-line comment */
	private String description = null;

	/** Properties associated with this algorithm. */
	private Properties props = new Properties();
	
	/** parameters to this algorithm */
	private ArrayList<DbAlgoParm> parms = new ArrayList<DbAlgoParm>();

	/** For use in the editor -- the number of computations using this algo. */
	private int numCompsUsing = 0;
	
	private ArrayList<DecodesAlgorithmScript> algoScripts = 
		new ArrayList<DecodesAlgorithmScript>();
	
	
	public DecodesAlgorithm()
	{
	}
	
	public DecodesAlgorithm(DbCompAlgorithm dca)
	{
		this.setAlgorithmId(dca.getId().getValue());
		this.setName(dca.getName());
		this.setExecClass(dca.getExecClass());
		this.setDescription(dca.getComment());
		this.setProps(dca.getProperties());
		this.setNumCompsUsing(dca.getNumCompsUsing());
		for(DbCompAlgorithmScript dcas : dca.getScripts())
		{
			DecodesAlgorithmScript as = new DecodesAlgorithmScript();
			as.setScriptType(dcas.getScriptType().getDbChar());
			as.setText(dcas.getText());
			algoScripts.add(as);
		}
		for(Iterator<DbAlgoParm> dit = dca.getParms(); dit.hasNext(); )
			parms.add(dit.next());
	}

	public Long getAlgorithmId()
	{
		return algorithmId;
	}

	public void setAlgorithmId(Long algorithmId)
	{
		this.algorithmId = algorithmId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getExecClass()
	{
		return execClass;
	}

	public void setExecClass(String execClass)
	{
		this.execClass = execClass;
	}

	public Properties getProps()
	{
		return props;
	}

	public void setProps(Properties props)
	{
		this.props = props;
	}

	public ArrayList<DbAlgoParm> getParms()
	{
		return parms;
	}

	public void setParms(ArrayList<DbAlgoParm> parms)
	{
		this.parms = parms;
	}

	public int getNumCompsUsing()
	{
		return numCompsUsing;
	}

	public void setNumCompsUsing(int numCompsUsing)
	{
		this.numCompsUsing = numCompsUsing;
	}

	public ArrayList<DecodesAlgorithmScript> getAlgoScripts()
	{
		return algoScripts;
	}

	public void setAlgoScripts(ArrayList<DecodesAlgorithmScript> algoScripts)
	{
		this.algoScripts = algoScripts;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
	
	
}
