package com.blockwit.kafka.security.examples;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.plain.PlainAuthenticateCallback;
import org.apache.kafka.common.security.plain.PlainLoginModule;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
public class CustomAuthenticationCallbackHandler implements AuthenticateCallbackHandler {

    String authServer;

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        String username = null;
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                username = ((NameCallback) callback).getDefaultName();
            } else if (callback instanceof PlainAuthenticateCallback) {
                PlainAuthenticateCallback plainCallback = (PlainAuthenticateCallback) callback;
                plainCallback.authenticated(authenticate(username, String.valueOf(plainCallback.password())));
            } else
                throw new UnsupportedCallbackException(callback);
        }
    }

    protected boolean authenticate(String username, String password) {
        if (username == null || password == null)
            return false;
        log.info("Try custom auth for user " + username);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        String url = authServer + "/?username=" + username + "&password=" + password;
        HttpGet request = new HttpGet(url);
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            int status = response.getStatusLine().getStatusCode();
            response.close();
            return status == 200;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                httpClient.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    @Override
    public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        jaasConfigEntries.stream()
                .filter(t -> t.getLoginModuleName().equals(PlainLoginModule.class.getName()))
                .findAny()
                .map(t -> t.getOptions())
                .ifPresent(t -> authServer = t.get("auth_server").toString());
    }

    @Override
    public void close() {
    }

}
