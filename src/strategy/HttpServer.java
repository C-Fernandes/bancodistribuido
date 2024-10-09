package strategy;

public class HttpServer implements RequestHandler {
    @Override
    public void handleRequest(String request) {
        System.out.println("Handling HTTP request: " + request);

    }
}
