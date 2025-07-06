package com.example.contabancaria.service; // PACOTE DO SERVIÇO AJUSTADO AQUI

import com.example.contabancaria.model.ContaBancaria; // IMPORT AJUSTADO PARA SEU PACOTE DA ENTIDADE
import com.example.contabancaria.repository.ContaBancariaRepository; // IMPORT AJUSTADO PARA SEU PACOTE DO REPOSITÓRIO
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
public class ContaBancariaService {

    @Autowired
    private ContaBancariaRepository contaBancariaRepository;

    public ContaBancaria criarConta(ContaBancaria conta) {
        return contaBancariaRepository.save(conta);
    }

    public Optional<ContaBancaria> buscarContaPorId(Long id) {
        return contaBancariaRepository.findById(id);
    }

    public List<ContaBancaria> listarTodasContas() {
        return contaBancariaRepository.findAll();
    }

    @Transactional
    @Retryable(
            value = { ObjectOptimisticLockingFailureException.class, OptimisticLockException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 200)
    )
    public ContaBancaria depositar(Long id, float valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }
        ContaBancaria conta = contaBancariaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada."));

        conta.setSaldo(conta.getSaldo() + valor);

        return contaBancariaRepository.save(conta);
    }

    @Transactional
    @Retryable(
            value = { ObjectOptimisticLockingFailureException.class, OptimisticLockException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 200)
    )
    public ContaBancaria sacar(Long id, float valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("O valor do saque deve ser positivo.");
        }
        ContaBancaria conta = contaBancariaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada."));
        if (conta.getSaldo() < valor) {
            throw new RuntimeException("Saldo insuficiente.");
        }

        conta.setSaldo(conta.getSaldo() - valor);

        return contaBancariaRepository.save(conta);
    }

    @Transactional
    public void excluirConta(Long id) {
        if (!contaBancariaRepository.existsById(id)) {
            throw new RuntimeException("Conta não encontrada para exclusão.");
        }
        contaBancariaRepository.deleteById(id);
    }

    @Recover
    public ContaBancaria recover(ObjectOptimisticLockingFailureException e, Long id, float valor) {
        throw new RuntimeException("Falha ao realizar operação após múltiplas tentativas devido a concorrência: " + e.getMessage());
    }

    @Recover
    public ContaBancaria recover(OptimisticLockException e, Long id, float valor) {
        throw new RuntimeException("Falha ao realizar operação após múltiplas tentativas devido a concorrência: " + e.getMessage());
    }
}