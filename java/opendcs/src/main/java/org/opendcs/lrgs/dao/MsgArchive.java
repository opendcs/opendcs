package org.opendcs.lrgs.dao;

import lrgs.archive.SearchHandle;
import lrgs.common.ArchiveUnavailableException;
import lrgs.common.DcpMsg;
import lrgs.common.LrgsStatusProvider;
import lrgs.common.SearchTimeoutException;
import lrgs.lrgsmain.LrgsInputInterface;

public interface MsgArchive
{
    /**
     * Archive a DCP message.
     * @param msg the message.
     * @param src the input device that generated the msg.
     * @return true if message was archived, false if it was discarded.
     */
    void archiveMsg(DcpMsg msg, LrgsInputInterface src);

    /**
     * Search for the next batch of messages.
     * Place retrieved indexes (each containing a message)
     * In the array stored in the passed handle.
     *
     * @return SEARCH_RESULT_DONE, SEARCH_RESULT_MORE, SEARCH_RESULT_PAUSE,
     *  or SEARCH_RESULT_TIMELIMIT
     * @throws ArchiveUnavailableException if can't init search criteria
     * @throws SearchTimeoutException if searchStopMsec reached with no results.
     */
    int search(SearchHandle handle, long stopSearchMsec)
            throws ArchiveUnavailableException, SearchTimeoutException;

    /**
     * Retrieve total number of messages stored
     * @return count of messages
     */
    int getTotalMessageCount();

    /**
     * Unix Epoch time (in seconds) of earliest message stored
     * @return 32 bit unix_t time.
     */
    int getOldestDapsTime();

    /**
     * Status Provider implemntation to keep updated.
     * @param statusProvider implementation of LrgsStatusProvider
     */
    void setStatusProvider(LrgsStatusProvider statusProvider);
}
