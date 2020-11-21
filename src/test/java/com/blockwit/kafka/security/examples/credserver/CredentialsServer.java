package com.blockwit.kafka.security.examples.credserver;

import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
public class CredentialsServer {

    public static void main(String[] args) throws IOException {
        Map<String, String> usersToPassword = new HashMap<>();
        usersToPassword.put("alice", "alice-secret");
        usersToPassword.put("robin", "robin-secret");

        Map<String, List<String>> usersToGroups = new HashMap<>();

        List<String> aliceGroups = new ArrayList<>();
        aliceGroups.add("STATS_VIEWERS");
        aliceGroups.add("STATS_WRITERS");
        usersToGroups.put("alice", aliceGroups);

        List<String> robinGroups = new ArrayList<>();
        robinGroups.add("STATS_VIEWERS");
        usersToGroups.put("robin", robinGroups);

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);

        server.createContext("/", httpExchange -> {
            if ("GET".equals(httpExchange.getRequestMethod())) {
                String[] params = httpExchange.getRequestURI().toString()
                        .substring(2)
                        .split("&");
                String username = null;
                String password = null;

                for (int i = 0; i < params.length; i++) {
                    if (params[i].startsWith("username=")) username = params[i].substring(9);
                    else if (params[i].startsWith("password=")) password = params[i].substring(9);
                }

                OutputStream outputStream = httpExchange.getResponseBody();
                if ((username == null || password == null || usersToPassword.get(username) == null) ||
                        !usersToPassword.get(username).equals(password)) {
                    log.info("Wrong username or password for " + username);
                    httpExchange.sendResponseHeaders(401, 0);
                } else {
                    log.info("User " + username + " successfully bind");
                    httpExchange.sendResponseHeaders(200, 0);
                }
                outputStream.flush();
                outputStream.close();
            }
        });

        server.createContext("/groups", httpExchange -> {
            if ("GET".equals(httpExchange.getRequestMethod())) {
                String[] params = httpExchange.getRequestURI().toString()
                        .substring(8)
                        .split("&");
                String username = null;
                String password = null;

                for (int i = 0; i < params.length; i++) {
                    if (params[i].startsWith("username=")) username = params[i].substring(9);
                    else if (params[i].startsWith("password=")) password = params[i].substring(9);
                }

                OutputStream outputStream = httpExchange.getResponseBody();
                if ((username == null || password == null || usersToPassword.get(username) == null) ||
                        !usersToPassword.get(username).equals(password)) {
                    log.info("Wrong username or password for " + username);
                    httpExchange.sendResponseHeaders(401, 0);
                } else {
                    String groups = usersToGroups.get(username).stream().collect(Collectors.joining(","));
                    log.info("User " + username + " successfully take groups " + groups);
                    byte[] contentBytes = groups.getBytes();
                    httpExchange.sendResponseHeaders(200, contentBytes.length);
                    outputStream.write(contentBytes);
                }
                outputStream.flush();
                outputStream.close();
            }
        });

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        server.setExecutor(threadPoolExecutor);
        server.start();
        log.info("Server started on port 8001");
    }

}
