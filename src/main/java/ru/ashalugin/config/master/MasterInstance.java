package ru.ashalugin.config.master;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class MasterInstance implements Condition {
    public static final String INSTANCE_CODE = "master";
    private static final String INSTANCE_TYPE = "application.mode";

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        return INSTANCE_CODE.equals(conditionContext.getEnvironment().getProperty(INSTANCE_TYPE));
    }
}