package com.aegik.argos;

import java.util.Map;
import java.util.List;
import java.util.Date;

/**
 * Factory method to enable custom creation of deserialized objects.
 *
 * @author Christoffer Lerno
 */
public interface ObjectFactory
{
    Map newMap(int size);
    List newList(int size);
    Date newDate(long time);
}
