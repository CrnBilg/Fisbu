package com.fisbu.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // API sadece native mobil istemci (Flutter) tarafından tüketiliyor — tarayıcı
    // tabanlı bir frontend yok. CORS yalnızca tarayıcıları etkilediği için native
    // istemci hiçbir zaman buradan kısıtlanmaz; wildcard origin sadece gereksiz bir
    // saldırı yüzeyi bırakıyordu, bu yüzden hiçbir origin'e izin verilmiyor.
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins()
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}