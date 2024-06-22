package com.example.mylib3;

import org.springframework.stereotype.Service;

@Service
public class HelloService {

    public String message() {
        return "Hello World!";
    }
}
