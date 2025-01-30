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
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private static final String USER_QUEUE_WAIT_KEY = "users:queue:%s:wait";
    private static final String USER_QUEUE_PROCEED_KEY = "users:queue:%s:proceed";

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

    // 진입 허용
    public Mono<Long> allowUser(final String queue, final Long count){
        var unixTimeStamp = Instant.now().getEpochSecond();
        String waitQueueKey = USER_QUEUE_WAIT_KEY.formatted(queue);
        String proceedQueueKey = USER_QUEUE_PROCEED_KEY.formatted(queue);

        // wait Queue에서 value가 작은 값 제거하여 proceed Queue에 추가
        return reactiveRedisTemplate.opsForZSet().popMin(waitQueueKey, count)
                .flatMap(member -> reactiveRedisTemplate.opsForZSet().add(proceedQueueKey, member.getValue(), unixTimeStamp))
                .filter(i -> i == true)
                .count()
        ;
    }

    // 진입이 가능한 상태인지 조회
    public Mono<Boolean> isAllowedUser(final String queue, final Long userId){
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_PROCEED_KEY.formatted(queue), userId.toString())
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0 );
    }

    public Mono<Long> getRank(final String queue, final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString())
                .defaultIfEmpty(-1L)
                .map( rank -> rank >= 0 ? rank + 1: rank);
    }
}
