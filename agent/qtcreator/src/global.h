
#ifndef GLOBAL_H
#define GLOBAL_H

// #define DEBUG

// number of data records that may be kept in the ringbuffer
#define SAMPLE_RINGBUFFER_SIZE 10240

#define GETTIME_BENCHMARK_LOOPCOUNT 35000000

#define FILEHEADER_MAGIC 0xdeadbeef

// time in millis the writer thread will sleep if the ringbuffer is empty
#define WRITERTHREAD_SLEEP_TIME_MILLIS 100

// max. support thread name length (longer names will be truncated)
#define MAX_THREAD_NAME_LENGTH 50

// Time of wait between consecutive GetThreadState() calls
#define SAMPLING_INTERVAL_MILLIS 1

// Name of agent JavaThread 
#define SAMPLING_THREAD_NAME "thread-watch-sampler"

// name of file data records will be written to
#define OUTPUT_FILE "/tmp/threadwatcher.out"

#endif
