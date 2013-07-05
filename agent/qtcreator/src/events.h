/*
Copyright 2013 Tobias Gierke <tobias.gierke@code-sourcery.de>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

#include <pthread.h>
#include <jvmti.h>
#include <sys/time.h>
#include <unistd.h>

#include "global.h"

#ifndef DATA_H

#define DATA_H

#undef DEBUG_BUFFER

typedef enum EventType {
   EVENT_THREAD_START=0,
   EVENT_THREAD_DEATH=1,
   EVENT_THREAD_SAMPLE=2
} EventType;

struct ThreadStartEvent {
   char threadName[ MAX_THREAD_NAME_LENGTH +1 ]; // +1 because of zero-byte        
};

typedef struct ThreadStartEvent ThreadStartEvent;

struct ThreadDeathEvent {
};

typedef struct ThreadDeathEvent ThreadDeathEvent;

struct ThreadStateChangeEvent {
    jint state;
};

typedef struct ThreadStateChangeEvent ThreadStateChangeEvent;

// TODO: Maybe include record size in bytes so
// TODO: parser can skip unknown record types ?

struct DataRecord {
       int type; // size: 4
       int uniqueThreadId; // size: 4
       struct timespec timestamp; // size: 16
       union {
         ThreadStartEvent startEvent;
         ThreadDeathEvent deathEvent;
         ThreadStateChangeEvent stateChangeEvent;
       };
};

typedef struct DataRecord DataRecord;

typedef int (*WriteRecordCallback)(DataRecord *record,void *data);

#define DATARECORD_BASE_SIZE sizeof(int)+sizeof(int)+sizeof(struct timeval)

#define THREAD_START_EVENT_SIZE DATARECORD_BASE_SIZE+MAX_THREAD_NAME_LENGTH+1
#define THREAD_DEATH_EVENT_SIZE DATARECORD_BASE_SIZE
#define THREAD_STATE_CHANGE_EVENT_SIZE DATARECORD_BASE_SIZE+sizeof(jint)

typedef struct ArrayRingBuffer {
    pthread_mutex_t lock;    
    int readPtr;
    int writePtr;
    int lostSamplesCount;
    int elementsWritten;
    int elementsRead;
    DataRecord *records;    
} RingBuffer;

RingBuffer *createRingBuffer();

void destroyRingBuffer(RingBuffer *buffer);

/*
 * The callback function must return 0 if no data was written.
 * 
 * RETURN: 0 if buffer was full
 */
int writeRecord(RingBuffer *buffer, WriteRecordCallback callback , void* data);

/*
 * RETURN: 0 if buffer was empty
 */
int readRecord(RingBuffer *buffer, void (*callback)(DataRecord*) );
#endif
