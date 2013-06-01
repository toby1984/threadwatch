#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <jvmti.h>
#include <unistd.h>
#include <pthread.h>
#include <sys/time.h>

#include "global.h"
#include "events.h"
#include "threadlist.h"
#include "agent.h"
#include "writerthread.h"

// thread responsible for sampling JVM thread states periodically 
static pthread_t samplingThreadId;
static jvmtiEnv *samplingThreadJvmti = NULL;
static volatile int terminateSamplingThread = 0;

static RingBuffer *sampleBuffer = NULL;

static JavaVM *currentVM = NULL;
static JNIEnv* globalJniEnv = NULL;
static jvmtiEnv *jvmti = NULL;

static jrawMonitorID lock;

static int getTimeExecNanos=0;

/* Get name for JVMTI error code */
static char * getErrorName(jvmtiEnv *jvmti, jvmtiError errnum)
{
    jvmtiError err;
    char      *name;

    err = (*jvmti)->GetErrorName(jvmti, errnum, &name);
    if ( err != JVMTI_ERROR_NONE ) {
        fprintf( stderr,"ERROR: JVMTI GetErrorName error err=%d\n", err);
        abort();
    }
    return name;
}

/* Deallocate JVMTI memory */
static void deallocate(jvmtiEnv *jvmti, void *p)
{
    jvmtiError err;

    err = (*jvmti)->Deallocate(jvmti, (unsigned char *)p);
    if ( err != JVMTI_ERROR_NONE ) {
        fprintf( stderr,"ERROR: JVMTI Deallocate error err=%d\n", err);
        abort();
    }
}

/* Check for JVMTI error */
#define CHECK_JVMTI_ERROR(jvmti, err) checkJvmtiError(jvmti, err, __FILE__, __LINE__)

static void printJvmtiError(jvmtiEnv *jvmti, jvmtiError err, char *file, int line) 
{
        char *name = getErrorName(jvmti, err);
        fprintf( stderr,"ERROR: JVMTI error err=%d(%s) in %s:%d\n",err, name, file, line);
        deallocate(jvmti, name);    
}
#define PRINT_JVMTI_ERROR(jvmti,err) printJvmtiError(jvmti,err, __FILE__, __LINE__ )

static void checkJvmtiError(jvmtiEnv *jvmti, jvmtiError err, char *file, int line)
{
    if ( err != JVMTI_ERROR_NONE ) 
    {
        printJvmtiError(jvmti,err,file,line);
        abort();
    }
}

/* Enter agent monitor protected section */
static void enterAgentMonitor(jvmtiEnv *jvmti)
{
    CHECK_JVMTI_ERROR(jvmti, (*jvmti)->RawMonitorEnter(jvmti, lock));
}

/* Exit agent monitor protected section */
static void exitAgentMonitor(jvmtiEnv *jvmti)
{
    CHECK_JVMTI_ERROR(jvmti, (*jvmti)->RawMonitorExit(jvmti, lock));
}

/*
 * JVMTI Agent initializer.
 */
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {

  jvmtiError          err;
  jvmtiEventCallbacks callbacks;

  currentVM = jvm;

  sampleBuffer = createRingBuffer(); 
   
#ifdef DEBUG  
  printf("onLoad() called , VM: %p\n",currentVM);
#endif  
   
  // clear memory
  memset(&callbacks, 0, sizeof(callbacks));

  (*jvm)->GetEnv(jvm, (void**) &jvmti, JVMTI_VERSION_1_0);
 
  // Create the raw monitor
  err = (*jvmti)->CreateRawMonitor(jvmti, "agent lock", &lock);
  CHECK_JVMTI_ERROR(jvmti, err);

#ifdef DEBUG    
  printf("Setting event callbacks\n");
#endif  
  callbacks.VMStart = &onVMStart;
  callbacks.VMDeath = &onVMDeath;
  callbacks.ThreadStart = &onThreadStart;
  callbacks.ThreadEnd = &onThreadEnd;

  err = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, sizeof(callbacks));
  CHECK_JVMTI_ERROR(jvmti,err);

#ifdef DEBUG    
  printf("Enabling events\n");
#endif
  
  err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE , JVMTI_EVENT_VM_START , (jthread) NULL);
  CHECK_JVMTI_ERROR(jvmti,err);

  err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE , JVMTI_EVENT_VM_DEATH , (jthread) NULL);
  CHECK_JVMTI_ERROR(jvmti,err);

  err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE , JVMTI_EVENT_THREAD_START, (jthread) NULL);
  CHECK_JVMTI_ERROR(jvmti,err);

  err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE , JVMTI_EVENT_THREAD_END, (jthread) NULL);
  CHECK_JVMTI_ERROR(jvmti,err);

   return JNI_OK;
}

/*
 * Called after JVM has started.
 */
static void JNICALL onVMStart(jvmtiEnv *jvmti_env,JNIEnv* jni_env)
{
  enterAgentMonitor(jvmti);

  // START: Critical section

  globalJniEnv = jni_env;

#ifdef DEBUG    
  printf("VM started.\n");
#endif  

  // Start sampling thread
  startSamplingThread(jni_env);

  // END: Critical section
  exitAgentMonitor(jvmti);
}

