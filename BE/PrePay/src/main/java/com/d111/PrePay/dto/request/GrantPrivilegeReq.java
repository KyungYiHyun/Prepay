package com.d111.PrePay.dto.request;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GrantPrivilegeReq {
    private String changeUserEmail;
    private Long teamId;
    private boolean privilege;

}
