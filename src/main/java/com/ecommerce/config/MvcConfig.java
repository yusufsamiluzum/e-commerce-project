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
        // Upload dizinini Path nesnesine çevir
        Path uploadPath = Paths.get(uploadDir);
        // Absolute path al
        String uploadAbsolutePath = uploadPath.toFile().getAbsolutePath();

        // Önemli: URL path'i ('/product-images/**') ile dosya sistemindeki yolu ('file:/path/to/your/uploads/') eşleştiriyoruz
        // 'file:' prefix'i ve sondaki '/' önemlidir.
        registry.addResourceHandler("/product-images/**") // Frontend bu path ile erişecek
                .addResourceLocations("file:" + uploadAbsolutePath + "/"); // Dosyaların bulunduğu gerçek yer

        // İsteğe bağlı: Diğer statik kaynaklar için (CSS, JS vs. classpath'ten)
         registry.addResourceHandler("/static/**")
                 .addResourceLocations("classpath:/static/");
    }
}