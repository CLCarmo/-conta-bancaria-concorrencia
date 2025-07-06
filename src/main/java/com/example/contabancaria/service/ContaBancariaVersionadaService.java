// src/main/java/com/example/contabancaria/service/ContaBancariaVersionadaService.java
package com.example.contabancaria.service;

import com.example.contabancaria.model.ContaBancariaVersionada;
import com.example.contabancaria.repository.ContaBancariaVersionadaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import jakarta.persistence.OptimisticLockException;

import java.util.List;
import java.util.Optional;

@Service
public class ContaBancariaVersionadaService {

    @Autowired
    private ContaBancariaVersionadaRepository contaBancariaVersionadaRepository;

    public ContaBancariaVersionada criarConta(ContaBancariaVersionada conta) {
        return contaBancariaVersionadaRepository.save(conta);
    }

    public Optional<ContaBancariaVersionada> buscarContaPorId(Long id) {
        return contaBancariaVersionadaRepository.findById(id);
    }

    public List<ContaBancariaVersionada> listarTodasContas() {
        return contaBancariaVersionadaRepository.findAll();
    }

    @Transactional
    @Retryable(
            value = { ObjectOptimisticLockingFailureException.class, OptimisticLockException.class },
            maxAttempts = 5, // Mantemos a mesma configuração de retentativas que funcionou
            backoff = @Backoff(delay = 200)
    )
    public ContaBancariaVersionada depositar(Long id, float valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }
        ContaBancariaVersionada conta = contaBancariaVersionadaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada."));

        conta.setSaldo(conta.getSaldo() + valor);

        return contaBancariaVersionadaRepository.save(conta);
    }

    @Transactional
    @Retryable(
            value = { ObjectOptimisticLockingFailureException.class, OptimisticLockException.class },
            maxAttempts = 5, // Mantemos a mesma configuração de retentativas que funcionou
            backoff = @Backoff(delay = 200)
    )
    public ContaBancariaVersionada sacar(Long id, float valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("O valor do saque deve ser positivo.");
        }
        ContaBancariaVersionada conta = contaBancariaVersionadaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada."));
        if (conta.getSaldo() < valor) {
            throw new RuntimeException("Saldo insuficiente.");
        }

        conta.setSaldo(conta.getSaldo() - valor);

        return contaBancariaVersionadaRepository.save(conta);
    }

    @Transactional
    public void excluirConta(Long id) {
        if (!contaBancariaVersionadaRepository.existsById(id)) {
            throw new RuntimeException("Conta não encontrada para exclusão.");
        }
        contaBancariaVersionadaRepository.deleteById(id);
    }

    // Métodos @Recover para lidar com falhas após todas as retentativas
    @Recover
    public ContaBancariaVersionada recover(ObjectOptimisticLockingFailureException e, Long id, float valor) {
        throw new RuntimeException("Falha ao realizar operação após múltiplas tentativas devido a concorrência: " + e.getMessage());
    }

    @Recover
    public ContaBancariaVersionada recover(OptimisticLockException e, Long id, float valor) {
        throw new RuntimeException("Falha ao realizar operação após múltiplas tentativas devido a concorrência: " + e.getMessage());
    }
}