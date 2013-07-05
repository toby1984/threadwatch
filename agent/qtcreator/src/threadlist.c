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

#include "global.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>

#include "threadlist.h"

static ThreadList threadList = { .head=NULL , .tail=NULL , .size=0 };

static pthread_mutex_t threadListLock = PTHREAD_MUTEX_INITIALIZER;

static int threadId=0;

/*
 * Create and add thread list node.
 */
ThreadListNode* addThreadListNode(char *threadName,jthread thread,jthread threadGlobalRef)
{
  const int nameLen = strlen(threadName)+1;

  ThreadListNode *newNode = (ThreadListNode*) malloc(sizeof(ThreadListNode));
  if ( ! newNode ) {
    fprintf(stderr,"ERROR: Failed to allocate memory for thread list node\n");
    abort();
  }

  newNode->next = NULL;
  newNode->threadName = (char*) malloc( nameLen );
  if ( ! newNode->threadName ) {
    fprintf(stderr,"ERROR: Failed to allocate memory for thread list node\n");
    abort();
  }
  strncpy( newNode->threadName , threadName, nameLen);
  
  newNode->thread = thread;
  newNode->threadGlobalRef = threadGlobalRef;
  newNode->previousThreadState = 0;

  // START: Critical section
  pthread_mutex_lock( &threadListLock);
  
  newNode->uniqueThreadId = threadId++;  

  if ( threadList.head == NULL ) {
    threadList.head = newNode;
  } else {
    threadList.tail->next=newNode;
  } 
  threadList.tail = newNode;
  threadList.size++;

  // END: Critical section
  pthread_mutex_unlock( &threadListLock);
  return newNode;
}

ThreadListNode *findThreadListNode(jthread thread) 
{    
  ThreadListNode *result;
  
      // START: Critical section
  pthread_mutex_lock( &threadListLock);
  
  result = threadList.head;
  while ( result != NULL && result->thread != thread ) {
    result = result -> next;    
  }
  // END: Critical section
  pthread_mutex_unlock( &threadListLock);
  return result;
}

/*
 * Unlink and free a thread list node.
 * RETURN VALUE: 1 if thread list is now empty
 */
void removeThreadListNode(jthread thread,CleanUpVisitor cleanUp,void *data)
{
  ThreadListNode *previous;
  ThreadListNode *current;
  int removed = 0;

  // START: Critical section
  pthread_mutex_lock( &threadListLock);

  previous = NULL;
  current = threadList.head;
  while ( current ) 
  {
    if ( current->thread == thread ) 
    {
      if ( previous == NULL ) { // remove first
        threadList.head = current->next;
      } else {
        // 2nd or last node
        previous->next = current->next;
      }
      if ( threadList.tail == current ) {
        threadList.tail = previous; 
      }      
      cleanUp(current,data);
      free( current->threadName );
      free( current ); 
      threadList.size--;
      removed = 1;   
      break; 
    } 
    previous = current;
    current = current-> next;
  }

  // END: Critical section
  pthread_mutex_unlock( &threadListLock);

  if ( ! removed ) {
    fprintf(stderr,"WARNING: Failed to release %p\n",thread);
  }
}

void visitThreadList( ThreadListVisitor visitor)
{
    ThreadListNode *current;

      // START: Critical section
    pthread_mutex_lock( &threadListLock);
  
    current = threadList.head;
    while( current ) 
    {
      visitor( current );
      current = current->next;
    }
     
  // END: Critical section
  pthread_mutex_unlock( &threadListLock);  
}
