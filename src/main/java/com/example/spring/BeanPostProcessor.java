package com.example.spring;

public interface BeanPostProcessor {
    default Object beforeInitializationBean(Object bean, String beanName) {
        return bean;
    }
    default Object afterInitializationBean(Object bean, String beanName) {
        return bean;
    }
}
