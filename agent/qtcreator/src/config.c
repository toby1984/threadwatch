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

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "pid.h"
#include "global.h"
#include "config.h"

static int getNameValuePairs(char *s, char **tokens,int maxTokenCount) {

    char *start=s;
    char *ptr=s;
    int tokenCount=0;

    if ( *ptr)
    {
        while (*ptr)
        {
            if ( *ptr == ',') {
                *ptr=0;
                tokens[tokenCount++] = start;
                start=ptr+1;
                if ( tokenCount == maxTokenCount ) {
                    return tokenCount;
                }
            }
            ptr++;
        }
        if ( tokenCount < maxTokenCount ) {
            tokens[tokenCount++] = start;
        }
    }
    return tokenCount;
}

static int splitNameValuePair(char *nameValuePair,char **name,char **value)
{
   char *ptr = nameValuePair;

   while( *ptr && *ptr != '=' ) {
     ptr++;
   }
   if ( *ptr == '=' )
   {
      *name=nameValuePair;
      *ptr=0;
      *value=ptr+1;
      return 1;
   }
   return 0;
}

void initializeConfig(char *options)
{
    char *nameValuePairs[10];
    char *name;
    char *value;
    int i;
    int pairCount=0;

    // setup default configuration
    configuration.outputFile = strdup( OUTPUT_FILE );
    if ( ! configuration.outputFile ) {
        fprintf(stderr,"Failed to allocate memory\n");
        abort();
    }

    configuration.verboseMode = 0;
    configuration.maxPidDelay = MAX_DELAY;

    // process options
    if ( options != NULL && *options )
    {
        pairCount = getNameValuePairs( options , &nameValuePairs[0] , 10 );
        for ( i = 0 ; i < pairCount ; i++ )
        {
            if ( splitNameValuePair( nameValuePairs[i] , &name, &value ) )
            {
                if ( ! strcasecmp("verbose" , name ) ) {
                    configuration.verboseMode=1;
                } else if ( ! strcasecmp("file" , name ) ) {
                    free(configuration.outputFile);
                    configuration.outputFile = strdup(value);
                    if ( ! configuration.outputFile )
                    {
                        fprintf(stderr,"Failed to allocate memory\n");
                        abort();
                    }
                }
                else if ( ! strcasecmp( "maxdelay" , name ) )
                {
                    configuration.maxPidDelay = atol( value );
                }
            }
        }
    }

    if ( configuration.verboseMode ) {
        printf("INFO: Verbose mode enabled.\n");
        printf("INFO: Output file: %s\n",configuration.outputFile);
        printf("INFO: Max. delay loop iterations: %ld\n",configuration.maxPidDelay);
    }
}
