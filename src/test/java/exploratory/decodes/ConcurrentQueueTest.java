package exploratory.decodes;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

class ConcurrentQueueTest
{
	ConcurrentLinkedQueue<Integer> theQ = new ConcurrentLinkedQueue<Integer>();
	int nextval = 0;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ConcurrentQueueTest cqt = new ConcurrentQueueTest();
		cqt.run();
	}

	public void run()
	{
		System.out.println("Adding 5");
		while(nextval < 5)
			add();
		Iterator<Integer> it = theQ.iterator();
		
		System.out.println("Iterating");
		while(it.hasNext())
			System.out.println("\tIterator 1 returned " + it.next());
		
		System.out.println("Adding 5 more");
		while(nextval < 10)
			add();
		
		System.out.println("Iterating again");
		while(it.hasNext())
			System.out.println("\tIterator 1 returned " + it.next());
		
		System.out.println("Adding 5 more");
		while(nextval < 15)
			add();
		
		System.out.println("Second iterator should return everything up to " + nextval);
		Iterator<Integer> it2 = theQ.iterator();
		while(it2.hasNext())
			System.out.println("\tIterator 2 returned " + it2.next());
		
		System.out.println("Deleting everything from queue");
		Integer I;
		while((I = theQ.poll()) != null)
			System.out.println("\tdeleted " + I);
		
		System.out.println("Adding 5 nore");
		while(nextval < 20)
			add();
		System.out.println("Both iterators should return just the new 5 elements.");
		while(it.hasNext())
			System.out.println("\tIterator 1 returned " + it.next());
		while(it2.hasNext())
			System.out.println("\tIterator 2 returned " + it2.next());
		
		System.out.println("Verify that iterator 1 skipped 10...14");
		

	}
	
	private void add()
	{
		System.out.println("\tAdding " + nextval);
		theQ.add(nextval++);
	}

}
