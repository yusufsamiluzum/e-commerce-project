package com.ecommerce.config; // Paket adını kendi yapınıza göre ayarlayın

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    // application.properties'ten değeri oku
    @Value("${image.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/product-images/**")
                .addResourceLocations("file:" + uploadDir);

        // İsteğe bağlı: Diğer statik kaynaklar için (CSS, JS vs. classpath'ten)
         registry.addResourceHandler("/static/**")
                 .addResourceLocations("classpath:/static/");
    }
}