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
