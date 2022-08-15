package opendcs.opentsdb.hydrojson.dao;

import java.util.ArrayList;

import decodes.sql.DbKey;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompAlgorithmScript;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.ScriptType;
import decodes.tsdb.compedit.AlgorithmInList;
import opendcs.dai.AlgorithmDAI;
import opendcs.opentsdb.hydrojson.ErrorCodes;
import opendcs.opentsdb.hydrojson.beans.AlgorithmRef;
import opendcs.opentsdb.hydrojson.beans.DecodesAlgorithm;
import opendcs.opentsdb.hydrojson.beans.DecodesAlgorithmScript;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;

/**
 * Thin wrapper around DECODES Algorithm DAO to translate into JSON-ready objects.
 */
public class AlgorithmDaoWrapper
{
	AlgorithmDAI dao = null;
	
	public AlgorithmDaoWrapper(AlgorithmDAI dao)
	{
		this.dao = dao;
	}

	public ArrayList<AlgorithmRef> getAlgorithmRefs()
		throws DbIoException
	{
		ArrayList<AlgorithmInList> ails = dao.listAlgorithmsForGui();
		ArrayList<AlgorithmRef> ret = new ArrayList<AlgorithmRef>();
		for(AlgorithmInList ail : ails)
			ret.add(new AlgorithmRef(ail));
		return ret;
	}

	public DecodesAlgorithm getAlgorithm(Long algoId)
		throws DbIoException, WebAppException
	{
		try
		{
			DbCompAlgorithm dca = dao.getAlgorithmById(DbKey.createDbKey(algoId));
			return new DecodesAlgorithm(dca);
		}
		catch(NoSuchObjectException ex)
		{
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
				"No Algorithm with id=" + algoId);

		}
	}
	
	public DecodesAlgorithm writeAlgorithm(DecodesAlgorithm algo)
		throws DbIoException, WebAppException
	{
		// Convert to decodes.tsdb object
		DbCompAlgorithm dca = new DbCompAlgorithm(
			algo.getAlgorithmId() == null ? DbKey.NullKey : DbKey.createDbKey(algo.getAlgorithmId()),
			algo.getName(), algo.getExecClass(), algo.getDescription());
		for(String pname : algo.getProps().stringPropertyNames())
			dca.setProperty(pname, algo.getProps().getProperty(pname));
		for(DecodesAlgorithmScript script : algo.getAlgoScripts())
		{
			DbCompAlgorithmScript dcas = new DbCompAlgorithmScript(dca, 
				ScriptType.fromDbChar(script.getScriptType()));
			dcas.addToText(script.getText());
			dca.putScript(dcas);
		}
		for(DbAlgoParm dap : algo.getParms())
			dca.addParm(dap);
		
		dao.writeAlgorithm(dca);
		
		// If this was a new algo, the ID will have been set.
		algo.setAlgorithmId(dca.getId().getValue());
		return algo;
	}

	
}
