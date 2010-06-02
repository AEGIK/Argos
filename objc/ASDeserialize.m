//
//  ASDeserialize.m
//  Argos
//
//  Created by Christoffer Lernö on 2010-01-06.
//  Copyright 2010 Aegik AB. All rights reserved.
//

#import "ASDeserialize.h"


@implementation ASDeserialize



- (id)init
{
	if (self = [super init])
	{
		int maxSymbols = AS_SYMBOL_ID_2D - AS_SYMBOL_ID_00 + 1 + 256;
		symbolList = [[NSMutableDictionary alloc] initWithCapacity:maxSymbols];
	}
	return self;
}

- (void)dealloc
{
	[symbolList release];
	[super dealloc];
}


- (NSNumber *)readOneByteInteger
{
	uint8_t value = *currentPointer++;
	return [NSNumber numberWithInt:(value >= 128 ? (int8_t)value : value + 128)];
}

- (uint64_t)readUnsignedInteger:(int)bytes
{
	uint64_t value = 0;
	for (int i = 0; i < bytes; i++)
	{
		value = value << 8 | *currentPointer++;
    }
	return value;
}

- (NSNumber *)readInteger:(int)bytes
{
	NSAssert(bytes > 0, @"Zero or less bytes read");
	int64_t value = [self readUnsignedInteger:bytes];
	if (bytes < 8)
	{
		long long max = (1LL << (8 * bytes - 1)) - 1;
		if (value > max)
		{
			value = (-1LL << (bytes * 8)) + value;
		}
	}
	return [NSNumber numberWithLongLong:value];
}

- (NSNumber *)readDouble
{
	uint64_t unsignedLong = [self readUnsignedInteger:8];
	double *d = (double *)&unsignedLong;
	return [NSNumber numberWithDouble:*d];
}

- (NSString *)readString:(uint64_t)length
{
	NSString *string = [[NSString alloc] initWithBytes:currentPointer length:length encoding:NSUTF8StringEncoding];
	currentPointer += length;
	return [string autorelease];
}

- (NSArray *)readList:(uint64_t)length
{
	NSMutableArray *array = [NSMutableArray arrayWithCapacity:length];
	for (int i = 0; i < length; i++)
	{
		[array addObject:[self deserializeInner]];
	}
	return array;
}

- (NSString *)readSymbol:(uint64_t)symbolId
{
	id key = [NSNumber numberWithInt:symbolId];
	NSString *symbol = [symbolList objectForKey:key];
	if (symbol == nil)
	{
		int length = [self readUnsignedInteger:1];
		symbol = [self readString:length];
		[symbolList setObject:symbol forKey:key];
	}
	return symbol;
}

- (NSData *)readData:(uint64_t)length
{
	NSData *data = [NSData dataWithBytes:currentPointer length:length];
	currentPointer += length;
	return data;
}

- (NSDictionary *)readDictionary:(uint64_t)length
{
	NSMutableDictionary *dictionary = [NSMutableDictionary dictionaryWithCapacity:length];
	for (int i = 0; i < length; i++)
	{
		id key = [self deserializeInner];
		id value = [self deserializeInner];
		[dictionary setObject:value forKey:key];
	}
	return dictionary;
}
- (NSObject *)deserializeInner
{
	if (currentPointer == endPointer) return nil;
	uint8_t value = *currentPointer++;
	switch (value) 
	{
		case AS_NULL:
			return [NSNull null];
		case AS_MINUS_ONE:
			return [NSNumber numberWithInt:-1];
		case AS_DOUBLE_ZERO:
			return [NSNumber numberWithDouble:0.0];
		case AS_DOUBLE:
			return [self readDouble];
		case AS_ONE_BYTE_INTEGER:
			return [self readOneByteInteger];
		case AS_STRING_LEN_MAX_255:
			return [self readString:[self readUnsignedInteger:1]];
		case AS_STRING_LEN_MAX_65535:
			return [self readString:[self readUnsignedInteger:2]];
		case AS_FALSE:
			return [NSNumber numberWithBool:NO];
		case AS_TRUE:
			return [NSNumber numberWithBool:YES];
		case AS_ARRAY_MAX_255:
			return [self readList:[self readUnsignedInteger:1]];
		case AS_ARRAY_MAX_65535:
			return [self readList:[self readUnsignedInteger:2]];
		case AS_MAP_MAX_255:
			return [self readDictionary:[self readUnsignedInteger:1]];
		case AS_MAP_MAX_65535:
			return [self readDictionary:[self readUnsignedInteger:2]];
		case AS_SYMBOL_ID_2E_12D:
			return [self readSymbol:[self readUnsignedInteger:1] + AS_SYMBOL_ID_2E_12D - AS_SYMBOL_ID_00];
		case AS_BYTE_ARRAY_MAX_255:
			return [self readData:[self readUnsignedInteger:1]];
		case AS_BYTE_ARRAY_MAX_65535:
			return [self readData:[self readUnsignedInteger:2]];
		case AS_DATE_H:
			return [NSDate dateWithTimeIntervalSince1970:[[self readInteger:3] doubleValue] * 3600.0];
		case AS_DATE_S:
			return [NSDate dateWithTimeIntervalSince1970:[[self readInteger:5] doubleValue]];
		case AS_DATE_MS:
			return [NSDate dateWithTimeIntervalSince1970:[[self readInteger:8] doubleValue] / 1000.0];
	}
	if (value <= AS_INT_7F)
	{
		return [NSNumber numberWithUnsignedChar:value - AS_INT_00];
	}
	if (value >= AS_ONE_BYTE_INTEGER && value <= AS_EIGHT_BYTES_INTEGER)
	{
		return [self readInteger:value - AS_ONE_BYTE_INTEGER + 1];
	}
	if (value >= AS_STRING_LEN_00 && value <= AS_STRING_LEN_0D)
	{
		return [self readString:value - AS_STRING_LEN_00];
	}
	if (value >= AS_ARRAY_LEN_00 && value <= AS_ARRAY_LEN_0D)
	{
		return [self readList:value - AS_ARRAY_LEN_00];
	}
	if (value >= AS_MAP_LEN_00 && value <= AS_MAP_LEN_0D)
	{
		return [self readDictionary:value - AS_MAP_LEN_00];
	}
	if (value >= AS_SYMBOL_ID_00 && value <= AS_SYMBOL_ID_2D)
	{
		return [self readSymbol:value - AS_SYMBOL_ID_00];
	}
	if (value >= AS_BYTE_ARRAY_LEN_00 && value <= AS_BYTE_ARRAY_LEN_0D)
	{
		return [self readData:value - AS_BYTE_ARRAY_LEN_00];
	}
	NSAssert1(false, @"Illegal code %x during deserialization", value);
	return nil;
}

- (NSObject *)deserialize:(NSData *)data
{
	currentPointer = (uint8_t *)[data bytes];
	endPointer = (uint8_t *)([data bytes] + [data length]);
	return [self deserializeInner];
}

@end
