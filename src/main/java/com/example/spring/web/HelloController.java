package com.example.spring.web;

import com.example.spring.Component;

@Controller
@Component
@RequestMapping("/hello")
public class HelloController {

    @RequestMapping("/a")
    public String Hello(@Param("name") String name, @Param("age") Integer age) {
        return String.format("<h1>hello world</h1><br/> name: %s, age: %s", name, age);
    }

    @RequestMapping("/json")
    @ResponseBody
    public User json(@Param("name") String name, @Param("age") Integer age) {
        return new User(name, age);
    }

    @RequestMapping("/html")
    public ModelAndView html(@Param("name") String name, @Param("age") Integer age) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setView("index.html");
        modelAndView.getContext().put("name", name);
        modelAndView.getContext().put("age", age.toString());
        return modelAndView;
    }



}
