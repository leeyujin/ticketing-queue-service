package com.ticketing.flow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserQueueScheduleService {
    private final UserQueueService userQueueService;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private static final String USER_QUEUE_WAIT_SCAN_KEY = "users:queue:*:wait";

    @Value("${scheduler.enabled}")
    private Boolean scheduling = false;

    // 서버 시작 후 5초 대기 , 10초 주기로 실행
    @Scheduled(initialDelay = 5000, fixedDelay = 10000)
    public void scheduleAllowUser() {
        if(!scheduling){
            return;
        }
        long maxAllowedUserCount = 1;
        // 사용자 허용
        reactiveRedisTemplate.scan(ScanOptions.scanOptions()
                        .match(USER_QUEUE_WAIT_SCAN_KEY)
                        .count(100)
                        .build())
                .map(key -> key.toString().split(":")[2])
                .flatMap(queue -> userQueueService.allowUser(queue.toString(), maxAllowedUserCount))
                .doOnNext(allowed -> log.info("Tried %d and allowed %d members".formatted(maxAllowedUserCount, allowed)))
                .subscribe();
    }
}
