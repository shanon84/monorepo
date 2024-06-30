package com.example.mylib1;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HelloService {

  public String message() {
    return "Hello World!";
  }
}
