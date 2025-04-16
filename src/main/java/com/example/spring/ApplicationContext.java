package com.example.spring;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationContext {

    private final Map<String,Object> ioc = new HashMap<>();
    private final Map<String,Object> loadingIoc = new HashMap<>();
    private final Map<String,BeanDefinition> beanDefinitionMap = new HashMap<>();
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    public ApplicationContext(String packageName) throws Exception {
        initContext(packageName);
        beanDefinitionMap.values().forEach(this::createBean);
    }

    public void initContext(String packageName) throws Exception {
        scanPackage(packageName).stream().filter(this::canCreate).forEach(this::wrapper);
        initBeanPostProcessor();
        beanDefinitionMap.values().forEach(this::createBean);
    }

    private void initBeanPostProcessor() {
        beanDefinitionMap.values().stream()
                .filter(bd -> BeanPostProcessor.class.isAssignableFrom(bd.getBeanType()))
                .map(this::createBean)
                .map(BeanPostProcessor.class::cast)
                .forEach(beanPostProcessors::add);
    }

    protected boolean canCreate(Class<?> type) {
        return type.isAnnotationPresent(Component.class);
    }

    protected Object createBean(BeanDefinition beanDefinition) {
        String name = beanDefinition.getName();
        if(ioc.containsKey(name)) {
            return ioc.get(name);
        }
        if(loadingIoc.containsKey(name)) {
            return loadingIoc.get(name);
        }
        return doCreateBean(beanDefinition);
    }

    private Object doCreateBean(BeanDefinition beanDefinition) {
        Constructor<?> constructor = beanDefinition.getConstructor();
        Object bean = null;
        try {
            bean = constructor.newInstance();
            loadingIoc.put(beanDefinition.getName(),bean);
            autowiredBean(bean,beanDefinition);
            bean = initializeBean(bean,beanDefinition);
            loadingIoc.remove(beanDefinition.getName());
            ioc.put(beanDefinition.getName(),bean);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bean;
    }

    private Object initializeBean(Object bean, BeanDefinition beanDefinition) throws Exception{
        for(BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            bean = beanPostProcessor.beforeInitializationBean(bean,beanDefinition.getName());
        }

        Method method = beanDefinition.getPostConstructMethod();
        if(method != null) {
            method.invoke(bean);
        }

        for(BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            bean = beanPostProcessor.afterInitializationBean(bean,beanDefinition.getName());
        }
        return bean;
    }

    private void autowiredBean(Object bean, BeanDefinition beanDefinition) throws IllegalAccessException {
        for(Field autowiredField : beanDefinition.getAutowiredFields()) {
            autowiredField.setAccessible(true);
            autowiredField.set(bean,getBean(autowiredField.getType()));
        }
    }

    protected BeanDefinition wrapper(Class<?> type) {
        BeanDefinition beanDefinition = new BeanDefinition(type);
        if(beanDefinitionMap.containsKey(beanDefinition.getName())) {
            throw new RuntimeException("bean的名字重复");
        }
        beanDefinitionMap.put(beanDefinition.getName(),beanDefinition);
        return beanDefinition;
    }

    private List<Class<?>> scanPackage(String packageName) throws Exception{
        //a.b.c
        List<Class<?>> classList = new ArrayList<>();
        URL resource = this.getClass().getClassLoader().getResource(packageName.replace(".", File.separator));
        File file = new File(resource.toURI());
        Path path = file.toPath();
        Files.walkFileTree(path,new SimpleFileVisitor<>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path absolutePath = file.toAbsolutePath();
                if(absolutePath.toString().endsWith(".class")) {
                    String replaceStr = absolutePath.toString().replace(File.separatorChar, '.');
                    int packageNameIndex = replaceStr.indexOf(packageName);
                    String className = replaceStr.substring(packageNameIndex, replaceStr.length() - ".class".length());
                    try {
                        classList.add(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return classList;
    }

    public Object getBean(String name) {
        if(name == null) return null;
        Object bean = this.ioc.get(name);
        if(bean != null) {
            return bean;
        }
        if(beanDefinitionMap.containsKey(name)) {
            return createBean(beanDefinitionMap.get(name));
        }
        return null;
    }

    public <T> T getBean(Class<T> beanType) {
        String beanName = this.beanDefinitionMap.values().stream()
                .filter(bd -> beanType.isAssignableFrom(bd.getBeanType()))
                .map(BeanDefinition::getName)
                .findFirst()
                .orElse(null);
        return (T)getBean(beanName);
    }

    public <T> List<T> getBeans(Class<T> beanType) {
        return this.beanDefinitionMap.values().stream()
                .filter(bd -> beanType.isAssignableFrom(bd.getBeanType()))
                .map(BeanDefinition::getName)
                .map(this::getBean)
                .map((bean) -> (T)bean)
                .toList();
    }

}
