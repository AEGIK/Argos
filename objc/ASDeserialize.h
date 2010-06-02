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
- (NSObject *)deserializeInner;
- (NSObject *)deserialize:(NSData *)data;

@end
