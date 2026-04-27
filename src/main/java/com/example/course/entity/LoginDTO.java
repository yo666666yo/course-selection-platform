package com.example.course.entity;
import lombok.Data;

@Data
public class LoginDTO {
    private String username;
    private String password;
    private String role; // student, teacher, admin
}