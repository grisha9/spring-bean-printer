package org.example.bean.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationHook;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.MethodMetadata;

import java.util.*;
import java.util.function.Supplier;

public class BeanDefinitionPrinter {

    public static final String DEFINITION_EXCEPTION = "I am print bean definition exception";

    public static void main(String[] args) throws ClassNotFoundException {
        Class<?> applicationClass = Class.forName(System.getenv("org.example.spring.appClassName"));
        SpringApplication springApplication = new SpringApplication(applicationClass);
        springApplication.setApplicationStartup(new SampleApplicationStartup());
        SpringApplicationHook applicationHook = application -> new SampleFailedSpringApplicationRunListener();
        SpringApplication.withHook(applicationHook, () -> springApplication.run(args));
    }

    private static class SampleFailedSpringApplicationRunListener implements SpringApplicationRunListener {
        @Override
        public void failed(ConfigurableApplicationContext context, Throwable exception) {
            if (!DEFINITION_EXCEPTION.equals(exception.getMessage())) return;
            printBeans(context);
        }
    }

    private static void printBeans(ConfigurableApplicationContext context) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        String[] definitionNames = beanFactory.getBeanDefinitionNames();

        for (String beanName : definitionNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            BeanInfo beanInfo = getBeanInfo(beanDefinition, beanName);
            System.out.println(beanInfo);
        }
    }

    private static BeanInfo getBeanInfo(BeanDefinition definition, String definitionName) {
        BeanInfo beanInfo = new BeanInfo();
        beanInfo.primary = definition.isPrimary();
        beanInfo.className = definition.getBeanClassName();
        beanInfo.beanName = definitionName;
        beanInfo.scope = Optional.ofNullable(definition.getScope()).filter(it -> !it.isEmpty()).orElse("singleton");

        String beanClassName = definition.getBeanClassName();
        String factoryBeanObjectType = getFactoryBeanObjectType(definition);

        if (factoryBeanObjectType != null) {
            beanInfo.className = factoryBeanObjectType;
        } else if (beanClassName != null && definition instanceof AnnotatedBeanDefinition) {
            Optional<AnnotationMetadata> metadata = Optional.of(((AnnotatedBeanDefinition) definition).getMetadata());
            beanInfo.className = metadata.map(ClassMetadata::getClassName).orElse(beanClassName);
        } else if (definition instanceof AnnotatedBeanDefinition
                && ((AnnotatedBeanDefinition) definition).getFactoryMethodMetadata() != null) {
            MethodMetadata methodMetadata = ((AnnotatedBeanDefinition) definition).getFactoryMethodMetadata();
            beanInfo.className = methodMetadata.getDeclaringClassName();
            beanInfo.methodName = methodMetadata.getMethodName();
            beanInfo.methodReturnTypeName = methodMetadata.getReturnTypeName();
        }
        return beanInfo;
    }

    private static String getFactoryBeanObjectType(BeanDefinition definition) {
        Object factoryBeanObjectType = definition.getAttribute("factoryBeanObjectType");
        if (factoryBeanObjectType instanceof String) {
            return (String) factoryBeanObjectType;
        } else if (factoryBeanObjectType instanceof Class) {
            return ((Class<?>) factoryBeanObjectType).getName();
        }
        return null;
    }

    static class BeanInfo {
        public String beanName;
        public String className;
        public String methodName;
        public String methodReturnTypeName;
        public String scope;
        public boolean primary;

        @Override
        public String toString() {
            return "BeanInfo{" +
                    "beanName='" + beanName + '\'' +
                    ", className='" + className + '\'' +
                    ", methodName='" + methodName + '\'' +
                    ", methodReturnTypeName='" + methodReturnTypeName + '\'' +
                    ", scope='" + scope + '\'' +
                    ", primary=" + primary +
                    '}';
        }
    }

    private static class SampleApplicationStartup implements ApplicationStartup {

        @Override
        public SampleDefaultStartupStep start(String name) {
            return new SampleDefaultStartupStep(name);
        }

        class SampleDefaultStartupStep implements StartupStep {

            private final SampleDefaultStartupStep.DefaultTags TAGS = new DefaultTags();
            private String stepName;

            public SampleDefaultStartupStep(String name) {
                this.stepName = name;
            }

            @Override
            public String getName() {
                return "default";
            }

            @Override
            public long getId() {
                return 0L;
            }

            @Override
            public Long getParentId() {
                return null;
            }

            @Override
            public Tags getTags() {
                return this.TAGS;
            }

            @Override
            public StartupStep tag(String key, String value) {
                return this;
            }

            @Override
            public StartupStep tag(String key, Supplier<String> value) {
                return this;
            }

            @Override
            public void end() {
                if ("spring.context.beans.post-process".equalsIgnoreCase(stepName)) {
                    throw new RuntimeException(DEFINITION_EXCEPTION);
                }
            }

            class DefaultTags implements StartupStep.Tags {

                @Override
                public Iterator<Tag> iterator() {
                    return Collections.emptyIterator();
                }
            }
        }
    }
}
