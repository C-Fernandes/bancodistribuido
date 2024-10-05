import java.util.ArrayList;
import java.util.List;

public class Banco {
    private int id;  // Identificador único do banco
    private String nome;  // Nome do banco
    private List<Conta> contas;

    public Banco(String nome) {
        this.contas = new ArrayList<>();
    }

    public Banco() {this.contas = new ArrayList<>();}

    public void adicionarConta(String agencia, String conta) {
        contas.add(new Conta(agencia, conta));
    }

    public Conta buscarConta(String agencia, String conta) {
        for (Conta c : contas) {
            if (c.getConta().equals(conta) && c.getAgencia().equals(agencia)) {
                return c;
            }
        }
        return null;
    }

    public List<Conta> listraContasPorAgencia(String agencia) {
        List<Conta> contasAgencia = new ArrayList<>();
        for (Conta c : contas) {
            if (c.getAgencia().equals(agencia)) {contasAgencia.add(c);}
        }
        return contasAgencia;
    }

    public void deletarConta(String conta, String agencia) {

    }


    public String getNome() {return nome;}

    public void setNome(String nome) {this.nome = nome;}

    public List<Conta> getContas() {return contas;}

    public void setContas(List<Conta> contas) {this.contas = contas;}
}
