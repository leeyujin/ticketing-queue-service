package com.ticketing.flow.service;

import com.ticketing.flow.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserQueueService {
    private static final String USER_QUEUE_WAIT_KEY = "users:queue:%s:wait";
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    // 대기열 등록 API
    public Mono<Long> registerWaitQueue(final String queue, final Long userId){
        var unixTimeStamp = Instant.now().getEpochSecond();
        String key = USER_QUEUE_WAIT_KEY.formatted(queue);
        return reactiveRedisTemplate.opsForZSet().add(key, userId.toString(), unixTimeStamp)
                .filter(result -> result == true)
                .switchIfEmpty(Mono.error(ErrorCode.QUEUE_ALREADY_REGISTERED_USER.build()))
                .flatMap(i -> reactiveRedisTemplate.opsForZSet().rank(key, userId.toString()))
                .map(i -> i>=0 ? i+1 : i)
                ;
    }
}
