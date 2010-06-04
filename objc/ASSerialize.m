//
//  ASSerialize.m
//  Argos
//
//  Created by Christoffer Lernö on 2010-01-06.
//  Copyright 2010 Aegik AB. All rights reserved.
//

#import "ASSerialize.h"

static int MAX_SYMBOL_LENGTH = 128;

@implementation ASSerialize

- (id)init
{
	if (self = [super init])
	{
		maxSymbols = AS_SYMBOL_ID_2D - AS_SYMBOL_ID_00 + 1 + 256;
		symbolList = [[NSMutableDictionary alloc] initWithCapacity:maxSymbols];
	}
	return self;
}

- (void)dealloc
{
	[symbolList release];
	[currentData release];
	[super dealloc];
}

- (void)addByte:(uint8_t)toAdd
{
	uint8_t b[1];
	b[0] = toAdd;
	[currentData appendBytes:b length:1];	
}

- (void)writeInteger:(int)byteWidth value:(int64_t)value
{
	for (int i = byteWidth - 1; i >= 0; i--)
	{
		[self addByte:(0xFF & (value >> (i * 8)))];
	}		
}

- (void)addInteger:(int64_t)value
{
	static int maxZeroByte = AS_INT_7F - AS_INT_00;
	if (value == -1)
	{
		[self addByte:AS_MINUS_ONE];
		return;
	}
	if (value >= 0 && value <= maxZeroByte)
	{
		[self addByte:AS_INT_00 + value];
		return;
	}
	if (value > maxZeroByte && value < 256)
	{
		[self addByte:AS_ONE_BYTE_INTEGER];
		[self addByte:value - 128];
		return;
	}
	int length = 8;
	for (int i = 1; i <= 7; i++)
	{
		long long max = (1LL << (8 * i - 1)) - 1;
		long long min = -max - 1;
		if (value >= min && value <= max)
		{
			length = i;
			break;
		}
	}
	[self addByte:AS_ONE_BYTE_INTEGER + length - 1];
	[self writeInteger:length value:value];
}


- (void)addDouble:(double)value
{
	if (value == 0.0)
	{
		[self addByte:AS_DOUBLE_ZERO];
	}
	else 
	{
		[self addByte:AS_DOUBLE];
		long long *longValue = (long long *)&value;
		for (int i = 7; i >= 0; i--)
		{
			[self addByte:(0xFF & (*longValue >> (i * 8)))];
		}	
	}
}

- (void)writeSize:(int)size zeroSize:(int)zeroSizeId maxSize:(int)lastFixSizeId
{
	int max = lastFixSizeId - zeroSizeId;
	if (size <= max)
	{
		[self addByte:zeroSizeId + size];
	}
	else if (size < 256)
	{
		[self addByte:lastFixSizeId + 1];
		[self writeInteger:1 value:size];
	}
	else if (size < 65536)
	{
		[self addByte:lastFixSizeId + 2];
		[self writeInteger:2 value:size];
	}
	else
	{
		NSAssert1(false, @"Size out of range: %d", size);
	}
}

- (void)addString:(NSString *)string
{
	int length = [string lengthOfBytesUsingEncoding:NSUTF8StringEncoding];
	[self writeSize:length zeroSize:AS_STRING_LEN_00 maxSize:AS_STRING_LEN_0D];
	[currentData appendBytes:[string UTF8String] length:length];
}

- (void)addSymbolToken:(int)symbolId
{
	int symbolSingleTokenMax = AS_SYMBOL_ID_2D - AS_SYMBOL_ID_00;
	if (symbolId <= symbolSingleTokenMax)
	{
		[self addByte:symbolId + AS_SYMBOL_ID_00];
	}
	else
	{
		[self addByte:AS_SYMBOL_ID_2E_12D];
		[self addByte:symbolId - symbolSingleTokenMax - 1];
	}	
}

- (void)addSymbol:(NSString *)symbol
{
	if ([symbol length] > MAX_SYMBOL_LENGTH) 
	{
		[self addString:symbol];
		return;
	}
	NSNumber *symbolId = [symbolList objectForKey:symbol];
	if (symbolId)
	{
		[self addSymbolToken:[symbolId intValue]];
		return;
	}
	int symbols = [symbolList count]; 
	if (symbols == maxSymbols)
	{
		[self addString:symbol];
		return;
	}
	[self addSymbolToken:symbols];
	[symbolList setObject:[NSNumber numberWithInt:symbols] forKey:symbol];
	int length = [symbol lengthOfBytesUsingEncoding:NSUTF8StringEncoding];
	[self writeInteger:1 value:length];
	[currentData appendBytes:[symbol UTF8String] length:length];
}

- (void)addBool:(BOOL)boolValue
{
	[self addByte:boolValue ? AS_TRUE : AS_FALSE];
}

- (void)addList:(NSArray *)list
{
	[self writeSize:[list count] zeroSize:AS_ARRAY_LEN_00 maxSize:AS_ARRAY_LEN_0D];
	for (id object in list)
	{
		[self add:object];
	}
}

- (void)addData:(NSData *)data
{
	[self writeSize:[data length] zeroSize:AS_BYTE_ARRAY_LEN_00 maxSize:AS_BYTE_ARRAY_LEN_0D];
	[currentData appendData:data];
}

- (void)addDictionary:(NSDictionary *)dictionary
{
	[self writeSize:[dictionary count] zeroSize:AS_MAP_LEN_00 maxSize:AS_MAP_LEN_0D];
	for (id key in [dictionary allKeys])
	{
		if ([key isKindOfClass:[NSString class]])
		{
			[self addSymbol:key];
		}
		else 
		{
			[self add:key];
		}

		[self add:[dictionary objectForKey:key]];
	}
}

- (void)addDate:(NSDate *)date
{
	int64_t time = llround([date timeIntervalSince1970] * 1000);
	if (time % 1000 != 0)
	{
		[self addByte:AS_DATE_MS];
		[self writeInteger:8 value:time];
		return;
	}
	time /= 1000;
	if (time % 3600 != 0)
	{
		[self addByte:AS_DATE_S];
		[self writeInteger:5 value:time];
		return;
	}
	time /= 3600;
	[self addByte:AS_DATE_H];
	[self writeInteger:3 value:time];
}

- (void)add:(id)object
{
	if (object == nil || object == [NSNull null])
	{
		[self addByte:AS_NULL];
	}
	else if ([object isKindOfClass:[NSNumber class]])
	{
		const char *type = [object objCType];
		switch (*type)
		{
			case 'c':
				[self addBool:[object boolValue]];
				break;
			case 'd':
			case 'f':
				[self addDouble:[object doubleValue]];
				break;
			default:
				[self addInteger:[object longLongValue]];
		}
	}
	else if ([object isKindOfClass:[NSString class]])
	{
		[self addString:object];
	}
	else if ([object isKindOfClass:[NSArray class]])
	{
		[self addList:object];
	}
	else if ([object isKindOfClass:[NSDictionary class]])
	{
		[self addDictionary:object];
	}
	else if ([object isKindOfClass:[NSData class]])
	{
		[self addData:object];
	}
	else if ([object isKindOfClass:[NSDate class]])
	{
		[self addDate:object];
	}
	else 
	{
		NSAssert1(false, @"Illegal object for serialization %@", object);
	}
}

- (void)begin
{
	currentData = [[NSMutableData alloc] init];	
}


- (NSData *)serialize
{
	NSData *data = [currentData autorelease];
	currentData = nil;
	return data;
}

- (NSData *)serialize:(NSObject *)object
{
	[self begin];
	[self add:object];
	return [self serialize];
}

@end
