package com.example.spring.sub;

import com.example.spring.Autowired;
import com.example.spring.Component;
import com.example.spring.PostConstruct;

@Component
public class Dog {
    @Autowired
    private Cat cat;

    @Autowired
    private Dog dog;

    @PostConstruct
    public void init() {
        System.out.println("Dog 创建完成了 里面有一只猫"+cat);
        System.out.println("Dog 创建完成了 里面有一只狗" + dog);
    }
}
