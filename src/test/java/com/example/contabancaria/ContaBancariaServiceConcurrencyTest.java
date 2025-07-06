package com.example.contabancaria;

import com.example.contabancaria.model.ContaBancaria;
import com.example.contabancaria.repository.ContaBancariaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import com.example.contabancaria.service.ContaBancariaService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test") // Garante que você pode ter configurações de teste específicas, se precisar
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD) // Reseta o contexto do Spring para cada teste
public class ContaBancariaServiceConcurrencyTest {

	@Autowired
	private ContaBancariaService contaBancariaService;

	@Autowired
	private ContaBancariaRepository contaBancariaRepository;

	private Long contaId;

	@BeforeEach
	void setup() {
		// Limpa o banco de dados antes de cada teste (útil para testes em memória H2)
		contaBancariaRepository.deleteAll();

		// Cria uma conta inicial para os testes
		ContaBancaria conta = new ContaBancaria(null, "Cliente Concorrente", 100.0f, null);
		contaBancariaRepository.save(conta);
		this.contaId = conta.getId();
	}

	@Test
	void shouldHandleConcurrentDepositsCorrectly() throws InterruptedException {
		int numberOfThreads = 100;
		float depositAmount = 1.0f;
		ExecutorService service = Executors.newFixedThreadPool(10); // Pool de 10 threads
		CountDownLatch latch = new CountDownLatch(numberOfThreads); // Para esperar todas as threads terminarem
		AtomicInteger successfulDeposits = new AtomicInteger(0);
		AtomicInteger failedDeposits = new AtomicInteger(0);

		for (int i = 0; i < numberOfThreads; i++) {
			service.execute(() -> {
				try {
					// Tenta depositar na conta
					contaBancariaService.depositar(contaId, depositAmount);
					successfulDeposits.incrementAndGet();
				} catch (RuntimeException e) {
					// Captura exceções como a de concorrência (Recover)
					failedDeposits.incrementAndGet();
					System.out.println("Depósito falhou: " + e.getMessage());
				} finally {
					latch.countDown(); // Decrementa o contador do latch
				}
			});
		}

		latch.await(30, TimeUnit.SECONDS); // Espera todas as threads terminarem (com timeout)
		service.shutdown();
		service.awaitTermination(1, TimeUnit.MINUTES);

		// Verifica o saldo final. Deve ser o saldo inicial + (depósitos bem-sucedidos * valor do depósito)
		ContaBancaria finalConta = contaBancariaRepository.findById(contaId).orElseThrow();
		float expectedBalance = 100.0f + (successfulDeposits.get() * depositAmount);

		System.out.println("Saldo inicial: 100.0");
		System.out.println("Total de depósitos tentados: " + numberOfThreads);
		System.out.println("Depósitos bem-sucedidos: " + successfulDeposits.get());
		System.out.println("Depósitos falhos (concorrência ou outros): " + failedDeposits.get());
		System.out.println("Saldo final esperado: " + expectedBalance);
		System.out.println("Saldo final real: " + finalConta.getSaldo());

		// O saldo final deve ser exatamente o esperado, pois o Spring Retry deve garantir que a maioria (ou todos)
		// dos depósitos sejam processados, mesmo com retries.
		// Se houverem falhas irrecuperáveis (após o retry), o saldo será o resultado dos sucessos.
		// É importante que o saldo final corresponda aos depósitos *realmente efetuados*.
		assertEquals(expectedBalance, finalConta.getSaldo(), 0.001f, "O saldo final não corresponde ao esperado após depósitos concorrentes.");
	}

