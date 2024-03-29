//
//  ASDeserialize.h
//  Argos
//
//  Created by Christoffer Lernö on 2010-01-06.
//  Copyright 2010 Aegik AB. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "ASSerialize.h"

@interface ASDeserialize : NSObject 
{
	uint8_t *currentPointer;
	uint8_t *endPointer;
	NSMutableDictionary *symbolList;
}
- (void)begin:(NSData *)data;
- (uint64_t)readUnsignedInteger:(int)bytes;
- (id)deserialize;
- (id)deserialize:(NSData *)data;

@end
