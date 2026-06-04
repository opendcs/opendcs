package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;


import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.sql.DbKey;
import opendcs.opentsdb.Interval;

/**
 * DAO to retrieve Intervals or Durations valid within this database.
 * 
 * Intervals and Durations, while related, are not the same concept.
 * 
 * An Interval is the time between two samples.
 * The duration is a window over which data was aggregated.
 * 
 * Thus this interace is called IntervalDurationDao, as it is rare the usage
 * would ever need one but not the other.
 * 
 * Unfortunately, some implementations do no distinguish between the two.
 * This interface does, if you're system does not, share logic appropriately.
 */
public interface IntervalDurationDao extends OpenDcsDao
{
    
    Optional<Interval> findIntervalByName(DataTransaction tx, String name) throws OpenDcsDataException;

    Optional<Interval> findIntervalById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    Optional<Interval> findDurationByName(DataTransaction tx, String name) throws OpenDcsDataException;

    Optional<Interval> findDurationById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Save an interval to this database
     * @param tx
     * @param interval
     * @return
     * @throws OpenDcsDataException
     */
    Interval saveInterval(DataTransaction tx, Interval interval) throws OpenDcsDataException;

    /**
     * Save a duration to this database
     * @param tx
     * @param interval
     * @return
     * @throws OpenDcsDataException
     */
    Interval saveDuration(DataTransaction tx, Interval interval) throws OpenDcsDataException;


    void deleteInterval(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    void deleteDuration(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    List<Interval> getAllIntervals(DataTransaction tx) throws OpenDcsDataException;

    List<Interval> getAllDurations(DataTransaction tx) throws OpenDcsDataException;

}
