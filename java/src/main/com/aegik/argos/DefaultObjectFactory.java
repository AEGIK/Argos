package com.aegik.argos;

import java.util.*;

/**
 * Undocumented Class
 *
 * @author Christoffer Lerno
 */
public class DefaultObjectFactory implements ObjectFactory
{
    public static ObjectFactory DEFAULT = new DefaultObjectFactory();

    protected DefaultObjectFactory()
    {
    }

    public Date newDate(long time)
    {
        return new Date(time);
    }

    public Map newMap(int size)
    {
        return new HashMap(size);
    }

    public List newList(int size)
    {
        return new ArrayList(size);
    }
}
