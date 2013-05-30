
static void JNICALL onVMStart(jvmtiEnv *jvmti_env,JNIEnv* jni_env);
static void JNICALL onVMDeath();

static void JNICALL onThreadStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread);
static void JNICALL onThreadEnd(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread);

static void startSamplingThread(JNIEnv* jni_env);
static void* sampleThreadStates(void *ptr); 
static void queryThreadState(ThreadListNode *current);

static int populateThreadStartRecord(DataRecord *record,ThreadListNode *current);
static int populateThreadSampleRecord(DataRecord *record,ThreadListNode *current);
static int populateThreadDeathRecord(DataRecord *record,ThreadListNode *current);

static void enterAgentMonitor(jvmtiEnv *jvmti);
static void exitAgentMonitor(jvmtiEnv *jvmti);