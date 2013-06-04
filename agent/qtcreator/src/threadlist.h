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

typedef void (*ThreadListVisitor)(ThreadListNode*);
typedef void (*CleanUpVisitor)(ThreadListNode*,void*);

ThreadListNode* addThreadListNode(char *threadName,jthread thread,jthread threadGlobalRef);
void removeThreadListNode(jthread thread,CleanUpVisitor,void *);
ThreadListNode *findThreadListNode(jthread thread); 
void visitThreadList(ThreadListVisitor visitor);

#endif
