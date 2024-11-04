package br.imd;

import br.imd.gateway.ApiGateway;
import br.imd.service.TransferenciaService;

public class Main {

    public static void main(String[] args) {
        TransferenciaService transferencia = new TransferenciaService();
        ApiGateway gateway = new ApiGateway();
        gateway.start();
    }
}