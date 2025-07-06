// src/main/java/com/example/contabancaria/repository/ContaBancariaVersionadaRepository.java
package com.example.contabancaria.repository;

import com.example.contabancaria.model.ContaBancariaVersionada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContaBancariaVersionadaRepository extends JpaRepository<ContaBancariaVersionada, Long> {
}