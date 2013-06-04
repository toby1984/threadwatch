#ifndef CONFIG_H

#define CONFIG_H

typedef struct Config {
  char *outputFile;
  long maxPidDelay;
  int  verboseMode;
} Config;

extern Config configuration;

void initializeConfig(char *options);

#endif
