package com.aegik.argos;

import java.util.*;
import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;

/**
 * Undocumented Class
 *
 * @author Christoffer Lerno
 */
public class ArgosDeserializer
{
    private final Map<Integer, String> m_symbols;
    private InputStream m_stream;

    public ArgosDeserializer()
    {
        m_symbols = new HashMap<Integer, String>();
    }

    public Object deserialize(InputStream stream) throws IOException
    {
        m_stream = stream;
        return deserialize();
    }

    private Object deserialize() throws IOException
    {
        int prefix = m_stream.read();
        switch (prefix)
        {
            case -1:
                throw new EOFException();
            case ArgosProtocol.NULL:
                return null;
            case ArgosProtocol.TRUE:
                return true;
            case ArgosProtocol.FALSE:
                return false;
            case ArgosProtocol.MINUS_ONE:
                return -1L;
            case ArgosProtocol.STRING_LEN_MAX_255:
                return readString((int)readUnsignedInteger(1));
            case ArgosProtocol.STRING_LEN_MAX_65535:
                return readString((int)readUnsignedInteger(2));
            case ArgosProtocol.ARRAY_MAX_255:
                return readArray((int)readUnsignedInteger(1));
            case ArgosProtocol.ARRAY_MAX_65535:
                return readArray((int)readUnsignedInteger(2));
            case ArgosProtocol.MAP_MAX_255:
                return readMap((int)readUnsignedInteger(1));
            case ArgosProtocol.MAP_MAX_65535:
                return readMap((int)readUnsignedInteger(2));
            case ArgosProtocol.SYMBOL_ID_MAX_255:
                return readSymbol((int)readUnsignedInteger(1));
            case ArgosProtocol.SYMBOL_ID_MAX_65535:
                return readSymbol((int)readUnsignedInteger(2));
            case ArgosProtocol.DOUBLE_ZERO:
                return 0.0d;
            case ArgosProtocol.DOUBLE:
                return readDouble();
            case ArgosProtocol.DATE_MS:
                return new Date(readInteger(8));
            case ArgosProtocol.DATE_S:
                return new Date(readInteger(5) * 1000);
            case ArgosProtocol.DATE_H:
                return new Date(readInteger(3) * 3600000);
            case ArgosProtocol.BYTE_ARRAY_MAX_255:
                return readBytes((int)readUnsignedInteger(1));
            case ArgosProtocol.BYTE_ARRAY_MAX_65535:
                return readBytes((int)readUnsignedInteger(2));
            default:
                return handleRange(prefix);
        }
    }

    private byte[] readBytes(int size) throws IOException
    {
        byte[] bytes = new byte[size];
        if (size != 0 && m_stream.read(bytes) != size) throw new EOFException();
        return bytes;
    }


    private Object handleRange(int prefix) throws IOException
    {
        if (prefix >= ArgosProtocol.ZERO && prefix <= ArgosProtocol.FOURTEEN)
        {
            return (long)prefix - ArgosProtocol.ZERO;
        }
        else if (prefix >= ArgosProtocol.ONE_BYTE_INTEGER && prefix <= ArgosProtocol.EIGHT_BYTES_INTEGER)
        {
            return readInteger(prefix - ArgosProtocol.ONE_BYTE_INTEGER + 1);
        }
        else if (prefix >= ArgosProtocol.STRING_LEN_ZERO && prefix <= ArgosProtocol.STRING_LEN_TEN)
        {
            return readString(prefix - ArgosProtocol.STRING_LEN_ZERO);
        }
        else if (prefix >= ArgosProtocol.ARRAY_LEN_ZERO && prefix <= ArgosProtocol.ARRAY_LEN_TEN)
        {
            return readArray(prefix - ArgosProtocol.ARRAY_LEN_ZERO);
        }
        else if (prefix >= ArgosProtocol.MAP_LEN_ZERO && prefix <= ArgosProtocol.MAP_LEN_TEN)
        {
            return readMap(prefix - ArgosProtocol.MAP_LEN_ZERO);
        }
        else if (prefix >= ArgosProtocol.SYMBOL_ID_00 && prefix <= ArgosProtocol.SYMBOL_ID_2D)
        {
            return readSymbol(prefix - ArgosProtocol.SYMBOL_ID_00);
        }
        else if (prefix >= ArgosProtocol.BYTE_ARRAY_LEN_ZERO && prefix <= ArgosProtocol.BYTE_ARRAY_LEN_TEN)
        {
            return readBytes(prefix - ArgosProtocol.BYTE_ARRAY_LEN_ZERO);
        }
        throw new IOException("Unknown type: " + prefix);
    }

    private String readSymbol(int i) throws IOException
    {
        String symbol = m_symbols.get(i);
        if (symbol == null)
        {
            byte[] utf8Data = new byte[(int)readUnsignedInteger(1)];
            if (utf8Data.length != m_stream.read(utf8Data)) throw new EOFException();
            symbol = new String(utf8Data, "UTF-8");
            m_symbols.put(i, symbol);
        }
        return symbol;
    }

    private Map readMap(int size) throws IOException
    {
        Map<Object, Object> map = new HashMap<Object, Object>();
        for (int i = 0; i < size; i++)
        {
            map.put(deserialize(), deserialize());
        }
        return map;
    }

    @SuppressWarnings({"unchecked"})
    private List readArray(int size) throws IOException
    {
        List<Object> list = new ArrayList<Object>(size);
        for (int i = 0; i < size; i++)
        {
            list.add(deserialize());
        }
        return list;
    }

    private String readString(int length) throws IOException
    {
        if (length == 0) return "";
        byte[] data = new byte[length];
        if (m_stream.read(data) != length) throw new EOFException();
        return new String(data, "UTF-8");
    }

    private Double readDouble() throws IOException
    {
        return Double.longBitsToDouble(readInteger(8));
    }

    private long readUnsignedInteger(int byteLength) throws IOException
    {
        long value = 0;
        for (int i = 0; i < byteLength; i++)
        {
            value = value << 8 | m_stream.read();
        }
        return value ;
    }

    private long readInteger(int byteLength) throws IOException
    {
        long value = readUnsignedInteger(byteLength);
        if (byteLength < 8)
        {
            long max = (1L << (8 * byteLength - 1)) - 1;
            if (value > max)
            {
                value = -(1L << (8 * byteLength)) + value;
            }
        }
        return value;
    }
}
