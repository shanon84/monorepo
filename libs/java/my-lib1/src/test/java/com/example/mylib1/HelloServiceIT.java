package com.example.mylib1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
public class HelloServiceIT {

  @Autowired
  private HelloService helloService;

  @Test
  public void shouldReturnHelloWorld() {
    assertThat(helloService.message()).contains("Hello World");
  }

}
