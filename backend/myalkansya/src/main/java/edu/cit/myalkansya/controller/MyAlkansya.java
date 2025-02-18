package edu.cit.myalkansya.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyAlkansya {
    @GetMapping("/")
    public String test() {
        return "Hello World";
    }
}
