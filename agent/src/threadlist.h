#include <jvmti.h>

#ifndef THREADLIST_H

#define THREADLIST_H
typedef struct ThreadListNode 
{
   struct ThreadListNode *next;
   jint previousThreadState;
   int uniqueThreadId;    
   char *threadName;
   jthread thread;
   jthread threadGlobalRef;   
} ThreadListNode;

typedef struct ThreadList {
   ThreadListNode *head;
   ThreadListNode *tail; 
   int size;
} ThreadList;

ThreadListNode* addThreadListNode(char *threadName,jthread thread,jthread threadGlobalRef);
void removeThreadListNode(jthread thread,void (*cleanUp)(ThreadListNode*,void*) ,void *);
ThreadListNode *findThreadListNode(jthread thread); 
void visitThreadList( void (*visit)(ThreadListNode*) );

#endif
