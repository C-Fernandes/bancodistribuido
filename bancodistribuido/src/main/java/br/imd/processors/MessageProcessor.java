package br.imd.processors;

public class MessageProcessor {

    /**
     * Processa a mensagem recebida, separando-a em partes.
     *
     * @param message A mensagem a ser processada.
     * @return Um array de strings contendo as partes da mensagem,
     *         ou null se a mensagem estiver malformada.
     */
    public String[] processMessage(String message) {
        // Verifica se a mensagem não é nula ou vazia
        if (message == null || message.isEmpty()) {
            return null; // Retorna nulo se a mensagem estiver vazia
        }

        // Separa a mensagem usando o delimitador '-'
        String[] parts = message.split("-");

        // Verifica se a mensagem contém pelo menos uma parte
        if (parts.length == 0) {
            return null; // Retorna nulo se não houver partes
        }

        return parts; // Retorna as partes da mensagem separadas
    }
}
