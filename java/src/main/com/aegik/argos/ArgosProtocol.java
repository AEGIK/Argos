package com.aegik.argos;

/**
 * Specification of the ARGOS object codes.
 *
 * @author Christoffer Lerno
 */
public interface ArgosProtocol
{
    int INT_00 = 0x00;
    int INT_7F = 0x7F;

    int FALSE = 0x80;
    int TRUE = 0x81;
    int DOUBLE_ZERO = 0x82;
    int DOUBLE = 0x83;
    int DATE_MS = 0x84;
    int DATE_S = 0x85;
    int DATE_H = 0x86;
    int MINUS_ONE = 0x87;
    int ONE_BYTE_INTEGER = 0x88;
    int EIGHT_BYTES_INTEGER = 0x8F;

    int STRING_LEN_00 = 0x90;
    int STRING_LEN_0D = 0x9D;
    int STRING_LEN_MAX_255 = 0x9E;
    int STRING_LEN_MAX_65535 = 0x9F;

    int ARRAY_LEN_00 = 0xA0;
    int ARRAY_LEN_0D = 0xAD;
    int ARRAY_MAX_255 = 0xAE;
    int ARRAY_MAX_65535 = 0xAF;

    int MAP_LEN_00 = 0xB0;
    int MAP_LEN_0D = 0xBD;
    int MAP_MAX_255 = 0xBE;
    int MAP_MAX_65535 = 0xBF;

    int SYMBOL_ID_00 = 0xC0;
    int SYMBOL_ID_2D = 0xED;
    int SYMBOL_ID_2E_12D = 0xEE;

    int NULL = 0xEF;

    int BYTE_ARRAY_LEN_00 = 0xF0;
    int BYTE_ARRAY_LEN_0D = 0xFD;
    int BYTE_ARRAY_MAX_255 = 0xFE;
    int BYTE_ARRAY_MAX_65535 = 0xFF;
}
