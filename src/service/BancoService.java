
package service;

import repository.BancoRepository;
import entity.Banco;

public class BancoService {

    private BancoRepository bancoRepository;

    public void adicionarBanco(String nome) {
        bancoRepository.adicionarBanco(new Banco(nome));
    }

    public void findAll() {
        bancoRepository.findAll();
    }
}
