# Controle de Concorrência Otimista em Operações Bancárias com Spring Boot

Este projeto é uma aplicação Spring Boot que simula operações bancárias básicas (criação de conta, depósito e saque), com foco na demonstração e resolução de problemas de concorrência de dados. Ele explora o conceito de Controle de Concorrência Otimista utilizando a anotação `@Version` do JPA, tanto com um campo `Long` quanto com um campo `Date`, para garantir a integridade dos dados em um ambiente multi-threaded.

A aplicação também integra a biblioteca **Spring Retry** para implementar retentativas automáticas em operações que falham devido a conflitos de concorrência, aumentando a robustez do sistema.

### Tecnologias Utilizadas

- **Spring Boot**: Framework para desenvolvimento rápido de aplicações Java.
- **Spring Data JPA & Hibernate**: Para persistência de dados e mapeamento objeto-relacional.
- **H2 Database**: Banco de dados em memória para ambiente de desenvolvimento e testes.
- **Lombok**: Ferramenta para reduzir código boilerplate (getters, setters, construtores).
- **Spring Retry**: Para implementar retentativas programáticas em caso de falhas específicas (como `OptimisticLockException`).
- **Maven**: Ferramenta de automação de construção.
- **JUnit 5**: Framework para testes unitários.

### Estrutura do Projeto e Controle de Concorrência Otimista (`@Version`)

O projeto possui duas entidades principais para demonstrar o controle de concorrência:

1. **ContaBancaria** (com `@Version Long`)
   - Esta é a entidade original, onde o controle de concorrência otimista é implementado usando um campo `private Long version;` anotado com `@Version`.
   - Quando um registro é lido, sua versão é armazenada. Ao tentar atualizar o registro, o Hibernate verifica se a versão atual no banco de dados ainda corresponde à versão armazenada. Se não corresponder (indicando que outro processo modificou o registro), um `ObjectOptimisticLockingFailureException` é lançado.

2. **ContaBancariaVersionada** (com `@Version Date`)
   - Esta entidade foi criada especificamente para demonstrar que a anotação `@Version` também pode ser aplicada a um campo do tipo `java.util.Date` (ou `java.time.LocalDateTime`).
   - Nesse caso, o Hibernate não incrementa um número, mas sim atualiza o campo `dataMovimento` para o timestamp da transação. O mecanismo de detecção de conflito é o mesmo: se o timestamp lido não corresponder ao timestamp esperado durante a atualização, um conflito é detectado.

Ambas as entidades são gerenciadas por seus respectivos Repositórios (`ContaBancariaRepository`, `ContaBancariaVersionadaRepository`) e Serviços (`ContaBancariaService`, `ContaBancariaVersionadaService`).

### Tratamento de Retentativas com Spring Retry

Nos métodos de depósito e saque dentro dos serviços, a anotação `@Retryable` é utilizada. Isso permite que a aplicação tente novamente uma operação que falhou devido a um `ObjectOptimisticLockingFailureException` ou `OptimisticLockException` (erros de concorrência) por um número configurável de vezes. Isso aumenta a robustez e a resiliência da aplicação em cenários de alta concorrência, evitando que todas as operações conflitantes resultem em falha imediata para o usuário.

---

# Como Usar / Rodar a Aplicação

### Pré-requisitos

- Java Development Kit (JDK) 17 ou superior instalado.
- Maven instalado.
- (Opcional) Uma IDE como IntelliJ IDEA ou Eclipse.

### Clonar o Repositório

`git clone https://github.com/CLCarmo/-conta-bancaria-concorrencia.git`

`cd conta-bancaria-concorrencia/conta-bancaria`


### A aplicação será iniciada em:

[http://localhost:8080](http://localhost:8080)

---

### Acessar o H2 Console

Enquanto a aplicação estiver rodando, você pode acessar o console do banco de dados H2 em:  
[http://localhost:8080/h2-console](http://localhost:8080/h2-console)

- **JDBC URL:** `jdbc:h2:mem:contabancaria`  
- **User Name:** `sa`  
- **Password:** *(deixe em branco)*

Clique em **Connect**. Você poderá ver as tabelas `CONTA_BANCARIA` e `CONTA_BANCARIA_VERSIONADA` e seus dados.

---

## Como Testar a Concorrência

Os testes de concorrência são as peças-chave para demonstrar o funcionamento do `@Version` e do `@Retryable`.

### Abra o Projeto na sua IDE

Importe o projeto Maven para sua IDE preferida.

### Execute os Testes de Concorrência

Localize e execute as seguintes classes de teste no diretório:  
`src/test/java/com/example/contabancaria/`

- `ContaBancariaServiceConcurrencyTest.java`
  - Testa a entidade `ContaBancaria` (com `@Version Long`).
  - Simula **10 threads** tentando sacar de uma conta com saldo inicial de **5.0**.

- `ContaBancariaVersionadaConcurrencyTest.java`
  - Testa a entidade `ContaBancariaVersionada` (com `@Version Date`).
  - Simula **10 threads** tentando sacar de uma conta com saldo inicial de **5.0**.

---

### Analise a Saída no Console

Ao executar cada teste, observe os logs no console da sua IDE. Você deverá ver um resultado similar a este:

Saques bem-sucedidos: 5
Saques falhos (saldo insuficiente ou concorrência): 5
Saldo final da conta: 0.0


---

### Interpretação dos Resultados

- O saldo inicial de **5.0** e saques de **1.0** por **10 threads concorrentes** resultam em apenas **5 saques bem-sucedidos**.

- As **5 saques falhos** indicam que o sistema:
  - Teve o saldo esgotado (após os 5 saques bem-sucedidos).
  - Detectou e bloqueou concorrência otimista via `@Version`, onde tentativas de saque em registros desatualizados resultaram em `OptimisticLockException`.

- Graças ao `@Retryable`, essas operações de saque foram automaticamente **retentadas** até o limite configurado (por exemplo, **5 tentativas**).  
  Se mesmo após as retentativas o conflito persistiu ou o saldo já era insuficiente, a operação falhou definitivamente.

---

O **saldo final de 0.0** confirma que o bloqueio otimista funcionou efetivamente, permitindo que apenas um número consistente de operações fosse concluído com sucesso.

Este projeto demonstra que tanto o campo `Long` quanto o `Date` no `@Version` são eficazes em detectar e gerenciar concorrência otimista, com o **Spring Retry** adicionando uma camada extra de resiliência ao processo.

