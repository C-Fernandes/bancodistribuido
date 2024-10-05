import java.io.Serializable;

public class MsgTCP implements Serializable {
    private int proc;
    private int mem;
    private int nclients;
    private String hostname;

    public int getProc() {
        return proc;
    }

    public void setProc(int proc) {
        this.proc = proc;
    }

    public int getMem() {
        return mem;
    }

    public void setMem(int mem) {
        this.mem = mem;
    }

    public int getNclients() {
        return nclients;
    }

    public void setNclients(int nclients) {
        this.nclients = nclients;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
