TEMPLATE = app
CONFIG += console
CONFIG -= qt

SOURCES += \
    src/agent.c \
    src/threadlist.c \
    src/writerthread.c \
    src/events.c \
    src/pid.c \
    src/config.c

HEADERS += \
    src/writerthread.h \
    src/threadlist.h \
    src/global.h \
    src/events.h \
    src/agent.h \
    src/pid.h \
    src/config.h
