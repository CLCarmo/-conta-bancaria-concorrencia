package com.example.contabancaria.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date; // Ou java.sql.Timestamp

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContaBancariaVersionada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nomeCliente;
    private float saldo;

    @Version // Anotação JPA para controle de concorrência otimista
    private Date dataMovimento; // Campo de versão do tipo Date ou Timestamp

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