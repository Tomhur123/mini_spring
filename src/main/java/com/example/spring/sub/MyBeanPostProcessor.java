package com.example.spring.sub;

import com.example.spring.BeanPostProcessor;
import com.example.spring.Component;

@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object afterInitializationBean(Object bean, String beanName) {
        System.out.println(beanName+"初始化完成");
        return bean;
    }
}
