package br.imd;

import br.imd.gateway.ApiGateway;

public class Main {

    public static void main(String[] args) {
        ApiGateway gateway = new ApiGateway();
        gateway.start();
    }
}