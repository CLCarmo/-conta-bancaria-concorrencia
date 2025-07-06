// src/test/java/com/example/contabancaria/ContaBancariaVersionadaConcurrencyTest.java
package com.example.contabancaria;

import com.example.contabancaria.model.ContaBancariaVersionada;
import com.example.contabancaria.service.ContaBancariaVersionadaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test") // Garante que o perfil de teste seja ativado, usando H2 em memória
public class ContaBancariaVersionadaConcurrencyTest {

    @Autowired
    private ContaBancariaVersionadaService contaBancariaVersionadaService;

    private Long contaId;

    @BeforeEach
    void setup() {
        // Inicializa uma nova conta antes de cada teste
        ContaBancariaVersionada novaConta = new ContaBancariaVersionada(null, "Cliente Teste Vers.", 5.0f, null);
        ContaBancariaVersionada contaSalva = contaBancariaVersionadaService.criarConta(novaConta);
        contaId = contaSalva.getId();
        System.out.println("Conta inicializada para teste com ID: " + contaId + " e saldo: " + contaSalva.getSaldo());
    }

    @Test
    void shouldHandleConcurrentWithdrawalsWithVersioning() throws InterruptedException {
        int numberOfThreads = 10;
        float withdrawalAmount = 1.0f;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successfulWithdrawals = new AtomicInteger(0);
        AtomicInteger failedWithdrawals = new AtomicInteger(0);

        System.out.println("Iniciando teste de concorrência com " + numberOfThreads + " threads...");

        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                try {
                    contaBancariaVersionadaService.sacar(contaId, withdrawalAmount);
                    successfulWithdrawals.incrementAndGet();
                } catch (RuntimeException e) {
                    // Espera-se "Saldo insuficiente." ou "Falha ao realizar operação..."
                    // A mensagem de "Falha ao realizar operação..." indica que o retry foi exaurido
                    failedWithdrawals.incrementAndGet();
                    // System.err.println("Erro na thread: " + Thread.currentThread().getName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS); // Espera por todas as threads
        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);

        ContaBancariaVersionada contaAtualizada = contaBancariaVersionadaService.buscarContaPorId(contaId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada após testes."));

        System.out.println("----------------------------------------");
        System.out.println("Saques bem-sucedidos: " + successfulWithdrawals.get());
        System.out.println("Saques falhos (saldo insuficiente ou concorrência): " + failedWithdrawals.get());
        System.out.println("Saldo final da conta: " + contaAtualizada.getSaldo());
        System.out.println("----------------------------------------");

        // Asserts
        // Com 5.0 de saldo inicial e 10 saques de 1.0, esperamos 5 sucessos e 5 falhas
        assertEquals(5, successfulWithdrawals.get(), "O número de saques bem-sucedidos deve ser 5.");
        assertEquals(0.0f, contaAtualizada.getSaldo(), 0.001f, "O saldo final deve ser 0.0.");
    }
}