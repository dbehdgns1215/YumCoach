package com.ssafy.yumcoach.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailSendDto (
        @NotBlank
        @Email
        String email
) {}