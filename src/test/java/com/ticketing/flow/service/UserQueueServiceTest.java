package com.ticketing.flow.service;

import com.ticketing.flow.EmbeddedRedis;
import com.ticketing.flow.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(EmbeddedRedis.class)
@ActiveProfiles("test")
class UserQueueServiceTest {
    public static final String DEFAULT_QUEUE = "default";
    @Autowired
    private UserQueueService userQueueService;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @BeforeEach
    public void beforeEach() {
        ReactiveRedisConnection reactiveConnection = reactiveRedisTemplate.getConnectionFactory().getReactiveConnection();
        // 테스트 시작 전 데이터 비우기
        reactiveConnection.serverCommands().flushAll().subscribe();
    }

    @Test
    void registerWaitQueue() {
        StepVerifier.create(userQueueService.registerWaitQueue(DEFAULT_QUEUE, 100L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue(DEFAULT_QUEUE, 101L))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue(DEFAULT_QUEUE, 102L))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void alreadyRegisterWaitQueue() {
        StepVerifier.create(userQueueService.registerWaitQueue(DEFAULT_QUEUE, 100L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue(DEFAULT_QUEUE, 100L))
                .expectError(ApplicationException.class)
                .verify();
    }

    @Test
    void emptyAllowUser() {
        StepVerifier.create(userQueueService.allowUser(DEFAULT_QUEUE, 3L))
                .expectNext(0L)
                .verifyComplete();

    }

    @Test
    void allowUser() {
        StepVerifier.create(
                        userQueueService.registerWaitQueue(DEFAULT_QUEUE, 100L)
                                .then(userQueueService.registerWaitQueue(DEFAULT_QUEUE, 101L))
                                .then(userQueueService.registerWaitQueue(DEFAULT_QUEUE, 102L))
                                .then(userQueueService.allowUser(DEFAULT_QUEUE, 2L))
                )
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void allowUserBiggerThanRegister() {
        StepVerifier.create(
                        userQueueService.registerWaitQueue(DEFAULT_QUEUE, 100L)
                                .then(userQueueService.registerWaitQueue(DEFAULT_QUEUE, 101L))
                                .then(userQueueService.registerWaitQueue(DEFAULT_QUEUE, 102L))
                                .then(userQueueService.allowUser(DEFAULT_QUEUE, 5L))
                )
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void allowUserAfterRegisterWaitQueue() {
        StepVerifier.create(
                        userQueueService.registerWaitQueue(DEFAULT_QUEUE, 100L)
                                .then(userQueueService.registerWaitQueue(DEFAULT_QUEUE, 101L))
                                .then(userQueueService.registerWaitQueue(DEFAULT_QUEUE, 102L))
                                .then(userQueueService.allowUser(DEFAULT_QUEUE, 3L))
                                .then(userQueueService.registerWaitQueue(DEFAULT_QUEUE, 200L))
                )
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void isNotAllowed() {
        StepVerifier.create(userQueueService.isAllowedUser(DEFAULT_QUEUE, 100L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void isNotAllowed2() {
        StepVerifier.create(
                        userQueueService.registerWaitQueue(DEFAULT_QUEUE, 100L)
                                .then(userQueueService.allowUser(DEFAULT_QUEUE, 3L))
                                .then(userQueueService.isAllowedUser(DEFAULT_QUEUE, 101L))
                )
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void isAllowed() {
        StepVerifier.create(
                        userQueueService.registerWaitQueue(DEFAULT_QUEUE, 100L)
                                .then(userQueueService.allowUser(DEFAULT_QUEUE, 3L))
                                .then(userQueueService.isAllowedUser(DEFAULT_QUEUE, 100L))
                )
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void getRank() {
        StepVerifier.create(
                        userQueueService.registerWaitQueue(DEFAULT_QUEUE, 100L)
                                .then(userQueueService.getRank(DEFAULT_QUEUE, 100L))
                ).expectNext(1L)
                .verifyComplete();

        StepVerifier.create(
                        userQueueService.registerWaitQueue(DEFAULT_QUEUE, 101L)
                                .then(userQueueService.getRank(DEFAULT_QUEUE, 101L))
                ).expectNext(2L)
                .verifyComplete();
    }

    @Test
    void emptyRank() {
        StepVerifier.create(userQueueService.getRank(DEFAULT_QUEUE, 100L))
                .expectNext(-1L)
                .verifyComplete();
    }
}