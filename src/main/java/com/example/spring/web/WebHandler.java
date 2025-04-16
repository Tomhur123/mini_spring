package com.example.spring.web;

import com.example.spring.Component;

import java.lang.reflect.Method;

public class WebHandler {

    private final Object controllerBean;
    private final Method method;
    private final ResultType resultType;

    public WebHandler(Object controllerBean, Method method) {
        this.controllerBean = controllerBean;
        this.method = method;
        this.resultType = resolveResultType(controllerBean,method);
    }

    private ResultType resolveResultType(Object controllerBean, Method method) {
        if(method.isAnnotationPresent(ResponseBody.class)) {
            return ResultType.JSON;
        }
        if(method.getReturnType() == ModelAndView.class) {
            return ResultType.LOCAL;
        }
        return ResultType.HTML;
    }

    public Object getControllerBean() {
        return controllerBean;
    }

    public Method getMethod() {
        return method;
    }

    public ResultType getResultType() {
        return resultType;
    }

    enum ResultType {
        JSON, HTML, LOCAL
    }
}
