package org.opendcs.tsdb.interfacess;

import opendcs.dai.TaskListDAI;

/**
 * Indicates that this database can process a computation tasklist.
 */
public interface ProvidesTaskListDao
{
    /**
     * Create a valid instance of a Task List Dao.
     * @return
     */
    public TaskListDAI makeTaskListDao();
}
