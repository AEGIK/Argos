//
//  SerializationTests.h
//  Argos
//
//  Created by Christoffer Lernö on 2010-01-06.
//  Copyright 2010 Aegik AB. All rights reserved.
//


#import <SenTestingKit/SenTestingKit.h>
#import <UIKit/UIKit.h>
@class ASSerialize;
@class ASDeserialize;
@interface SerializationTests : SenTestCase 
{
	ASSerialize *serializer;
	ASDeserialize *deserializer;
}

- (void) testSerializeInt;


@end