	@Test
	void shouldHandleConcurrentWithdrawalsCorrectly() throws InterruptedException {
		// Saldo inicial que permite múltiplos saques
		ContaBancaria initialConta = contaBancariaRepository.findById(contaId).orElseThrow();
		initialConta.setSaldo(500.0f); // Aumenta o saldo para testar saques
		contaBancariaRepository.save(initialConta);

		int numberOfThreads = 100;
		float withdrawalAmount = 2.0f; // Valor de cada saque
		ExecutorService service = Executors.newFixedThreadPool(10);
		CountDownLatch latch = new CountDownLatch(numberOfThreads);
		AtomicInteger successfulWithdrawals = new AtomicInteger(0);
		AtomicInteger failedWithdrawals = new AtomicInteger(0);

		for (int i = 0; i < numberOfThreads; i++) {
			service.execute(() -> {
				try {
					// Tenta sacar da conta
					contaBancariaService.sacar(contaId, withdrawalAmount);
					successfulWithdrawals.incrementAndGet();
				} catch (RuntimeException e) {
					// Captura exceções como a de concorrência ou saldo insuficiente
					failedWithdrawals.incrementAndGet();
					System.out.println("Saque falhou: " + e.getMessage());
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await(30, TimeUnit.SECONDS);
		service.shutdown();
		service.awaitTermination(1, TimeUnit.MINUTES);

		ContaBancaria finalConta = contaBancariaRepository.findById(contaId).orElseThrow();
		float expectedBalance = 500.0f - (successfulWithdrawals.get() * withdrawalAmount);

		System.out.println("Saldo inicial: 500.0");
		System.out.println("Total de saques tentados: " + numberOfThreads);
		System.out.println("Saques bem-sucedidos: " + successfulWithdrawals.get());
		System.out.println("Saques falhos (concorrência, saldo insuficiente ou outros): " + failedWithdrawals.get());
		System.out.println("Saldo final esperado: " + expectedBalance);
		System.out.println("Saldo final real: " + finalConta.getSaldo());

		assertEquals(expectedBalance, finalConta.getSaldo(), 0.001f, "O saldo final não corresponde ao esperado após saques concorrentes.");
	}

	@Test
	void shouldNotAllowWithdrawalPastZeroConcurrently() throws InterruptedException {
		// Inicia com um saldo baixo para forçar falhas por saldo insuficiente
		ContaBancaria initialConta = contaBancariaRepository.findById(contaId).orElseThrow();
		initialConta.setSaldo(5.0f);
		contaBancariaRepository.save(initialConta);

		int numberOfThreads = 10;
		float withdrawalAmount = 1.0f; // Saques de 1.0
		ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
		CountDownLatch latch = new CountDownLatch(numberOfThreads);
		AtomicInteger successfulWithdrawals = new AtomicInteger(0);
		AtomicInteger failedWithdrawals = new AtomicInteger(0);

		for (int i = 0; i < numberOfThreads; i++) {
			service.execute(() -> {
				try {
					contaBancariaService.sacar(contaId, withdrawalAmount);
					successfulWithdrawals.incrementAndGet();
				} catch (RuntimeException e) {
					failedWithdrawals.incrementAndGet();
					// Esperamos que muitos falhem por "Saldo insuficiente."
					// System.out.println("Saque falhou: " + e.getMessage());
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await(30, TimeUnit.SECONDS);
		service.shutdown();
		service.awaitTermination(1, TimeUnit.MINUTES);

		ContaBancaria finalConta = contaBancariaRepository.findById(contaId).orElseThrow();
		System.out.println("Saldo inicial: 5.0");
		System.out.println("Total de saques tentados: " + numberOfThreads);
		System.out.println("Saques bem-sucedidos: " + successfulWithdrawals.get());
		System.out.println("Saques falhos (saldo insuficiente): " + failedWithdrawals.get());
		System.out.println("Saldo final real: " + finalConta.getSaldo());

		// Em 10 tentativas de saque de 1.0 a partir de 5.0, esperamos 5 saques bem-sucedidos
		assertEquals(5.0f - (successfulWithdrawals.get() * withdrawalAmount), finalConta.getSaldo(), 0.001f);
		assertEquals(5, successfulWithdrawals.get(), "Deveriam ter sido 5 saques bem-sucedidos.");
		assertEquals(5, failedWithdrawals.get(), "Deveriam ter sido 5 saques falhos por saldo insuficiente.");
		assertEquals(0.0f, finalConta.getSaldo(), 0.001f, "O saldo final deveria ser zero.");
	}
}