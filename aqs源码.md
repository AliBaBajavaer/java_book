aqs源码

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

```java
// 头结点，你直接把它当做 当前持有锁的线程 可能是最好理解的
private transient volatile Node head;

// 阻塞的尾节点，每个新的节点进来，都插入到最后，也就形成了一个链表
private transient volatile Node tail;

// 这个是最重要的，代表当前锁的状态，0代表没有被占用，大于 0 代表有线程持有当前锁
// 这个值可以大于 1，是因为锁可以重入，每次重入都加上 1
private volatile int state;

// 代表当前持有独占锁的线程，举个最重要的使用例子，因为锁可以重入
// reentrantLock.lock()可以嵌套调用多次，所以每次用这个来判断当前线程是否已经拥有了锁
// if (currentThread == getExclusiveOwnerThread()) {state++}
private transient Thread exclusiveOwnerThread; //继承自AbstractOwnableSynchronizer
```

waitStatus用于四个地方：互斥式同步队列（-1），共享式同步队列（-3），condition的阻塞队列（-2），取消抢锁的节点（1）。waitStatus<0代表正在争抢锁。头节点在唤醒时也是要唤醒他后面第一个waitStatus<0的节点。每一个抢锁的节点也要确保他前边那个节点的waitStatus<0这样他才会唤醒她，如果不是，那就要找到前边第一个waitstatus<0的节点。去掉status=cancell的节点；

只有懂了node节点，才会懂aqs；

aqs整体使用了模板方法模式，他的抢锁逻辑是：只有头节点对应的线程才拥有锁，头节点释放锁的时候，会唤醒他的后继节点对应的线程，该线程和还没有加入队列的线程一起抢锁。如果没有抢到，而head==null,那就要自己设定一个head节点。这里保证了最多只有线程中只有两个线程活着。

acquire方法：

​	抢锁成功则抢锁成功，否则就要加入同步队列里，在同步队列里，锁的安排是公平的。因此非公平锁的实现主要是在tryAcquire里实现的。方法也只是一条，需要不要判断前继节点。

​	如果抢锁失败就要安排在同步队列里。每次只有该线程的前继节点是头节点且该线程抢锁成功时，才代表抢锁成功，否则就要阻塞。

目前锁的高效性才是根本。而java里只提供了synchronized,reentrantlock对应的aqs，因此只需要平衡这两点就行了。此外还可以使用cas来简化锁的操作。只有学会使用信号量同步线程，学会尽可能优化锁带来的性能消耗，保证分布式锁的可靠性，才算是掌握了并发编程。 

为什么是shouldParkAfterFailedAcquire()?

只有prev.waitStatus=-1时，才会在释放锁的时候，唤醒当前节点。如果不为-1，就要修改前继节点，同时，在这个方法里删除掉已经取消争抢锁的节点。

什么时候waitStatus=1;

在addWaiter里当tryAcquire方法抛出异常时，就会调用cancelAcquire方法，将节点的waitStatus设为1；

对于取消抢锁的节点，aqs采取了惰性措施，只在释放锁以及shouldParkAfterAcquire的时候才会删除队列里waitStatus>0的节点。

释放锁的时候，并不会删除head节点，因为如果用的是非公平锁，不在队列里的线程抢锁成功了，就不用把自己设成头节点了，直接复用就行。

​	以上是对互斥锁的aqs使用方式；互斥锁又可以分为公平锁，非公平锁。

对于互斥锁，同步队列里节点的waitStatus只可能是-1或者>0即取消。

对于condition:

​	status=-2;

当多个线程读取集合的时候，应该尽可能减少锁的粒度。例如mysql的间隙锁，记录锁；redis也可以直接分片。这样可以增加并发度。当集合非常大的时候就需要考虑分段锁。以及查询的性能。



​	

以semaphore为例子

当多个线程读取集合的时候，应该尽可能减少锁的粒度。例如mysql的间隙锁，记录锁；redis也可以直接分片。这样可以增加并发度。当集合非常大的时候就需要考虑分段锁。以及查询的性能。

目前的重点就是多线程，分布式，数据库，计算机基础。
gfs学习：

​	gfs由存储服务，客户端，网关组成。gfs架构中最大的特点是没有袁术服务器组件，这有助于提升整个烯烃的性能，可靠性和稳定性。元数据值文件的属性信息，理由，元数据服务器的单点故障率较高。一旦元数据服务器崩溃，即使节点的冗余能力再强，系统也不可用了。GFS通过扩展可以支持数PB存储以及处理数千客户端的请求。gfs借助tcp/ip或者infiniBandRdna网络将物理分散分布的存储资源汇聚在一起。统一提供存储服务。使用同意全局命名空间来管理数据。

