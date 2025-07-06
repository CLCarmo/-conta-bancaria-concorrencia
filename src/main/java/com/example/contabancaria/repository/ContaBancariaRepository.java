// src/main/java/com/example/contabancaria/repository/ContaBancariaRepository.java
package com.example.contabancaria.repository;

import com.example.contabancaria.model.ContaBancaria; // Importa a classe ContaBancaria que criamos
import org.springframework.data.jpa.repository.JpaRepository; // Importa a interface JpaRepository
import org.springframework.stereotype.Repository; // Importa a anotação @Repository

// @Repository é uma anotação do Spring que indica que esta interface é um componente de acesso a dados.
// O Spring a gerenciará e encontrará automaticamente quando precisar.
@Repository
// ContaBancariaRepository estende JpaRepository.
// JpaRepository<TipoDaEntidade, TipoDoId> fornece métodos CRUD (Create, Read, Update, Delete) prontos.
// No nosso caso, é ContaBancaria e Long (o tipo do 'id' da nossa ContaBancaria).
public interface ContaBancariaRepository extends JpaRepository<ContaBancaria, Long> {
}