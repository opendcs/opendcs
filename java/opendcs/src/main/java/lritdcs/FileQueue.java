/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2012/12/12 16:01:31  mmaloney
*  Several updates for 5.2
*
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2003/08/15 20:13:07  mjmaloney
*  dev
*
*  Revision 1.1  2003/08/06 23:29:24  mjmaloney
*  dev
*
*/
package lritdcs;

import java.io.File;
import java.util.LinkedList;

/**
File objects are stored on this synchronized queue.
*/
public class FileQueue
{
	LinkedList<LritDcsFileStats> queue;

	public FileQueue()
	{
		queue = new LinkedList<LritDcsFileStats>();
	}

	public synchronized void enqueue(LritDcsFileStats f)
	{
		queue.addFirst(f);
	}

	public synchronized void enqueue(File f)
	{
		LritDcsFileStats stats = new LritDcsFileStats();
		stats.setFile(f);
		queue.addFirst(stats);
	}

	public synchronized LritDcsFileStats dequeue()
	{
		if (queue.size() == 0)
			return null;
		return queue.removeLast();
	}

	public synchronized void clear()
	{
		queue.clear();
	}

	public int size()
	{
		return queue.size();
	}
}
