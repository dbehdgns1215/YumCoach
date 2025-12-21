package com.ssafy.yumcoach.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer id;
    private String email;
    private String password;
    private String name;
    private String phone;
    private String gender;
    private Integer age;
    private String role;
    private String createAt;
    private String updateAt;
}
