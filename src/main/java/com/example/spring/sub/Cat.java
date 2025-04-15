package com.example.spring.sub;

import com.example.spring.Autowired;
import com.example.spring.Component;
import com.example.spring.PostConstruct;


@Component
public class Cat {
    @Autowired
    Dog dog;

    @PostConstruct
    public void init() {
        System.out.println("cat 创建完成完成了 里面有一只狗"+dog);
    }

}
