package com.ticketing.flow.controller;

import com.ticketing.flow.dto.AllowUserResponse;
import com.ticketing.flow.dto.AllowedUserResponse;
import com.ticketing.flow.dto.RankNumberResponse;
import com.ticketing.flow.dto.RegisterUserResponse;
import com.ticketing.flow.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class UserQueueController {
    private final UserQueueService userQueueService;

    // 대기열 등록
    @PostMapping("")
    public Mono<RegisterUserResponse> registerUser(@RequestParam(name = "queue", defaultValue = "default") String queue,
                                                   @RequestParam(name = "user_id") Long userId){
        return userQueueService.registerWaitQueue(queue, userId)
                .map(RegisterUserResponse::new);
    }

    // 진입 허용
    @PostMapping("/allow")
    public Mono<AllowUserResponse> allowUser(@RequestParam(name = "queue", defaultValue = "default") String queue,
                                             @RequestParam(name = "count") Long count){
        return userQueueService.allowUser(queue, count)
                .map(allowdCount -> new AllowUserResponse(count, allowdCount));
    }

    // 진입 가능한 상태인지 조회
    @GetMapping("/allowed")
    public Mono<AllowedUserResponse> isAllowedUser(@RequestParam(name = "queue", defaultValue = "default") String queue,
                                 @RequestParam(name = "user_id") Long userId){
        return userQueueService.isAllowedUser(queue, userId)
                .map(AllowedUserResponse::new);
    }

    @GetMapping("/rank")
    public Mono<RankNumberResponse> getRank(@RequestParam(name = "queue", defaultValue = "default") String queue,
                                                   @RequestParam(name = "user_id") Long userId){
        return userQueueService.getRank(queue, userId)
                .map(RankNumberResponse::new);
    }
}
