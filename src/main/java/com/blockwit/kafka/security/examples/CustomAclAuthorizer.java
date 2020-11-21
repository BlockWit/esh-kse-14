package com.blockwit.kafka.security.examples;

import kafka.security.authorizer.AclAuthorizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.common.requests.RequestContext;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.apache.kafka.server.authorizer.Action;
import org.apache.kafka.server.authorizer.AuthorizableRequestContext;
import org.apache.kafka.server.authorizer.AuthorizationResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class CustomAclAuthorizer extends AclAuthorizer {

    String authServer;

    @Override
    public List<AuthorizationResult> authorize(AuthorizableRequestContext authRequestContext, List<Action> actions) {
        KafkaPrincipal sessionPrincipal = authRequestContext.principal();

        List<AuthorizationResult> authResults = new ArrayList<>();

        if (isSuperUser(sessionPrincipal)) {
            actions.forEach(a -> authResults.add(AuthorizationResult.ALLOWED));
            return authResults;
        }

        actions.forEach(a -> authResults.add(AuthorizationResult.DENIED));

        String username = sessionPrincipal.getName();
        String password = CustomAuthenticationCallbackHandler.authenticatedUsers.get(username);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        String url = authServer + "/groups?username=" + username + "&password=" + password;
        HttpGet request = new HttpGet(url);

        String[] groups = null;
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            int status = response.getStatusLine().getStatusCode();
            response.close();
            if (status == 200) {
                String content = EntityUtils.toString(response.getEntity());
                groups = content.split(",");
            } else
                return authResults;
        } catch (IOException e) {
            e.printStackTrace();
            return authResults;
        } finally {
            try {
                httpClient.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }


        RequestContext requestContext = (RequestContext) authRequestContext;
        for (int i = 0; i < groups.length; i++) {

            KafkaPrincipal groupKafkaPrincipal = new KafkaPrincipal(username, groups[i]);

            RequestContext groupRequestContext = new RequestContext(
                    requestContext.header,
                    requestContext.connectionId,
                    requestContext.clientAddress,
                    groupKafkaPrincipal,
                    requestContext.listenerName,
                    requestContext.securityProtocol,
                    requestContext.clientInformation);

            List<AuthorizationResult> groupAuthResult = super.authorize(groupRequestContext, actions);
            for (int j = 0; i < groupAuthResult.size(); j++) {
                if (authResults.get(j) == AuthorizationResult.DENIED && groupAuthResult.get(j) == AuthorizationResult.ALLOWED)
                    authResults.set(j, AuthorizationResult.ALLOWED);
            }
        }

        return authResults;
    }

    @Override
    public void configure(Map<String, ?> javaConfigs) {
        super.configure(javaConfigs);
        this.authServer = javaConfigs.get("custom.acl.authorizer.auth.server").toString();
    }

}
