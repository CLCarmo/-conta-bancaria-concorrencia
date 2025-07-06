// src/main/java/com/example/contabancaria/model/ContaBancaria.java
package com.example.contabancaria.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Entity; // Removido duplicata
import jakarta.persistence.Id;
import jakarta.persistence.Version; // Importar o @Version

import lombok.Data;          // Para getters, setters, toString, equals, hashCode
import lombok.NoArgsConstructor; // Para construtor sem argumentos
import lombok.AllArgsConstructor; // Para construtor com todos os argumentos

@Entity
@Data                 // Anotação Lombok para gerar Getters, Setters, toString(), equals(), hashCode()
@NoArgsConstructor    // Anotação Lombok para gerar um construtor sem argumentos
@AllArgsConstructor   // Anotação Lombok para gerar um construtor com todos os argumentos
public class ContaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nomeCliente;
    private float saldo;

    @Version // Anotação JPA para controle de concorrência otimista
    private Long version; // Campo que o Hibernate usará para controle de versão

    // Método para depósito
    public void deposita(float valor) {
        if (valor > 0) {
            this.saldo += valor;
        } else {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }
    }

    // Método para retirada (saque)
    public void retirada(float valor) {
        if (valor > 0) {
            if (this.saldo >= valor) {
                this.saldo -= valor;
            } else {
                throw new IllegalArgumentException("Saldo insuficiente para a retirada.");
            }
        } else {
            throw new IllegalArgumentException("O valor da retirada deve ser positivo.");
        }
    }
}