package com.klu.spring.mvc.auth;

import javax.validation.constraints.NotBlank;

public class TokenRequest {

    @NotBlank(message = "token is required")
    private String token;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
