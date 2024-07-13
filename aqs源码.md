cas源码

​	aqs采用了模板方法设计模式和装饰器模式，获取锁的操作均为acquire方法，里面的tryAcquire方法则是自定义的获取规则。非公平锁在加锁时不需要判断自己是不是头结点的后继节点，公平锁则只有head.next才可以获得锁。

​	aqs定义了acquire，acquireInterupt,acquireNaos方法，他们的加锁流程基本相同，只是获取锁失败时的流程不同，acquire只是让线程阻塞，第二个则是在线程被中断时抛出异常，第三个则是定义了判断时间的逻辑。

​	reetrantLock里的抽象类sync实现了aqs接口，他的两个实现类fairSync,nonFairSync实现了tryAcquire方法，reetrantLock本质上是排他锁，因为他没有实现模板方法里的shared相关的方法。

​	其余的多线程组件semaphore,countdownLatch,CycliBairrer都是aqs的实现类。不过他们都是共享锁的实现方式

​	reetrantReadWriteLock实现了全部的方法，不过他的读写锁的实现和其他多线程组件读锁的实现不一样，他将state变量一分为二，高位维持读锁，低位是写锁。

才查看aqs实现类时，关键就是看tryAcquire,tryAcquireShared,tryRelease tryReleaseShared四个方法。以及他们自己的接口。

```java
	ReetrantLock是可重入的。他在加锁和释放锁时都增加了重入的操作。
reetrantlock，非公平获取锁，不会阻塞和自旋，直接返回
        //nonfairSync的加锁方式
     
 final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) { //直接乐观锁判断不用加阻塞队列
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
 // fairSync的tryAcquire();是公平锁，获取锁时还要判断是不是同步队列的队首元素.
  protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() && //
                    compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
    }
  
  //nofairsync相比于sync重写了acquire方法，fairsync直接用了aqs里的acquire方法，即阻塞的，公平的获取锁
     final void lock() {
            acquire(1);
        }
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))  //调用了addWaiter方法，将当前节点加入同步队列里
            selfInterrupt();
    }
    
    nonfairsync则是：我们可以看到，直接就是cas操作。
   final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }
//通过该方法可知reentranlock默认是非公平的
          public ReentrantLock() {
        sync = new NonfairSync();
    }
//加锁时默认调用的也是sync的加锁方式。
 public void lock() {
        sync.lock();
    }

//nonfairSync的加锁方式，可以看到无论是公平队列还是非公平队列，都用的是acquire方法，即阻塞的等待锁。
     final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

      protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        } 
//如果是非阻塞的获取锁。都调取的是sync本来就有的方法。
 public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }
//实际fairSync和nonfariSync的唯一区别就是tryAcquire方法，fairSync要判断有没有前驱节点。
//本质上只有tryLock调用了非公平的获取锁//但其实acquire方法里调用了tryAcquire方法，这样就有区别了。因此还是有区别的。非公平队列调用的都是乐观方法，公平队列调用的都是需要判断钱去节点的。因为他们调用的都是acquire方法，acquire作为模板方法，里面调用了子类实现的的tryAcquire()方法，而
```





release()方法可以唤醒head的后继节点

```java
private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         */
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         */
    //唤醒后继节点。
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            LockSupport.unpark(s.thread);
    }

```

以semaphore为例子

CountDownLatch

CyclicBairrer

ReentrantReadWriteLock