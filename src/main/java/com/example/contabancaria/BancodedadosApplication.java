package com.example.contabancaria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry; // Importe esta anotação

@SpringBootApplication
@EnableRetry // ESSA LINHA É CRÍTICA PARA O SPRING RETRY
public class BancodedadosApplication {

	public static void main(String[] args) {
		SpringApplication.run(BancodedadosApplication.class, args);
	}

}