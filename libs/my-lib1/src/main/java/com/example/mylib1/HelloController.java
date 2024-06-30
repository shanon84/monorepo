package com.example.mylib1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HelloController {
  private final HelloService helloService;

  @GetMapping("/hello")
  public ResponseEntity<String> getHello() {
    return ResponseEntity.ok(helloService.message());
  }
}
