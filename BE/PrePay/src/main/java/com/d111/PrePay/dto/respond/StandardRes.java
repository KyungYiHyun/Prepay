package com.d111.PrePay.dto.respond;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StandardRes {
    private String message;
    private int statusCode;

    public StandardRes(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}