​	采用弹性哈希算法在存储池里面定位数据，取代了传统的元数据服务器定位数据，可以真正的实现并行化访问。改善了单点故障和性能瓶颈。

​	高可用性，通过配置某些类型的数据卷，对文件进行自动复制，即使单个节点出现故障，也不影响数据的访问。当数据出现不一致的时候，自动修复功能能够把数据恢复到正确的状态，数据的修复一增量的方式在后台进行。GFS采用标准的磁盘文件系统存储文件，数据可以使用传统访问磁盘的方式被访问。

​	如何同时保证高可用高性能：使用数据卷进行自动复制，使用弹性hash自动映射，这样就可以做到真正的并行化访问了。

​	全局统一命名空间可以基于不同节点进行负载均衡，同时动态的扩展，收缩存储资源，大大提高了存取效率和系统的可用性。
	弹性卷管理，类似于redis里的hash slot，将数据存储在逻辑卷里，逻辑卷从逻辑存储池进行独立逻辑划分。逻辑存储池可以在线的增加和移除。不会导致业务中断，逻辑卷可以在线增长和缩减，并可以在多个节点复杂均衡。不同的是gfs的逻辑卷可以在线增加减少，此外采用hash算法并行化确定访问节点与访问。通过自动复制客服redis的非高可用性，实现消息的原子保留。
 	基于标准协议 可以支持http,ftp即cluster原生协议，现有应用程序不需要做任何修改就可以对gfs里的数据进行访问。也可以使用专用api访问。
  	这些的基础都是磁盘的顺序写。
   操作系统进行进程管理的银行家算法可以解决死锁的问题。但其实不太能用到现实中。
   为什么要保证基本可用，正是因为无法完全保证强一致性的同时保证高可用，才出现的基本可用，以及弱一致性。比如1s只有200qps，那当然可以做到强一致性和可用，也就是acid。几乎所有的互联网系统采用的都是最终一致性，只有在实在无法使用最终一致性，才使用强一致性或事务，比如，对于决定系统运行的敏感元数据，需要考虑采用强一致性，对于与钱有关的支付系统或金融系统的数据，需要考虑采用事务。所有都采用的是最终一致性，但是，怎么实现最终一致性？目前的做法只有轮询。强一致性属于不存在延迟的最终一致性。如果业务无法容忍短暂的延迟，那就是用强一致性，如果可以容忍（比如qq状态）那就用最终一致性；
   最终一致性的核心就是以什么为准？以最新写入的数据为准或者第一次写入的数据为准。
   数据的一致性指的是那些支持事务的数据库即本身在qps低的时候可以做到cap的数据库，而不是本身就是ap的数据库，他们怎么也做不到最终一致性的。
   对于mysql:不同的表最终实现一致性，该怎么做？启用后台线程进行轮询；
   定时任务的完成：怎么实现最终一致性。比如中间失败了，那就要重试，重试后还要通过幂等性来保证最终一致性，即幂等就是为了最终一致性而存在的。即最终只会有一个状态。幂等正是最终一致性程序中的一个必须措施。而布隆过滤器，bitmap,唯一索引，只是为了达到幂等性的工具，他们最终都是为了最终一致性而服务的。这就是通过锁机制来实现的。也就是兜底机制。
   感悟：其实一个项目，不同进程之间的项目，就是分布式系统，考虑到就好了。
   最终一致性适合于高性能，该并发场景，且具备可用性。
   实现最终一致性的措施：版本向量，分布式事务，事件日志，消息队列，时间戳，状态机复制，冲突解决策略，复制策略，弱一致性。
   最终一致性一般采用消息队列实现。消息队列顺序消费，但这样，不好吧。
   最终一致性定义：对于同一个数据对象，如果没有更多的更新产生，最终所有的访问都会返回最新更新的数据(版本)
   最终一致性最常规的实现方式：
   	R>N-W;比如5个副本，以此写入两个，那就要以此至少读4次，其中N>1,否则就无法实现最终一致性。因此为了实现最终一致性，需要写两次，而对于redis，就是消息队列。
    事务：N个副本要么全更新，要么全不更新。
    可以采用两阶段提交，或者三阶段提交，消息队列+补偿机制，异步回调。
    两阶段提交：以事务协调者发出的指令位区分，prepare就是第一阶段，global commit是第二阶段。
    a.准备阶段
    	1.事务协调者向参与者a,b发起事务预处理请求，
	2.a,b收到预处理请求后，开始执行，写redolog,unlog,刷盘,但之后并不会commit,而是返回给coordinator返回vote commit
 	3.事务协调者收到所有的vote commit后，就进入提交阶段，如果没有收到任何一个，就回滚所有的事务。
  b.提交阶段
   	1.如果所有参与者都返回vote commit,那么协调者向所有参与者发送全局提交确认通知(global commit)，参与者会完成自身事务的提交，然后回复ack,
    	2.如果有任何一个参与者返回失败，就回滚事务。
    因此redis丢了怎么办？那就是说，使用两个以上副本，一次至少写入两个，另外一个是消息队列，重新生成主节点的时候，从消息队列里拿到数据，进行更新。
    2pc存在的问题：
    	1.2pc属于cp类型，非常耗费性能，如果其中一个参与者通信超时，其他参与者都会阻塞的占用资源，这个资源或者是数据库连接，或者是其他。第二，2pc特别依赖与事务协调者，如果事务协调者出现单点故障，整个系统将无法提供服务，虽然可以重新选举事务协调者，但也会造成之前的事务参与者无法释放资源。第三，如果发送commit后出现了网络抖动，一部分参与者没有收到commit，自然也就不会提交事务，返回ack,那么整个数据库系统就会出现不一致，即不能保证最终一致性。
     三阶段提交：
     a.cancommit阶段 验证阶段
     	1.检测各个参与者网络通信健康程度，事务协调者向所有参与者发送cancommit,所有参与者返回yes后，才进入下一个阶段。如果任何一个参与者反馈No,整个分布式事务就会中断。
      b.precommit阶段 执行阶段，不commit
    	1.阶段一中，所有事务参与者返回yes时，就会进入precommit阶段，事务协调者向所有参与者发送precommit请求，参与者收到后开始进行事务操作，并将undolog,redolog记录到事务日志里，执行完后，不提交commit，而是向协调者返回ack.所有参与者都返回ack时，才进入下一阶段，如果任何一个不返回，就回滚整个事务，协调者向参与者发送abort请求。
     c.docommit commit阶段
     	1.协调者向所有参与者发送docommit请求，参与者收到请求后，执行commit操作，并向协调者返回ack消息。协调者收到所有的ack后，提交事务。如果有一个没有返回ack或者返回超时，协调者就会向所有参与者发送abort请求，
