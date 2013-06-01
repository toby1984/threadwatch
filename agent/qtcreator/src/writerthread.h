#include "events.h"

#ifndef WRITERTHREAD_H
#define WRITERTHREAD_H

void startWriterThread(RingBuffer *buffer,char *fileToWriteTo);
void terminateWriterThread();

#endif