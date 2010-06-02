//
//  ASSerialize.h
//  Argos
//
//  Created by Christoffer Lernö on 2010-01-06.
//  Copyright 2010 Aegik AB. All rights reserved.
//

#import <Foundation/Foundation.h>

typedef enum _ASProtocol
{
	AS_INT_00 = 0x00,
    AS_INT_7F = 0x7F,
	AS_FALSE = 0x80,
    AS_TRUE = 0x81,
    AS_DOUBLE_ZERO = 0x82,
	AS_DOUBLE = 0x83,
    AS_DATE_MS = 0x84,
    AS_DATE_S = 0x85,
    AS_DATE_H = 0x86,
    AS_MINUS_ONE = 0x87,
    AS_ONE_BYTE_INTEGER = 0x88,
    AS_EIGHT_BYTES_INTEGER = 0x8F,
    AS_STRING_LEN_00 = 0x90,
    AS_STRING_LEN_0D = 0x9D,
    AS_STRING_LEN_MAX_255 = 0x9E,
    AS_STRING_LEN_MAX_65535 = 0x9F,
    AS_ARRAY_LEN_00 = 0xA0,
    AS_ARRAY_LEN_0D = 0xAD,
    AS_ARRAY_MAX_255 = 0xAE,
    AS_ARRAY_MAX_65535 = 0xAF,
    AS_MAP_LEN_00 = 0xB0,
    AS_MAP_LEN_0D = 0xBD,
    AS_MAP_MAX_255 = 0xBE,
    AS_MAP_MAX_65535 = 0xBF,
    AS_SYMBOL_ID_00 = 0xC0,
    AS_SYMBOL_ID_2D = 0xED,
    AS_SYMBOL_ID_2E_12D = 0xEE,
    AS_NULL = 0xEF,
    AS_BYTE_ARRAY_LEN_00 = 0xF0,
    AS_BYTE_ARRAY_LEN_0D = 0xFD,
    AS_BYTE_ARRAY_MAX_255 = 0xFE,
    AS_BYTE_ARRAY_MAX_65535 = 0xFF,
} ASProtocol;

@interface ASSerialize : NSObject 
{
	NSMutableDictionary *symbolList;
	int maxSymbols;
	NSMutableData *currentData;
}

- (void)serializeInner:(id)object;
- (NSData *)serialize:(NSObject *)object;

@end
