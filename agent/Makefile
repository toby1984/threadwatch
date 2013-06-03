CC=/usr/bin/gcc
CFLAGS=-g -Wall -Wformat=0 -fPIC -O0
SRC=qtcreator/src
INCLUDES=-I/usr/local/java/include -I/usr/local/java/include/linux -Isrc
MYINCLUDES=qtcreator/src
TARGET=target
JAVA=java
JAVAC=javac

all: agent events threadlist writerthread pid
	gcc -pthread -fPIC -shared -Wall  -Wl,--no-as-needed -lm -lrt -o $(TARGET)/agent.so $(TARGET)/agent.o $(TARGET)/events.o $(TARGET)/threadlist.o $(TARGET)/writerthread.o $(TARGET)/pid.o

agent: $(SRC)/agent.c $(MYINCLUDES)/agent.h
	$(CC) -c -o $(TARGET)/agent.o $(SRC)/agent.c $(CFLAGS) ${INCLUDES}
 
events: $(SRC)/events.c $(MYINCLUDES)/events.h
	$(CC) -c -o $(TARGET)/events.o $(SRC)/events.c $(CFLAGS) ${INCLUDES}

writerthread: $(SRC)/writerthread.c $(MYINCLUDES)/writerthread.h
	$(CC) -c -o $(TARGET)/writerthread.o $(SRC)/writerthread.c $(CFLAGS) ${INCLUDES}

threadlist: $(SRC)/threadlist.c $(MYINCLUDES)/threadlist.h
	$(CC) -c -o $(TARGET)/threadlist.o $(SRC)/threadlist.c $(CFLAGS) ${INCLUDES}

pid: $(SRC)/pid.c $(MYINCLUDES)/pid.h
	$(CC) -c -o $(TARGET)/pid.o $(SRC)/pid.c $(CFLAGS) ${INCLUDES}
	 
.PHONY: clean

clean:
	rm -f target/*.o target/*.so

Test.class: Test.java
	$(JAVAC) Test.java

test:  Test.class all
	$(JAVA) -Xss512k -agentpath:target/agent.so Test
