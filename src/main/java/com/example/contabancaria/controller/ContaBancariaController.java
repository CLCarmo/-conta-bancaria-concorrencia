package com.example.contabancaria.controller; // PACOTE DO CONTROLLER AJUSTADO AQUI

import com.example.contabancaria.model.ContaBancaria; // IMPORT AJUSTADO PARA SEU PACOTE DA ENTIDADE
import com.example.contabancaria.service.ContaBancariaService; // IMPORT AJUSTADO PARA SEU PACOTE DO SERVIÇO
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/contas")
public class ContaBancariaController {

    @Autowired
    private ContaBancariaService contaBancariaService;

    @PostMapping
    public ResponseEntity<ContaBancaria> criarConta(@RequestBody ContaBancaria conta) {
        ContaBancaria novaConta = contaBancariaService.criarConta(conta);
        return new ResponseEntity<>(novaConta, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaBancaria> buscarContaPorId(@PathVariable Long id) {
        Optional<ContaBancaria> conta = contaBancariaService.buscarContaPorId(id);
        return conta.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<ContaBancaria>> listarTodasContas() {
        List<ContaBancaria> contas = contaBancariaService.listarTodasContas();
        return new ResponseEntity<>(contas, HttpStatus.OK);
    }

    @PostMapping("/{id}/depositar")
    public ResponseEntity depositar(@PathVariable Long id, @RequestParam float valor) {
        try {
            ContaBancaria contaAtualizada = contaBancariaService.depositar(id, valor);
            return new ResponseEntity<>(contaAtualizada, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{id}/sacar")
    public ResponseEntity sacar(@PathVariable Long id, @RequestParam float valor) {
        try {
            ContaBancaria contaAtualizada = contaBancariaService.sacar(id, valor);
            return new ResponseEntity<>(contaAtualizada, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirConta(@PathVariable Long id) {
        try {
            contaBancariaService.excluirConta(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}