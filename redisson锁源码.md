```java
 public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
     
        long time = unit.toMillis(waitTime);
        long current = System.currentTimeMillis();
        long threadId = Thread.currentThread().getId();
     	//尝试获取锁，返回时间是锁的剩余有效时间，如果自己获得了锁，就会返回null，代表抢锁成功，否则就代表锁已经被别的线程
     //抢走了，这里有个疑问，如果方法执行时间超过有效期怎么办？因此这个方法的源码里边给出了解决方案
        Long ttl = this.tryAcquire(waitTime, leaseTime, unit, threadId);
        if (ttl == null) {
            return true;
        } else {
            //抢锁失败后首先要计算当前锁的有效期，如果有效期已到，那就返回false,表明抢锁失败
            time -= System.currentTimeMillis() - current;
            if (time <= 0L) {
                this.acquireFailed(waitTime, unit, threadId);
                return false;//返回
            } else {
                //否则，就要订阅这个锁的频道。等待锁的释放者在释放时发布的消息，用到了subscribe/publish机制
                current = System.currentTimeMillis();
                RFuture<RedissonLockEntry> subscribeFuture = this.subscribe(threadId);
                //等待Publish的时间是有限的，期限就是time，如果返回false就表明到了time后对方依然没有释放锁，因此返回抢锁失败
                if (!subscribeFuture.await(time, TimeUnit.MILLISECONDS)) {
                    if (!subscribeFuture.cancel(false)) {
                        subscribeFuture.onComplete((res, e) -> {
                            if (e == null) {
                                this.unsubscribe(subscribeFuture, threadId);
                            }

                        });
                    }

                    this.acquireFailed(waitTime, unit, threadId);
                    return false;
                } else {
                    //代表在time到来之前返回了，但依然要计算抢锁的有效期是否已经到达。
                    boolean var14;
                    try {
                        //计算有效期
                        time -= System.currentTimeMillis() - current;
                        if (time > 0L) {
                            boolean var16;
                            do {//开始循环抢锁
                                long currentTime = System.currentTimeMillis();
                                ttl = this.tryAcquire(waitTime, leaseTime, unit, threadId);
                                if (ttl == null) {//抢到锁了，返回
                                    var16 = true;
                                    return var16;
                                }
					//继续计算有效期，如果到达有效期，就返回失败
                                time -= System.currentTimeMillis() - currentTime;
                                if (time <= 0L) {
                                    this.acquireFailed(waitTime, unit, threadId);
                                    var16 = false;
                                    return var16;
                                }
						//抢锁失败后，这里调用aqs进行自旋等待，根据time,ttl进行自旋等待
                                currentTime = System.currentTimeMillis();
                                if (ttl >= 0L && ttl < time) {
                                    ((RedissonLockEntry)subscribeFuture.getNow()).getLatch().tryAcquire(ttl, TimeUnit.MILLISECONDS);
                                } else {
                                    ((RedissonLockEntry)subscribeFuture.getNow()).getLatch().tryAcquire(time, TimeUnit.MILLISECONDS);
                                }
						//阻塞返回后继续判断time是否已经到达有效期，到达了就返回false;没到达就继续循环抢锁。
                                time -= System.currentTimeMillis() - currentTime;
                            } while(time > 0L);

                            this.acquireFailed(waitTime, unit, threadId);
                            var16 = false;
                            return var16;
                        }

                        this.acquireFailed(waitTime, unit, threadId);
                        var14 = false;
                    } finally {
                        this.unsubscribe(subscribeFuture, threadId);
                    }

                    return var14;
                }
            }
        }
    }
```

