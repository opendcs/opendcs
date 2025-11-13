package org.opendcs.util;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.SubmissionPublisher;

/**
 * A circular buffer.
 * Additionally the Buffer extends SubmissionPublisher to allow subscription
 * based notification instead of polling.
 *
 * @see java.util.concurrent.SubmissionPublisher
 */
public class RingBuffer<T> extends SubmissionPublisher<T> implements List<T>
{
    // Performance hit, but easier to deal with initially
    private final LinkedList<T> list = new LinkedList<>();
    private int maxSize;

    public RingBuffer(int size)
    {
        this.maxSize = size;
    }


    /**
     * Set the size of this Buffer, NOTE: will grow but not shrink the backing array
     * @param size
     */
    public void setSize(int size)
    {
        this.maxSize = size;
    }

    @Override
    public boolean add(T element)
    {
        while (list.size() >= maxSize)
        {
            list.removeFirst();
        }
        boolean added = list.add(element);
        if (added)
        {
            submit(element);
        }
        return added;
    }

    @Override
    public boolean addAll(Collection<? extends T> elements)
    {
        boolean ret = true;
        for (T element: elements)
        {
            if (!add(element))
            {
                ret = false;
                break;
            }
        }
        return ret;
    }

    /**
     * Return the number of active elements
     * @return
     */
    public int size()
    {
        return list.size();
    }

    public T get(int index)
    {
        return list.get(index);
    }

    @Override
    public Iterator<T> iterator()
    {
        return list.iterator();
    }


    @Override
    public boolean isEmpty()
    {
        return list.isEmpty();
    }


    @Override
    public boolean contains(Object o)
    {
        return list.contains(o);
    }


    @Override
    public Object[] toArray()
    {
        return list.toArray();
    }


    @Override
    public <T> T[] toArray(T[] a)
    {
        return list.toArray(a);
    }


    @Override
    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException("Only the buffer is allowed to remove elements.");
    }


    @Override
    public boolean containsAll(Collection<?> c)
    {
        return list.containsAll(c);
    }


    @Override
    public boolean addAll(int index, Collection<? extends T> c)
    {
        throw new UnsupportedOperationException("Adding from index is not supported");
    }


    @Override
    public boolean removeAll(Collection<?> c)
    {
        throw new UnsupportedOperationException("Only the buffer is allowed to remove elements.");
    }


    @Override
    public boolean retainAll(Collection<?> c)
    {
        throw new UnsupportedOperationException("Unimplemented method 'retainAll'");
    }


    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Only the buffer is allowed to remove elements.");
    }


    @Override
    public T set(int index, T element)
    {
        throw new UnsupportedOperationException("setting element at index is not supported.");
    }


    @Override
    public void add(int index, T element)
    {
        throw new UnsupportedOperationException("adding element at index is not supported.");
    }


    @Override
    public T remove(int index)
    {
        throw new UnsupportedOperationException("Only the buffer is allowed to remove elements.");
    }


    @Override
    public int indexOf(Object o)
    {
        return list.indexOf(o);
    }


    @Override
    public int lastIndexOf(Object o)
    {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator()
    {   
        return list.listIterator();
    }


    @Override
    public ListIterator<T> listIterator(int index)
    {
        return list.listIterator(index);
    }


    @Override
    public List<T> subList(int fromIndex, int toIndex)
    {
        return list.subList(fromIndex, toIndex);
    }
}