/*
 * Called when JVM is terminating.
 */
static void JNICALL onVMDeath() 
{
#ifdef DEBUG      
  printf("VM exit.\n");
#endif  
  
__sync_synchronize();   
  terminateSamplingThread = 1;

#ifdef DEBUG
  printf("Waiting for sampling thread to die\n");
#endif
  
  if ( pthread_join( samplingThreadId , NULL ) ) 
  {
    fprintf(stderr,"ERROR: Failed to join() with sampling thread");    
  }
   
#ifdef DEBUG
  printf("Terminating writer thread\n");
#endif   
  terminateWriterThread();
  
  if ( sampleBuffer != NULL) 
  {          
    destroyRingBuffer(sampleBuffer);  
  }
}

/*
 * Forks a POSIX background thread that will periodically 
 * query the state of all threads we know about. 
 */
static void startSamplingThread(JNIEnv* jni_env) 
{
  int err;
  pthread_attr_t tattr;

#ifdef DEBUG    
  printf("Starting sampling thread (env: %p\n",jni_env);
#endif  

  /* initialized with default attributes */
  pthread_attr_init(&tattr);

  /* call an appropriate functions to alter a default value */
  pthread_attr_setstacksize(&tattr , 3 * 1024 * 1024);

  if ( ( err = pthread_create( &samplingThreadId, &tattr , &sampleThreadStates , (void*) jni_env) ) ) {
        fprintf( stderr,"ERROR: Failed to create pthread , err=%d\n", err);
        abort();
  }
}

/*
 * Periodically samples the current state of all JVM threads we know about.
 */
static void* sampleThreadStates(void *ptr) 
{   
   jint err;
   JNIEnv *env;
   JavaVMAttachArgs attach_args = {JNI_VERSION_1_2, SAMPLING_THREAD_NAME ,NULL};
   
   // the next line is actually a hack to work around a race-condition
   // when this thread starts to run although the thread that spawned it
   // has not yet left the onVMStart() method
   
   // TODO: Get rid of using a Java native monitor , use a pthread_mutex to protected JNI callback methods
   usleep(1000*1000); // 1 second
   
   startWriterThread(sampleBuffer,OUTPUT_FILE);   
   
#ifdef DEBUG     
   printf("*** attaching sampling thread.\n"); 
#endif

   err = (*currentVM)->AttachCurrentThreadAsDaemon(currentVM,(void **) &env, &attach_args);
   if ( err != 0 ) {
        fprintf(stderr,"ERROR: FAILED to attach sampling thread.\n");        
        abort();
   } 

   printf("INFO: Ready to sample thread states, ring buffer size: %d samples \n",SAMPLE_RINGBUFFER_SIZE); 
   
  (*currentVM)->GetEnv(currentVM, (void**) &samplingThreadJvmti, JVMTI_VERSION_1_0);   

   while( ! terminateSamplingThread ) 
   {
     usleep(SAMPLING_INTERVAL_MILLIS*1000);
     visitThreadList( &queryThreadState ); 
     __sync_synchronize(); 
   }
   
#ifdef DEBUG   
   printf("*** Sampling thread detached, terminating.\n");    
#endif
   
   (*currentVM)->DetachCurrentThread(currentVM);   
      
   return NULL;
}

static void queryThreadState(ThreadListNode *current) 
{    
     writeRecord(sampleBuffer, &populateThreadSampleRecord, (void*) current );
}

static int populateThreadSampleRecord(DataRecord *record,ThreadListNode *current) 
{
   jvmtiError err;    
   jint threadState;
   
#ifdef DEBUG     
   printf("Querying thread state for %lx : ",current->thread);
   fflush(stdout);
#endif   

   err = (*samplingThreadJvmti)->GetThreadState(samplingThreadJvmti,current->threadGlobalRef,&threadState);
   if ( err != JVMTI_ERROR_NONE ) 
   {
     PRINT_JVMTI_ERROR(samplingThreadJvmti,err);
     return 0;
   } 

   if ( threadState == current->previousThreadState) {
       return 0;
   }

   if ( clock_gettime( CLOCK_REALTIME , &record->timestamp) ) {
     fprintf(stderr,"ERROR: Failed to get current time?");
     return 0;    
   }
   
#ifdef DEBUG     
   printf("Thread %d , state: %d\n",current->uniqueThreadId,threadState);
   fflush(stdout);
#endif       

   current->previousThreadState = threadState;

   record->type = EVENT_THREAD_SAMPLE;
   record->uniqueThreadId=current->uniqueThreadId;
   record->stateChangeEvent.state = threadState;
   return 1;
}

static int isSamplingThread(jvmtiThreadInfo *threadInfo) {
    return strcmp(SAMPLING_THREAD_NAME,threadInfo->name) == 0;
}

