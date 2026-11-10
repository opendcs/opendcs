package org.opendcs.data.computations.algorithms;

import decodes.sql.DbKey;
import decodes.tsdb.ScriptType;

/**
 * Intermmediate record to allow construction of the actual DbCompAlgorithmScript object from the
 * individual rows
 * AlgorithmScriptPart
 * @param algorithmId
 * @param type
 * @param blockNumber
 * @param scriptData
 */
public record AlgorithmScriptPart(DbKey algorithmId, ScriptType type, int blockNumber, String scriptData)
{
}
