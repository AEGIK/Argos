package com.aegik.argos;

/**
 * Undocumented Class
 *
 * @author Christoffer Lerno
 */
public interface ArgosProtocol
{
    int NULL = 0x00;
    int TRUE = 0x01;
    int FALSE = 0x02;
    int ZERO = 0x10;
    int FOURTEEN = 0x1E;
    int MINUS_ONE = 0x1F;
    int ONE_BYTE_INTEGER = 0x21;
    int EIGHT_BYTES_INTEGER = 0x28;
    int STRING_LEN_ZERO = 0x30;
    int STRING_LEN_TEN = 0x3A;
    int STRING_LEN_MAX_255 = 0x3B;
    int STRING_LEN_MAX_65535 = 0x3C;

    int ARRAY_LEN_ZERO = 0x40;
    int ARRAY_LEN_TEN = 0x4A;
    int ARRAY_MAX_255 = 0x4B;
    int ARRAY_MAX_65535 = 0x4C;

    int MAP_LEN_ZERO = 0x50;
    int MAP_LEN_TEN = 0x5A;
    int MAP_MAX_255 = 0x5B;
    int MAP_MAX_65535 = 0x5C;

    int SYMBOL_ID_00 = 0x60;
    int SYMBOL_ID_2D = 0x8D;
    int SYMBOL_ID_MAX_255 = 0x8E;
    int SYMBOL_ID_MAX_65535 = 0x8F;

    int DOUBLE_ZERO = 0x90;
    int DOUBLE = 0x91;

    int DATE_MS = 0xA0;
    int DATE_S = 0xA1;
    int DATE_H = 0xA2;

    int BYTE_ARRAY_LEN_ZERO = 0xE0;
    int BYTE_ARRAY_LEN_TEN = 0xEA;
    int BYTE_ARRAY_MAX_255 = 0xEB;
    int BYTE_ARRAY_MAX_65535 = 0xEC;
}