回滚整个事务。
      3pc设置了超时时间，解决了参与者长时间无法与协调者通信的情况下，无法释放资源的问题。但如果，参与者实际上提交了事务，但协调者回滚了其他地区的事务时，就会造成数据不一致。因此3pc是努力向cp，但实际也会导致数据不一致性。或者说，为了cp不仅仅是3pc这么简单。
      感受：2pc,3pc都是cp系统（强一致系统），不具备高可用性。
      TCC:
      分为try,confime,cancel。
      try阶段：对业务系统做检测，以及资源预留
      confirm:确认执行业务操作
      cancel:取消业务执行。
tcc开源框架由hmily,seta.
分布式事务执行注意：
	可以将分布式事务变为异步任务，先写一次数据库，保证数据库成功，之后采用异步任务执行框架，保证最终一致性。其实也类似于消息队列了。理解来是这样的：
原来：
 事务A：
 	子事务a...
  	子事务b...
   子事务c...
   事务完成...
现在：
	   完成事务A所需参数写入数据库，提交....   
	   异步线程a执行子事务a...
	   异步线程b执行子事务b...
	   异步线程c执行子事务c...
	   事务完成.
    异步线程执行时允许失败，可以多次重复执行，重试超过一定次数需要额外处理。这样就可以保证最终一致性了。数据可也可以使用消息队列来代替。
    第一步举例：举例比如将购物时支付成功后。将商品订单号，收货地址，收货人姓名以任务上下文context的形式写在数据库表的一个字段里，只写一次数据库，就代表执行成功了。
    一般情况下，异步事务已经足够解决所有问题了。
    因此无论是nacos，或者是别的，本质上我们都讨论的是他们是什么数据系统，cp,还是ap，或者base。
    此外当前一些系统，由于raft,paxos的使用，还出现了cp+ha的方案，比如kafka，可以保证完全满足cp，就是每一个Get，可以拿到最新一个put,但同时，也保证了高可用，但这只是针对一条消息而言，多事务仍要用base,
