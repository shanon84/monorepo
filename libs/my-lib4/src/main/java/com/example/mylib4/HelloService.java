package com.example.mylib4;

import org.springframework.stereotype.Service;

@Service
public class HelloService {

    public String message() {
        return "Hello World!";
    }
}
