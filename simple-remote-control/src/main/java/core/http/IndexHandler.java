package core.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import core.Configuration;
import core.dto.Command;

@SuppressWarnings("restriction")
public class IndexHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("Returning index page");

        OutputStream out = exchange.getResponseBody();
        StringBuilder sb = new StringBuilder();

        // HTML Head
        sb.append("<html><head>");

        // Styles
        sb.append("<style>"
                + "button { margin: 5px; font-size: 50px;"
                + " color: white; background-color: black;"
                + " border-style: solid; border-color: white; }"
                + "body { color: white; background-color: black; }"
                + "</style>");

        // Body
        sb.append("</head><body>");

        // Header
        sb.append("<h1>Remote control</h1>");

        // Buttons
        Collection<Command> commands = Configuration.getCommands().values();
        for (Command command : commands) {
            sb.append("<button onclick=\"sendCommand(this, '" + command.getName() + "');\">" + command.getName() + "</button><br />");
        }

        // JavaScript
        sb.append("<script>"
                + "async function sendCommand(button, command) {"
                + " button.disabled=true;"
                + ""
                + " console.log(command);"
                + ""
                + " const httpRequest = new XMLHttpRequest();"
                + " httpRequest.open(\"POST\", \"/ajax\", true);"
                + " httpRequest.setRequestHeader ("
                + "     \"Content-Type\","
                + "     \"application/x-www-form-urlencoded\","
                + " );"
                + " httpRequest.send('command=' + command);"
                //+ " httpRequest.send();"
                + ""
                + "await sleep(1000);"
                + "button.disabled=false;"
                + "}"
                + "function sleep(ms) {"
                + "    return new Promise(resolve => setTimeout(resolve, ms));"
                + "}"
                + "</script>");
        // TODO Auto-generated method stub

        // HTML End
        sb.append("</body></html>");

        // Sending Response
        byte[] response = sb.toString().getBytes();
        exchange.sendResponseHeaders(200, response.length);
        out.write(response);
        out.flush();
        out.close();
    }
}
