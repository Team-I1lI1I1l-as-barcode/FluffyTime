package com.fluffytime.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder

// 프로필 수정 등 요청 결과를 담아 보내는 응답 DTO
public class RequestResultResponse {

    private Boolean result; // 요청 결과 여부
}
