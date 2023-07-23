package core.http;

import java.io.IOException;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import core.Configuration;
import core.Robot;
import core.dto.Command;
import core.dto.Operation;
import util.HttpHelper;

@SuppressWarnings("restriction")
public class AjaxHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params = HttpHelper.parsePostParams(exchange);
        String commandName = params.get("command");

        Command command = Configuration.getCommand(commandName);
        System.out.println("Executing command: " + command.getName());

        for (Operation operation : command.getOperations()) {
            Robot.processOperation(operation);
        }
    }
}
