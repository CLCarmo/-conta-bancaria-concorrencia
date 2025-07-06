// src/main/java/com/example/contabancaria/controller/WebController.java
package com.example.contabancaria.controller;

import jakarta.annotation.PostConstruct; // Adicionar este import
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.slf4j.Logger; // Adicionar este import
import org.slf4j.LoggerFactory; // Adicionar este import

@Controller
public class WebController {

    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    @GetMapping("/")
    public String index() {
        logger.info("Requisição para '/' recebida. Tentando servir index.html"); // Este log aparecerá se o método for chamado
        return "index";
    }

    @PostConstruct // Este método será executado logo após o Spring inicializar o bean
    public void init() {
        logger.info("WebController foi inicializado pelo Spring.");
    }
}