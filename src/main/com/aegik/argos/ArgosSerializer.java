package com.aegik.argos;

import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Undocumented Class
 *
 * @author Christoffer Lerno
 */
public class ArgosSerializer
{
    private final static int MAX_SYMBOL_LENGTH = 128;
    private final Map<String, Integer> m_symbols;
    private final int m_maxSymbols;
    private final Calendar m_calendar;
    private ByteArrayOutputStream m_stream;

    public ArgosSerializer(int maxSymbols)
    {
        m_calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        m_maxSymbols = maxSymbols;
        m_symbols = new HashMap<String, Integer>(maxSymbols);
    }

    public ArgosSerializer begin()
    {
        m_stream = new ByteArrayOutputStream();
        return this;
    }

    public ArgosSerializer add(Object object)
    {
        try
        {
            if (object == null)
            {
                m_stream.write(ArgosProtocol.NULL);
            }
            else if (object instanceof Boolean)
            {
                m_stream.write(((Boolean) object) ? ArgosProtocol.TRUE : ArgosProtocol.FALSE);
            }
            else if (object instanceof Byte)
            {
                addInt((Byte) object);
            }
            else if (object instanceof Short)
            {
                addInt((Short) object);
            }
            else if (object instanceof Integer)
            {
                addInt((Integer) object);
            }
            else if (object instanceof Long)
            {
                addInt((Long) object);
            }
            else if (object instanceof String)
            {
                addString((String) object);
            }
            else if (object instanceof Collection)
            {
                addCollection((Collection) object);
            }
            else if (object instanceof Map)
            {
                addMap((Map) object);
            }
            else if (object instanceof Double)
            {
                addDouble((Double) object);
            }
            else if (object instanceof Date)
            {
                addDate((Date) object);
            }
            else if (object instanceof byte[])
            {
                addByte((byte[]) object);
            }
            else
            {
                throw new IllegalArgumentException("Unsupported class sent to serializer: " + object.getClass());
            }
            return this;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void addByte(byte[] bytes) throws IOException
    {
        writeSize(bytes.length, ArgosProtocol.BYTE_ARRAY_LEN_ZERO, ArgosProtocol.BYTE_ARRAY_LEN_TEN);
        m_stream.write(bytes);
    }

    private void addDate(Date date)
    {
        m_calendar.setTime(date);
        if (m_calendar.get(Calendar.MILLISECOND) != 0)
        {
            m_stream.write(ArgosProtocol.DATE_MS);
            writeInteger(8, date.getTime());
            return;
        }
        if (m_calendar.get(Calendar.SECOND) != 0 || m_calendar.get(Calendar.MINUTE) != 0)
        {
            m_stream.write(ArgosProtocol.DATE_S);
            writeInteger(5, date.getTime() / 1000);
            return;
        }
        m_stream.write(ArgosProtocol.DATE_H);
        writeInteger(3, date.getTime() / 3600000);
    }

    private void addDouble(Double aDouble)
    {
        if (aDouble == 0.0)
        {
            m_stream.write(ArgosProtocol.DOUBLE_ZERO);
            return;
        }
        m_stream.write(ArgosProtocol.DOUBLE);
        writeInteger(8, Double.doubleToLongBits(aDouble));
    }

    @SuppressWarnings({"unchecked"})
    private void addMap(Map map) throws IOException
    {
        writeSize(map.size(), ArgosProtocol.MAP_LEN_ZERO, ArgosProtocol.MAP_LEN_TEN);
        for (Map.Entry entry : (Set<Map.Entry>) map.entrySet())
        {
            if (entry.getKey() instanceof String)
            {
                addPossibleSymbol((String) entry.getKey());
            }
            else
            {
                add(entry.getKey());
            }
            add(entry.getValue());
        }
    }

    private void addPossibleSymbol(String string) throws IOException
    {
        if (string.length() > MAX_SYMBOL_LENGTH)
        {
            addString(string);
            return;
        }
        Integer symbol = m_symbols.get(string);
        if (symbol == null)
        {
            if (m_symbols.size() == m_maxSymbols)
            {
                addString(string);
                return;
            }
            symbol = m_symbols.size();
            m_symbols.put(string, symbol);
            writeSize(symbol, ArgosProtocol.SYMBOL_ID_00, ArgosProtocol.SYMBOL_ID_2D);
            byte[] bytes = string.getBytes("UTF-8");
            writeInteger(1, bytes.length);
            m_stream.write(bytes);
            return;
        }
        writeSize(symbol, ArgosProtocol.SYMBOL_ID_00, ArgosProtocol.SYMBOL_ID_2D);
    }

    private void addCollection(Collection collection)
    {
        writeSize(collection.size(), ArgosProtocol.ARRAY_LEN_ZERO, ArgosProtocol.ARRAY_LEN_TEN);
        for (Object o : collection)
        {
            add(o);
        }
    }

    private void writeSize(int size, int zeroSizeId, int lastFixSizeId)
    {
        int max = lastFixSizeId - zeroSizeId;
        if (size <= max)
        {
            m_stream.write(zeroSizeId + size);
        }
        else if (size < 256)
        {
            m_stream.write(lastFixSizeId + 1);
            writeInteger(1, size);
        }
        else if (size < 65536)
        {
            m_stream.write(lastFixSizeId + 2);
            writeInteger(2, size);
        }
        else
        {
            throw new IllegalArgumentException("Size out of range: " + size);
        }
    }

    private void addString(String s) throws IOException
    {
        byte[] bytes = s.getBytes("UTF-8");
        writeSize(bytes.length, ArgosProtocol.STRING_LEN_ZERO, ArgosProtocol.STRING_LEN_TEN);
        m_stream.write(bytes);
    }

    public byte[] serialize()
    {
        return m_stream.toByteArray();
    }

    private int byteLength(long integer)
    {
        for (int i = 1; i < 8; i++)
        {
            long min = -(1L << ((i - 1) * 8 + 7));
            long max = -min - 1;
            if (integer >= min && integer <= max) return i;
        }
        return 8;
    }

    private void writeInteger(int byteLength, long integer)
    {
        for (int i = byteLength - 1; i >= 0; i--)
        {
            m_stream.write((int) (0xFF & (integer >>> (i * 8))));
        }
    }

    private void addInt(long integer)
    {
        if (integer == -1)
        {
            m_stream.write(ArgosProtocol.MINUS_ONE);
            return;
        }
        else if (integer >= 0 && integer <= 14)
        {
            m_stream.write(ArgosProtocol.ZERO + (int) integer);
            return;
        }
        int byteLength = byteLength(integer);
        m_stream.write(ArgosProtocol.ONE_BYTE_INTEGER + byteLength - 1);
        writeInteger(byteLength, integer);
    }
}
