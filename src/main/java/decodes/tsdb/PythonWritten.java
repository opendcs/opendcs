package decodes.tsdb;

import java.util.Date;

import decodes.sql.DbKey;

/**
 * Bean that stores a value written to the database from a python algorithm.
 * These beans will stay in the compproc queue for 2 minutes. The purpose is
 * to prevent a python algorithm from triggering itself.
 * <br>
 * Example: Simple python algorithm with two inputs A, and B, and one pure
 * output, C:
 * <pre>
 * 		if isNew('A'):		
 * 			setOutput('B', A.value * 2)
 * 		setOutput('C', rating('B', B.value))
 * </pre>
 * So, A is used to compute B. Then B is used to compute C.
 * Without the PythonWritten queue, this is what would happen:
 * <ul>
 *   <li>Computation triggered by new A being written to the database.</li>
 *   <li>Computation computes and writes B</li>
 *   <li>Computation computes and writes C</li>
 *   <li>A new tasklist record for B is created</li>
 *   <li>Computation runs again. It fetches existin A value. It does not recompute
 *       B because the isNew call returns false.</li>
 *   <li>Computation computes and writes C</li>
 * </ul>
 * To fix this, whenever a multi-input python algorithm writes an output, it places
 * a PythonWritten entry in a queue where it resides for 2 minutes.
 * The Resolver will ignore matching tasklist records.
 * @author mmaloney
 *
 */
public class PythonWritten
{
	private DbKey compId = DbKey.NullKey;
	private DbKey tsCode = DbKey.NullKey;
	private Date timeWritten = null;
	
	public PythonWritten(DbKey compId, DbKey tsCode)
	{
		super();
		this.compId = compId;
		this.tsCode = tsCode;
		this.timeWritten = new Date();
	}

	public DbKey getCompId()
	{
		return compId;
	}

	public DbKey getTsCode()
	{
		return tsCode;
	}

	public Date getTimeWritten()
	{
		return timeWritten;
	}
}
