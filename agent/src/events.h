#include "global.h"
#include <pthread.h>
#include <jvmti.h>
#include <sys/time.h>

#ifndef DATA_H

#define DATA_H

#undef DEBUG_BUFFER

typedef enum EventType {
   EVENT_THREAD_START=0,
   EVENT_THREAD_DEATH=1,
   EVENT_THREAD_SAMPLE=2
} EventType;

typedef struct ThreadStartEvent {
   char threadName[ MAX_THREAD_NAME_LENGTH +1 ]; // +1 because of zero-byte        
} ThreadStartEvent;

typedef struct ThreadDeathEvent {
} ThreadDeathEvent;

typedef struct ThreadStateChangeEvent {
    jint state;
} ThreadStateChangeEvent;

// TODO: Maybe include record size in bytes so
// TODO: parser can skip unknown record types ?

struct __attribute__ ((__packed__)) DataRecord {
       int type; // size: 4
       int uniqueThreadId; // size: 4
       struct timeval timestamp; // size: 16
       union {
         ThreadStartEvent startEvent;
         ThreadDeathEvent deathEvent;
         ThreadStateChangeEvent stateChangeEvent;
       };    
};

typedef struct DataRecord DataRecord;

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
int writeRecord(RingBuffer *buffer, int (*callback)(DataRecord*,void*) , void* data);

/*
 * RETURN: 0 if buffer was empty
 */
int readRecord(RingBuffer *buffer, void (*callback)(DataRecord*) );
#endif
