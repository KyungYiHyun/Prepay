package com.d111.PrePay.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLoginReq {
    private String email;
    private String password;
}
