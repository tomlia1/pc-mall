package com.pcmall.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationConfigurationTest {
    @Test
    void configuresMySqlPaginationForTheExistingProductPageParameters() throws Exception {
        Class<?> configurationClass = Class.forName("com.pcmall.config.MybatisPlusConfig");
        Object configuration = configurationClass.getConstructor().newInstance();
        Method method = configurationClass.getDeclaredMethod("mybatisPlusInterceptor");
        MybatisPlusInterceptor interceptor = (MybatisPlusInterceptor) method.invoke(configuration);

        assertTrue(interceptor.getInterceptors().stream().anyMatch(PaginationInnerInterceptor.class::isInstance));
    }
}
