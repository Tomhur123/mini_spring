package com.example.spring.web;

import com.alibaba.fastjson2.JSONObject;
import com.example.spring.BeanPostProcessor;
import com.example.spring.Component;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DispatcherServlet extends HttpServlet implements BeanPostProcessor {

    private static final Pattern PATTERN = Pattern.compile("\\$\\{(.*?)}");

    private final Map<String,WebHandler> HandlerMap = new HashMap<>();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        WebHandler handler = findHandler(req);
        if (handler == null) {
            resp.setContentType("text/html");
            resp.getWriter().write("<h1>Error！！！ 您的请求没有对应的处理器！</h1><br/> "+ req.getRequestURL().toString());
            return;
        }
        try {
            Object controllerBean = handler.getControllerBean();
            Object[] args = resolveArgs(req, handler.getMethod());
            Object result = handler.getMethod().invoke(controllerBean,args);
            switch (handler.getResultType()) {
                case HTML -> {
                    resp.setContentType("text/html");
                    resp.getWriter().write(result.toString());
                }
                case JSON -> {
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.getWriter().write(JSONObject.toJSONString(result));
                }
                case LOCAL -> {
                    ModelAndView mv = (ModelAndView) result;
                    String view = mv.getView();
                    InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(view);
                    try(resourceAsStream) {
                        String html = new String(resourceAsStream.readAllBytes());
                        html = renderTemplate(html, mv.getContext());
                        resp.setContentType("text/html;charset=UTF-8");
                        resp.getWriter().write(html);
                    }
                }
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

    private String renderTemplate(String template, Map<String, String> context) {
        Matcher matcher = PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = context.getOrDefault(key,"");
            matcher.appendReplacement(sb,Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Object[] resolveArgs(HttpServletRequest req, Method method) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String value = null;
            Param pram = parameter.getAnnotation(Param.class);
            if (pram != null) {
                value = req.getParameter(pram.value());
            } else {
                value = req.getParameter(parameter.getName());
            }
            Class<?> parameterType = parameter.getType();
            if(String.class.isAssignableFrom(parameterType)) {
                args[i] = value;
            } else if (Integer.class.isAssignableFrom(parameterType)) {
                args[i] = Integer.parseInt(value);
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    private WebHandler findHandler(HttpServletRequest req) {
        return HandlerMap.get(req.getRequestURI());
    }

    @Override
    public Object afterInitializationBean(Object bean, String beanName) {
        if (!bean.getClass().isAnnotationPresent(Controller.class)) {
            return bean;
        }
        RequestMapping classRm = bean.getClass().getAnnotation(RequestMapping.class);
        String classUrl = classRm == null?"":classRm.value();
        Arrays.stream(bean.getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(RequestMapping.class))
                .forEach(m -> {
                    RequestMapping methodRm = m.getAnnotation(RequestMapping.class);
                    String key = classUrl.concat(methodRm.value());
                    WebHandler handler = new WebHandler(bean,m);
                    if (HandlerMap.put(key,handler) != null) {
                        throw new RuntimeException("controller 定义重复"+key);
                    }
                });
        return bean;
    }
}
