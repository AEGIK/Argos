package com.aegik.argos;
/**
 * Undocumented Class
 *
 * @author Christoffer Lerno
 */

import junit.framework.*;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.util.*;
import java.text.SimpleDateFormat;

@SuppressWarnings({"unchecked"}) public class ArgosSerializerTest extends TestCase
{
    char[] HEX = "0123456789ABCDEF".toCharArray();
    ArgosSerializer m_argosSerializer;
    ArgosDeserializer m_argosDeserializer;

    public void setUp()
    {
        m_argosSerializer = new ArgosSerializer();
        m_argosDeserializer = new ArgosDeserializer();
    }

    private String toHex(int i)
    {
        return "" + HEX[(i >> 4) & 0x0F] + HEX[i & 0x0F];
    }

    private void printLength(Object o)
    {
        m_argosSerializer.begin().add(o);
        byte[] bytes = m_argosSerializer.serialize();
        System.out.println(o + " -> " + bytes.length + " bytes (" + toHex(bytes) + ")");
    }

    public void testLength()
    {
        Map map = new HashMap();
        map.put("a", 1);
        map.put("b", Arrays.asList(11, 22, 33));
        map.put("c", null);
        printLength(map);
        printLength(map);
        printLength(Arrays.asList(1, 20, 255, 1000, 65536));
        printLength(Arrays.asList(11, 33, 0, 55, -11));
        printLength(1.0);
    }
    private String toHex(byte[] bytes)
    {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes)
        {
            builder.append(HEX[(0xF0 & b) >> 4]).append(HEX[b & 0xF]).append(" ");
        }
        return builder.toString();
    }
    public void compareBytes(String hex)
    {
        StringBuilder builder = new StringBuilder();
        for (byte b : m_argosSerializer.serialize())
        {
            builder.append(HEX[(0xFF & b) >> 4]).append(HEX[b & 0x0F]);
        }
        assertEquals(hex, builder.toString());
    }

    public void testLongString() throws Exception
    {
        m_argosSerializer.begin();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 256; i++)
        {
            builder.append((char)('A' + (int)(Math.random() * 20)));
        }
        m_argosSerializer.add(builder.toString());
        assertEquals(builder.toString(), m_argosDeserializer.deserialize(new ByteArrayInputStream(m_argosSerializer.serialize())));
    }

    public void testAddByte() throws Exception
    {
        m_argosSerializer.begin().add((byte)-128);
        assertEquals(-128L, m_argosDeserializer.deserialize(new ByteArrayInputStream(m_argosSerializer.serialize())));
    }

    public void testAddShort() throws Exception
    {
        m_argosSerializer.begin().add((short)-128);
        assertEquals(-128L, m_argosDeserializer.deserialize(new ByteArrayInputStream(m_argosSerializer.serialize())));
    }

    public void assertSerialization(Object o) throws Exception
    {
        m_argosSerializer.begin().add(o);
        byte[] bytes = m_argosSerializer.serialize();
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        Object result = m_argosDeserializer.deserialize(stream);
        assertEquals(result + ": Stream was: " + toHex(bytes), o, result);
    }

    public void testAddBytes() throws Exception
    {
        for (int i = 0; i < 257; i++)
        {
            byte[] bytes = new byte[i];
            for (int j = 0; j < i; j++)
            {
                bytes[j] = (byte)j;
            }
            m_argosSerializer.begin().add(bytes);
            byte[] result = (byte[]) m_argosDeserializer.deserialize(new ByteArrayInputStream(m_argosSerializer.serialize()));
            assertEquals(Arrays.toString(bytes), Arrays.toString(result));
        }
    }
    public void testAddDate() throws Exception
    {
        assertSerialization(new Date());
        SimpleDateFormat format = new SimpleDateFormat("MMddHHmmss");
        assertSerialization(format.parse("0420123012"));
        assertSerialization(format.parse("0420123000"));
        assertSerialization(format.parse("0420120000"));
    }
    public void testAddMap() throws Exception
    {
        Map map = new HashMap();
        assertSerialization(map);
        map.put(toHex(new byte[1024]), "test");
        for (int i = 0; i < 1024; i++)
        {
            if (i % 2 == 0)
            {
                map.put(i + "", (long)i * i);
            }
            else
            {
                map.put((long)i, (long)- i * i);
            }
            assertSerialization(map);
        }
    }

    public void testExceptions() throws Exception
    {
        try
        {
            m_argosSerializer.begin().add(new Object());
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("Unsupported class sent to serializer: class java.lang.Object", e.getMessage());
        }
        try
        {
            m_argosDeserializer.deserialize(new ByteArrayInputStream(new byte[0]));
            fail();
        }
        catch (EOFException e)
        {}
    }

    public void testAddDouble() throws Exception
    {
        assertSerialization(0.0);
        assertSerialization(1.0);
    }

    public void testAddList() throws Exception
    {
        List<Object> list = new ArrayList<Object>();
        list.add("Test");
        list.add(1L);
        List<Object> list2 = new ArrayList<Object>();
        list2.add(10L);
        list.add(list2);
        assertSerialization(list);
        assertSerialization(new ArrayList());
        ArrayList newList = new ArrayList();
        for (int i = 0; i < 10; i++)
        {
            newList.add((long)i * i);
        }
        assertSerialization(newList);
        newList.add("Test");
        assertSerialization(newList);
        for (int i = 0; i < 250; i++)
        {
            newList.add(-(long) i * i);
            assertSerialization(newList);
        }
    }

    public void testAddInt() throws Exception
    {
        for (int i = 0; i <= 127; i++)
        {
            m_argosSerializer.begin().add(i);
            compareBytes(toHex(i));
            assertEquals((long)i, m_argosDeserializer.deserialize(new ByteArrayInputStream(m_argosSerializer.serialize())));
        }
        m_argosSerializer.begin().add(-1);
        compareBytes("87");
        assertEquals(-1L, m_argosDeserializer.deserialize(new ByteArrayInputStream(m_argosSerializer.serialize())));
        m_argosSerializer.begin().add(255);
        compareBytes("887F");
        m_argosSerializer.begin().add(-128);
        compareBytes("8880");
        m_argosSerializer.begin().add(128);
        compareBytes("8800");
        for (int i = 2; i <= 8; i++)
        {
            long valueMax = (1L << (8 * (i - 1) + 7)) - 1;
            long valueMin = -(1L << (8 * (i - 1) + 7));
            String minHex = "80";
            String maxHex = "7F";
            for (int j = 1; j < i; j++)
            {
                minHex += "00";
                maxHex += "FF";
            }
            m_argosSerializer.begin().add(valueMax);
            compareBytes("8" + HEX[i + 7] + maxHex);
            assertEquals(valueMax, m_argosDeserializer.deserialize(new ByteArrayInputStream(m_argosSerializer.serialize())));
            m_argosSerializer.begin().add(valueMin);
            compareBytes("8" + HEX[i + 7] + minHex);
            assertEquals(valueMin, m_argosDeserializer.deserialize(new ByteArrayInputStream(m_argosSerializer.serialize())));
        }
    }

    public void testAddString() throws Exception
    {
        String test = "";
        String result = "";
        for (int i = 0; i <= 13; i++)
        {
            if (i > 0)
            {
                test += HEX[i];
                result += toHex(HEX[i]);
            }
            m_argosSerializer.begin().add(test);
            compareBytes("9" + HEX[i] + result);
            assertSerialization(test);
        }
        test += HEX[14];
        result += toHex(HEX[14]);
        m_argosSerializer.begin().add(test);
        compareBytes("9E0E" + result);
        assertSerialization(test);
    }

    public void testAddBoolean() throws Exception
    {
        m_argosSerializer.begin().add(true).add(false);
        compareBytes("8180");
        assertEquals(true, m_argosDeserializer.deserialize(new ByteArrayInputStream(m_argosSerializer.serialize())));
        m_argosSerializer.begin().add(false);
        assertEquals(false, m_argosDeserializer.deserialize(new ByteArrayInputStream(m_argosSerializer.serialize())));
    }
    public void testAddNull() throws Exception
    {
        m_argosSerializer.begin().add(null);
        compareBytes("EF");
        m_argosSerializer.begin().add(null).add(null);
        compareBytes("EFEF");
        assertEquals(null, m_argosDeserializer.deserialize(new ByteArrayInputStream(m_argosSerializer.serialize())));

    }
}