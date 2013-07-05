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

#ifndef GLOBAL_H
#define GLOBAL_H

// #define DEBUG
#define DEBUG_STRUCTURE

// number of events that may be kept in the ringbuffer
#define SAMPLE_RINGBUFFER_SIZE 10240

#define FILEHEADER_MAGIC 0xdeadbeef

// time in millis the writer thread will sleep if the ringbuffer is empty
#define WRITERTHREAD_SLEEP_TIME_MILLIS 100

// max. support thread name length (longer names will be truncated)
#define MAX_THREAD_NAME_LENGTH 50

// Name of agent JavaThread 
#define SAMPLING_THREAD_NAME "thread-watch-sampler"

// default output file events will be written to
#define OUTPUT_FILE "/tmp/threadwatcher.out"

#endif