static int populateThreadStartRecord(DataRecord *record,ThreadListNode *current) 
{

    if ( clock_gettime( CLOCK_REALTIME , &record->timestamp ) ) {
      fprintf(stderr,"ERROR: Failed to get current time?");
      return 0;    
    }    
    record->type = EVENT_THREAD_START;
    record->uniqueThreadId=current->uniqueThreadId;
    strncpy( &record->startEvent.threadName,current->threadName,MAX_THREAD_NAME_LENGTH);
    return 1;
}

static int populateThreadDeathRecord(DataRecord *record,ThreadListNode *current) 
{
    if ( clock_gettime( CLOCK_REALTIME , &record->timestamp ) ) {
      fprintf(stderr,"ERROR: Failed to get current time?");
      return 0;    
    }    
    record->type = EVENT_THREAD_DEATH;
    record->uniqueThreadId=current->uniqueThreadId;
    return 1;    
}

static int benchmarkGetTime() {

    unsigned long i = GETTIME_BENCHMARK_LOOPCOUNT;
    struct timespec start;
    struct timespec end;
    struct timespec dummy;

    double deltaSeconds=0;
    double deltaNanos=0;
    double execTime;

    if ( clock_gettime( CLOCK_REALTIME , &start ) ) {
      fprintf(stderr,"ERROR: Failed to get current time?");
      return 1;
    }

    for ( ; i > 0 ; i-- )
    {
      clock_gettime( CLOCK_REALTIME , &dummy);
    }

    if ( clock_gettime( CLOCK_REALTIME , &end) ) {
      fprintf(stderr,"ERROR: Failed to get current time?");
      return 1;
    }

    deltaSeconds = end.tv_sec - start.tv_sec;
    if ( deltaSeconds == 0 ) {
        printf("case #1\n");
        deltaNanos = end.tv_nsec-start.tv_nsec;
        execTime = deltaNanos / (double) GETTIME_BENCHMARK_LOOPCOUNT;
    } else {
        printf("case #2\n");
        deltaNanos = 1000000000.0-start.tv_nsec + end.tv_nsec;
        while ( deltaNanos > 1000000000.0 )
        {
            printf("loop\n");
            deltaSeconds++;
            deltaNanos -= 1000000000.0;
        }
        execTime = (deltaSeconds*1000000000.0 - deltaNanos) / (double) GETTIME_BENCHMARK_LOOPCOUNT;
    }
    printf("delta seconds: %f / delta nanos: %f\n",deltaSeconds,deltaNanos);
    printf("Calling clock_gettime() takes %f nanoseconds\n",execTime);
    return 1;
}

static void JNICALL onThreadStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread) 
{ 
    jthread threadGlobalRef;    
    jvmtiThreadInfo threadInfo;
    ThreadListNode *newNode;

    enterAgentMonitor(jvmti_env);

    if ( getTimeExecNanos == 0 ) {
        getTimeExecNanos = benchmarkGetTime();
    }
           
    // START: Critical section
    (*jvmti)->GetThreadInfo(jvmti,thread,&threadInfo);
          
    if ( threadInfo.name != NULL && ! isSamplingThread( &threadInfo ) ) { // ignore attaching of our sampling thread        

#ifdef DEBUG  
      printf("Thread started: %s (ID: %lx)\n",threadInfo.name,thread);
#endif      
    
      // create global ref to the thread instance so it
      // does not get GC'ed after this function returns..we need it in the sampling thread

      threadGlobalRef = (*jni_env)->NewGlobalRef(jni_env,thread);
      newNode = addThreadListNode( threadInfo.name , thread , threadGlobalRef );
      
      writeRecord(sampleBuffer, &populateThreadStartRecord, (void*) newNode );      
    }

    // END: Critical section
    exitAgentMonitor(jvmti_env);
}

static void cleanUp(ThreadListNode *node,jvmtiEnv *jvmti_env)
{
#ifdef DEBUG
   printf("cleanUp(): Thread %lx : ",node->thread);
   fflush(stdout);
#endif

   enterAgentMonitor(jvmti_env);
   // START: Critical section

   // TODO: Deleting ref is currently disabled since it triggers SEGV ....
    // (*globalJniEnv)->DeleteGlobalRef(globalJniEnv,node->threadGlobalRef);

   // END: Critical section
   exitAgentMonitor(jvmti_env);
}

static void JNICALL onThreadEnd(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread)
{
    jvmtiThreadInfo threadInfo;
    ThreadListNode *existingNode;

    enterAgentMonitor(jvmti_env); 

    // START: Critical section
    (*jvmti)->GetThreadInfo(jvmti,thread,&threadInfo);
          
    if ( threadInfo.name != NULL && ! isSamplingThread( &threadInfo ) ) 
    {
#ifdef DEBUG          
      printf("Thread ended: %s (ID: %lx)\n",threadInfo.name,thread);    
#endif      
      
      existingNode = findThreadListNode( thread );
      if ( existingNode != NULL ) {
        writeRecord(sampleBuffer, &populateThreadDeathRecord, (void*) existingNode );  
      }      
      removeThreadListNode(thread,&cleanUp, jvmti_env);
    }
    
    // END: Critical section
    exitAgentMonitor(jvmti_env);
}
