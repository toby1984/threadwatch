#include "config.h"

#ifndef PID_H

#define PID_H

#define DESIRED_LOOPS_PER_SECOND 1000

#define MAX_DELAY 10000

#define DEBUG_PID
#define USE_DELAY_LOOP

#define Kp -5.0
#define Ki  0.0
#define Kd  0.1 

void delayLoop(Config *config);

#endif
