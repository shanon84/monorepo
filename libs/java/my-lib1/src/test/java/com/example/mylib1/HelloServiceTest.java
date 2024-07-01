package com.example.mylib1;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HelloServiceTest {
  HelloService helloService = new HelloService();

  @Test
  void shouldPrintOut() {
    assertThat(helloService.message()).contains("Hello World");
  }
}
