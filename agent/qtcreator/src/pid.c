#include <stdio.h>
#include <unistd.h>
#include <math.h>
#include <time.h>
#include "pid.h"

static double calculateDelay(double e, double Ta);
static double delay(long value);

static int  initialized=0;

static struct timespec pidStartTime = {0,0};
static struct timespec pidCurrentTime = {0,0};

static double esum = 0.0;
static double ealt = 0.0;
static double delaySum = 0.0;

static unsigned long long loopCount = 0;
static double deviation;



static double dummyValue = 0.0;

double elapsedSeconds() {

    double deltaSeconds=pidCurrentTime.tv_sec-pidStartTime.tv_sec;
    double deltaNanos;
    if ( deltaSeconds < 1.0 ) {
        deltaNanos = pidCurrentTime.tv_nsec - pidStartTime.tv_nsec;
    } else {
        deltaNanos = (1000000000.0 - pidStartTime.tv_nsec)+pidCurrentTime.tv_nsec;
    }
    return deltaSeconds+deltaNanos/1000000000.0;
}

double fakeSomeWork()
{
  double x = 1;
  int i,j;
  for ( i = 100 ; i > 0 ; i--)
  {
     for ( j = 2000 ; j > 0 ; j--) {
        x = x*x+(i*j)+i*x;
     }
  }
  return x;
}

int main(int argc,char **args) {

  unsigned long i;
  for ( i = 1000000 ; i > 0 ; i-- )
  {
    fakeSomeWork();
     delayLoop();
  }
  return 0;
}

void delayLoop()
{
    long delayMicros;
    double deltaTimeSeconds;
    double actualLoopsPerSecond;

    if ( ! initialized )
    {
        if ( clock_gettime( CLOCK_REALTIME, &pidStartTime ) ) {
            fprintf(stderr,"ERROR: clock_gettime() failed\n");
        }
        initialized=1;
        return;
    }

    if ( clock_gettime( CLOCK_REALTIME , &pidCurrentTime ) ) {
        fprintf(stderr,"ERROR: clock_gettime() failed\n");
        return;
    }

    loopCount++;

    deltaTimeSeconds = elapsedSeconds();
    actualLoopsPerSecond = ( loopCount / deltaTimeSeconds );
    deviation = DESIRED_LOOPS_PER_SECOND  - actualLoopsPerSecond;

    delayMicros = (long) round( MAX_DELAY * calculateDelay( deviation , 2 ) );

#ifdef DEBUG_PID
    delaySum += delayMicros;

    if ( (loopCount % DESIRED_LOOPS_PER_SECOND) == 0 ) {
        printf("PID: %f seconds elapsed (actual samples/second: %f , deviation: %f, delay loop iterations: %ld, avg. iterations: %f)\n",deltaTimeSeconds,actualLoopsPerSecond,deviation, delayMicros,delaySum/loopCount);
    }
#endif

    if ( delayMicros > 0 ) {
        dummyValue = delay( delayMicros );
    }
}

static double delay(long value)
{
        double dummy = 0.0;
#ifdef USE_DELAY_LOOP
        long j;
    for ( ; value > 0 ; value-- ) {
      for (  j = 50 ; j > 0 ; j--) {
          dummy=dummy*dummy*value+j;
      }
    }
#else
        usleep(value);
#endif
      return dummy;
}

static double calculateDelay(double e, double Ta)
{
    double y;

    /*
e = w - x;					//Vergleich
esum = esum + e;				//Integration I-Anteil
y = Kp*e + Ki*Ta*esum + Kd/Ta*(e – ealt);	//Reglergleichung
ealt = e;
     */
    esum += e;
    if ( esum > 10 ) {
        esum = 10;
    } else if ( esum < -10) {
        esum = -10;
    }
    y = Kp * e + Ki*Ta*esum + Kd/Ta*(e-ealt);
    ealt = e;

    if ( y < 0 ) {
        y = 0;
    } else if ( y > 1 ) {
        y = 1;
    }
    return y;
}
