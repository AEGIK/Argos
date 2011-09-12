package com.aegik.argos;

import java.util.*;

/**
 * Default object factory returns hash maps as object representations and array lists as list representations.
 * Dates are constructed using default Date.
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
