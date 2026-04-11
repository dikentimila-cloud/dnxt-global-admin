package com.dnxt.globaladmin.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoginResponse {

    private String token;
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private List<String> permissions;
    private boolean mustChangePassword;
}
