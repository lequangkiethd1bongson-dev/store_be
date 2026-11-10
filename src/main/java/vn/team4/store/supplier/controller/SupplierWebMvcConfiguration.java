package vn.team4.store.supplier.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SupplierWebMvcConfiguration implements WebMvcConfigurer {
    private final SupplierRequestLoggingInterceptor loggingInterceptor;

    public SupplierWebMvcConfiguration(SupplierRequestLoggingInterceptor loggingInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/suppliers", "/api/suppliers/**");
    }
}
