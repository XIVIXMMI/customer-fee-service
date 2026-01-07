package com.hdbank.customer_fee_service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Distributed lock using PosgreSQL advisory locks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Try to acquire lock
     * @param lockKey unique identifier for the lock (use hashCode instead of job name)
     * @return true if lock acquired, false otherwise
     */
    public boolean tryLock(String lockKey){
        long lockId = Math.abs(lockKey.hashCode());

        try {
            Boolean result = jdbcTemplate.queryForObject(
                    "SELECT pg_try_advisory_lock(?)",
                    Boolean.class,
                    lockId
            );
            if(Boolean.TRUE.equals(result)){
                log.info("Lock acquired for key: {} (id: {})",lockKey, lockId);
                return true;
            } else {
                log.info("Lock already held for key: {} (id: {})",lockKey,lockId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error acquiring lock for key: {}",lockKey,e);
            return false;
        }
    }

    public void unlock(String lockKey){
        long lockId = Math.abs(lockKey.hashCode());
        try {
            Boolean result = jdbcTemplate.queryForObject(
                    "SELECT pg_advisory_unlock(?)",
                    Boolean.class,
                    lockId
            );
            if(Boolean.TRUE.equals(result)){
                log.info("Lock released for key: {} (id: {})",lockKey,lockId);
            } else{
                log.info("Lock was not held for key: {} (id: {})", lockKey, lockId);
            }
        } catch (Exception e){
            log.error("Error releasing lock for key: {}", lockKey,e);
        }
    }

    /**
     * Executed with lock
     * Automatically acquires and releases lock
     */
    public void executeWithLock(String lockKey, Runnable task){
        if(!tryLock(lockKey)){
            log.info("Could not acquires lock, skipping execution for: {}", lockKey);
            return;
        }
        try {
            log.info("Executing task with lock: {}", lockKey);
            task.run();
        } finally {
            unlock(lockKey);
        }
    }

}
