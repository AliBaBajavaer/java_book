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
看门狗：

​	如果锁没有过期时间，那么redisson会自动给他加上30s的过期时间，而后调用定时任务，每过1/3时间时就自动续期。取消锁的时候，并不是之间删除，而是移除定时任务，等待锁的自动过期。
```java
 private <T> RFuture<Long> tryAcquireAsync(long waitTime, long leaseTime, TimeUnit unit, long threadId) {
        if (leaseTime != -1L) {//存在过期时间。
            return this.tryLockInnerAsync(waitTime, leaseTime, unit, threadId, RedisCommands.EVAL_LONG);
        } else {//不存在的时候，使用静态常量，加上一个过期时间，这样即使服务崩溃了，也可以保证锁的释放
            //this.commandExecutor.getConnectionManager().getCfg().getLockWatchdogTimeout()就是30s
            RFuture<Long> ttlRemainingFuture = this.tryLockInnerAsync(waitTime, this.commandExecutor.getConnectionManager().getCfg().getLockWatchdogTimeout(), TimeUnit.MILLISECONDS, threadId, RedisCommands.EVAL_LONG);
            ttlRemainingFuture.onComplete((ttlRemaining, e) -> {
                if (e == null) {//获取锁成功了
                    if (ttlRemaining == null) {
                        this.scheduleExpirationRenewal(threadId);//开始定时续期
                    }
	
                }
            });
            return ttlRemainingFuture;//获取锁失败了，返回锁现在的过期时间
        }
    }
//定时续期锁
 private void scheduleExpirationRenewal(long threadId) {
        ExpirationEntry entry = new ExpirationEntry();
        ExpirationEntry oldEntry = (ExpirationEntry)EXPIRATION_RENEWAL_MAP.putIfAbsent(this.getEntryName(), entry);
        if (oldEntry != null) {
            oldEntry.addThreadId(threadId);
        } else {
            entry.addThreadId(threadId);
            this.renewExpiration();//开启定时任务
        }

    }
//在这个方法里会创建一个定时任务，每1/3时间自动更新锁的有效期
 private void renewExpiration() {
        ExpirationEntry ee = (ExpirationEntry)EXPIRATION_RENEWAL_MAP.get(this.getEntryName());
        if (ee != null) {
            //commandExecutor应该是一个线程池，用于执行定时任务
            Timeout task = this.commandExecutor.getConnectionManager().newTimeout(new TimerTask() {
                public void run(Timeout timeout) throws Exception {
                    ExpirationEntry ent = (ExpirationEntry)RedissonLock.EXPIRATION_RENEWAL_MAP.get(RedissonLock.this.getEntryName());
                    if (ent != null) {
                        Long threadId = ent.getFirstThreadId();
                        if (threadId != null) {
                            //这段代码就是自动更新锁的有效期
                            RFuture<Boolean> future = RedissonLock.this.renewExpirationAsync(threadId);
                            future.onComplete((res, e) -> {
                                if (e != null) {
                                    RedissonLock.log.error("Can't update lock " + RedissonLock.this.getName() + " expiration", e);
                                } else {
                                    if (res) {
                                        RedissonLock.this.renewExpiration();
                                    }

                                }
                            });
                        }
                    }
                }
            }, this.internalLockLeaseTime / 3L, TimeUnit.MILLISECONDS);
            ee.setTimeout(task);
        }
    }
//自动更新锁的有效期；
protected RFuture<Boolean> renewExpirationAsync(long threadId) {
        return this.evalWriteAsync(this.getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN, "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then redis.call('pexpire', KEYS[1], ARGV[1]); return 1; end; return 0;", Collections.singletonList(this.getName()), this.internalLockLeaseTime, this.getLockName(threadId));
    }

//取消任务的时候，只是将任务从线程池里面移除，锁到期自动取消；、
 void cancelExpirationRenewal(Long threadId) {
        ExpirationEntry task = (ExpirationEntry)EXPIRATION_RENEWAL_MAP.get(this.getEntryName());
        if (task != null) {
            if (threadId != null) {
                task.removeThreadId(threadId);
            }

            if (threadId == null || task.hasNoThreads()) {
                Timeout timeout = task.getTimeout();
                if (timeout != null) {
                    timeout.cancel();//取消任务
                }

                EXPIRATION_RENEWAL_MAP.remove(this.getEntryName());
            }

        }
    }

```

