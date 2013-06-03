/**
 * Copyright 2012 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.threadwatcher;

import java.util.HashSet;
import java.util.Set;

public enum JVMTIThreadState
{
    // Thread is alive. Zero if thread is new (not started) or terminated.    
    ALIVE(0x0001,"ALIVE"), 
    // Thread has completed execution.    
    TERMINATED(0x0002,"TERMINATED"),
    // Thread is runnable.    
    RUNNABLE(0x0004,"RUNNABLE"),
    // Thread is waiting to enter a synchronization block/method or, after an Object.wait(), waiting to re-enter a synchronization block/method.    
    BLOCKED_ON_MONITOR_ENTER( 0x0400 , "WAITING (aquire)"),
    // Thread is waiting.    
    WAITING(0x0080,"WAITING"), 
    // Thread is waiting without a timeout. For example, Object.wait().
    WAITING_INDEFINITELY(0x0010,"WAITING (indef)"), 
    // Thread is waiting with a maximum time to wait specified. For example, Object.wait(long).    
    WAITING_WITH_TIMEOUT(0x0020,"WAITING (timeout)"), 
    // Thread is sleeping -- Thread.sleep(long).            
    SLEEPING(0x0040,"SLEEPING"), 
    // Thread is waiting on an object monitor -- Object.wait.    
    IN_OBJECT_WAIT(0x0100,"Object#wait()"), 
    // Thread is parked, for example: LockSupport.park, LockSupport.parkUtil and LockSupport.parkNanos.    
    PARKED(0x0200,"PARKED"),
    /*
     * Thread suspended. java.lang.Thread.suspend() or a JVM TI suspend function (such as SuspendThread) has been called on the thread. 
     * If this bit is set, the other bits refer to the thread state before suspension.     
     */
    SUSPENDED(0x100000,"SUSPENDED"), 
    // Thread has been interrupted.    
    INTERRUPTED(0x200000,"INTERRUPTED"), 
    // Thread is in native code--that is, a native method is running which has not called back into the VM or Java programming language code.    
    IN_NATIVE(0x400000,"IN NATIVE");     
    
    private final int mask;
    private final String name;
    
    private JVMTIThreadState(int mask,String name) {
        this.mask = mask;
        this.name = name;
    }
    
    public String getName()
    {
        return name;
    }
    
    public static Set<JVMTIThreadState> fromBitMask(int threadState) {
        final Set<JVMTIThreadState> result = new HashSet<>();
        for ( JVMTIThreadState state : values() ) {
            if ( state.isSet( threadState ) ) {
                result.add( state );
            }
        }
        return result;
    }
    
    public int getBitMask()
    {
        return mask;
    }
    
    public boolean isSet(int threadState) {
        return (threadState&mask) != 0;
    }
}
