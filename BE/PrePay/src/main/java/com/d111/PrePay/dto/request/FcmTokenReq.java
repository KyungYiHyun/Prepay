package com.d111.PrePay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
public class FcmTokenReq {
    @Schema(description = "이메일", example = "test1")
    String email;
    @Schema(description = "토큰", example = "android_token")
    String token;

    public FcmTokenReq(String email, String token) {
        this.email = email;
        this.token = token;
    }
}
