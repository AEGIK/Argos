//
//  SerializationTests.m
//  Argos
//
//  Created by Christoffer Lernö on 2010-01-06.
//  Copyright 2010 Aegik AB. All rights reserved.
//

#import "SerializationTests.h"
#import "ASSerialize.h"
#import "ASDeserialize.h"

@implementation SerializationTests

- (void)setUp
{
	serializer = [[ASSerialize alloc] init];
	deserializer = [[ASDeserialize alloc] init];
}

- (void)tearDown
{
	[serializer release];
	[deserializer release];
}

- (void)compare:(NSObject *)object bytes:(uint8_t*) bytes length:(int)length
{
	NSData *data = [serializer serialize:object];
	NSData *expectedData = [NSData dataWithBytes:bytes length:length];
	STAssertTrue([data isEqualToData:expectedData], @"Data mismatch was %@ - expected %@ for %@", data, expectedData, object);
}

- (void)compareSerialize:(NSObject *)object
{
	NSData *serialized = [serializer serialize:object];
	NSObject *deserialized = [deserializer deserialize:serialized];
	STAssertTrue([deserialized isEqual:object], @"[%@] Failed to serialize (%@) %@", serialized, object, deserialized);
}

- (void)testSerializeString
{
	[self compareSerialize:@""];
	[self compareSerialize:@"A"];
	NSString *string = @"";
	for (int i = 0; i < 257; i++)
	{
		[self compareSerialize:string];
		string = [string stringByAppendingString:@"a"];
	}
	[self compareSerialize:@"A\u1233"];
}
- (void)testSerializeDouble
{
	[self compareSerialize:[NSNumber numberWithDouble:1.0]];
	[self compareSerialize:[NSNumber numberWithDouble:0.0]];
	[self compareSerialize:[NSNumber numberWithDouble:0x1.fffffffffffffP+1023]];
	[self compareSerialize:[NSNumber numberWithDouble:0x0.0000000000001P-1022]];
	uint8_t b[] = { AS_DOUBLE_ZERO };
	[self compare:[NSNumber numberWithDouble:0.0] bytes:b length:1]; 	
	uint8_t b2[] = { 0x83, 0x3F, 0xF0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
	[self compare:[NSNumber numberWithDouble:1.0] bytes:b2 length:9]; 	
	uint8_t b3[] = { 0x83, 0x7F, 0xEF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF };
	[self compare:[NSNumber numberWithDouble:0x1.fffffffffffffP+1023] bytes:b3 length:9]; 	
	uint8_t b4[] = { 0x83, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 };
	[self compare:[NSNumber numberWithDouble:0x0.0000000000001P-1022] bytes:b4 length:9]; 	
}

- (void)testSerializeBool
{
	[self compareSerialize:[NSNumber numberWithBool:YES]];
	[self compareSerialize:[NSNumber numberWithBool:NO]];
	uint8_t b1[] = { AS_FALSE };
	[self compare:[NSNumber numberWithBool:NO] bytes:b1 length:1]; 	
	uint8_t b2[] = { AS_TRUE };
	[self compare:[NSNumber numberWithBool:YES] bytes:b2 length:1]; 		
}

- (void)testSerializeList
{
	NSMutableArray *array = [NSMutableArray array];
	for (int i = 0; i <= 257; i++)
	{
		[self compareSerialize:array];
		[array addObject:@"a"];
	}
}

- (void)testSerializeDict
{
	NSMutableDictionary *dict = [NSMutableDictionary dictionary];
	for (int i = 0; i <= 257; i++)
	{
		[self compareSerialize:dict];
		[dict setObject:[NSNumber numberWithInt:i * i] forKey:i % 2 == 0 ? [NSString stringWithFormat:@"%d", i] : [NSNumber numberWithInt:i]];
	}
}

- (void)testSerializeData
{
	NSMutableData *data = [NSMutableData data];
	for (int i = 0; i < 257; i++)
	{
		[self compareSerialize:data];
		char x[1];
		x[0] = i;
		[data appendBytes:x length:1];
	}
}

- (void)testSerializeDate
{
	[self compareSerialize:[NSDate dateWithTimeIntervalSince1970:((uint64_t)([[NSDate date] timeIntervalSince1970] * 1000))/1000.0]];
	NSDateComponents *dateComponents = [[NSDateComponents alloc] init];
	[dateComponents setDay:10];
	[dateComponents setHour:4];
	[dateComponents setMinute:0];
	[dateComponents setYear:2000];
	[dateComponents setMonth:2];
	NSCalendar *calendar = [NSCalendar currentCalendar];
	NSDate *date = [calendar dateFromComponents:dateComponents];
	[self compareSerialize:date];
	date = [date addTimeInterval:60];
	[self compareSerialize:date];
	date = [date addTimeInterval:4];
	[self compareSerialize:date];
	date = [date addTimeInterval:4.5];
	[self compareSerialize:date];
}

- (void)testSerializeDict2
{
	NSMutableDictionary *dict = [NSMutableDictionary dictionary];
	uint8_t b[] = { 0xB3, 0xC0, 0x01, 0x61, 0x01, 0xC1, 0x01, 0x63, 0xEF, 0xC2, 0x01, 0x62, 0xA3, 0x0B, 0x16, 0x21 };
	// {"a": 1, "b": [11, 22, 33], "c": null }
	[dict setObject:[NSNumber numberWithInt:1] forKey:@"a"];
	[dict setObject:[NSArray arrayWithObjects:[NSNumber numberWithInt:11], [NSNumber numberWithInt:22], [NSNumber numberWithInt:33], nil] forKey:@"b"];
	[dict setObject:[NSNull null] forKey:@"c"];
	[self compare:dict bytes:b length:16];
	uint8_t b2[] = { 0xB3, 0xC0, 0x01, 0xC1, 0xEF, 0xC2, 0xA3, 0x0B, 0x16, 0x21 };
	[self compare:dict bytes:b2 length:10];
}

- (void)testSerializeInt
{
	uint8_t b[] = { 0x00 };
	[self compare:[NSNumber numberWithInt:0] bytes:b length:1]; 	
	b[0] = AS_NULL;
	[self compare:nil bytes:b length:1]; 	
	[self compare:[NSNull null] bytes:b length:1]; 	
	for (int i = 0; i < 257; i++)
	{
		[self compareSerialize:[NSNumber numberWithInt:i]];
	}
	for (int i = 2; i <= 7; i++)
	{
		[self compareSerialize:[NSNumber numberWithLongLong:((1LL >> (i * 8 - 1)) - 1)]];
		[self compareSerialize:[NSNumber numberWithLongLong:(-(1LL >> (i * 8 - 1)))]];
	}
	[self compareSerialize:[NSNumber numberWithLongLong:0x7FFF]];
	[self compareSerialize:[NSNumber numberWithLongLong:0x3FFF]];
	[self compareSerialize:[NSNumber numberWithLongLong:0x8000]];
	[self compareSerialize:[NSNumber numberWithLongLong:0xA000]];
	[self compareSerialize:[NSNumber numberWithLongLong:0x7FFFFF]];
	[self compareSerialize:[NSNumber numberWithLongLong:0x3FFFFF]];
	[self compareSerialize:[NSNumber numberWithLongLong:0x800000]];
	[self compareSerialize:[NSNumber numberWithLongLong:0xA00000]];
	[self compareSerialize:[NSNumber numberWithLongLong:0x7FFFFFFF]];
	[self compareSerialize:[NSNumber numberWithLongLong:0x3FFFFFFF]];
	[self compareSerialize:[NSNumber numberWithLongLong:0x80000000]];
	[self compareSerialize:[NSNumber numberWithLongLong:0xA0000000]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0x7FFF]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0x3FFF]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0x8000]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0xA000]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0x7FFFFF]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0x3FFFFF]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0x800000]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0xA00000]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0x7FFFFFFF]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0x3FFFFFFF]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0x80000000]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0xA0000000]];
	[self compareSerialize:[NSNumber numberWithLongLong:0x7FFFFFFFFFFFFFFFLL]];
	[self compareSerialize:[NSNumber numberWithLongLong:-0x8000000000000000LL]];
}

@end
