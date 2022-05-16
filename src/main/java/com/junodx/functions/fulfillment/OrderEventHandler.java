package com.junodx.functions.fulfillment;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

public class OrderEventHandler  implements RequestHandler<SQSEvent, Object> {

    protected String RESOURCE_ARN;
    protected String SECRET_ARN;

    protected String fixedSeqRunId;
    protected String fixedPipelineRunId;

    protected String clientId;
    protected String clientSecret;

    protected String alliedUrl;
    protected String alliedClientId;
    protected String alliedClientSecret;

    protected String baseUrl;

    String applicationConfigurationFileName = "application.properties";

    private ObjectMapper mapper;
    private Properties props;
    private InputStream inStream;

    public OrderEventHandler() {
        mapper = new ObjectMapper();
        props = new Properties();
        try {
            loadConfiguration();
        } catch(Exception e){

        }
    }

    public void loadConfiguration() throws IOException {
        try {
            inStream = getClass().getClassLoader().getResourceAsStream(applicationConfigurationFileName);
            if (inStream != null) {
                props.load(inStream);

                RESOURCE_ARN = props.getProperty("RESOURCE_ARN");
                SECRET_ARN = props.getProperty("SECRET_ARN");
                fixedSeqRunId = props.getProperty("fixedSeqRunId");
                fixedPipelineRunId = props.getProperty("fixedPipelineRunId");

                alliedUrl = props.getProperty("jdx.connectors.fulfillment.allied.dev");
                alliedClientId = props.getProperty("jdx.connectors.fulfillment.allied.dev.clientId");
                alliedClientSecret = props.getProperty("jdx.connectors.fulfillment.allied.dev.secret");

                clientId = props.getProperty("clientId");
                clientSecret = props.getProperty("clientSecret");

                baseUrl = props.getProperty("baseUrl");
            }
        } catch(Exception e){
            e.printStackTrace();
        } finally {
            inStream.close();
        }
    }

    @Override
    public Object handleRequest(SQSEvent snsEvent, Context context) {

        //return callAlliedCatalog(alliedUrl, alliedClientId, alliedClientSecret);
        return createOrderWithAllied(alliedUrl, alliedClientId, alliedClientSecret);

    }

    protected String createOrderWithAllied(String url, String id, String secret) {
        try {
            String body = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                    "<tagpayload xmlns=\"https://secure.thealliedgrp.com/integration/schemas/orders\">\n" +
                    "<tagorder>\n" +
                    "<orderheader>\n" +
                    "<billto>99999</billto>\n" +
                    "<cc>costcenter</cc>\n" +
                    "<po>purchaseorder</po>" +
                    "<saddr1>25 Amflex Drive</saddr1>\n" +
                    "<saddr2></saddr2>\n" +
                    "<scity>Cranston</scity>\n" +
                    "<sstate>RI</sstate>\n" +
                    "<szip>02921</szip>\n" +
                    "<scountry>US</scountry>\n" +
                    "<shipvia>UPS</shipvia>\n" +
                    "<onitemerr>I</onitemerr>\n" +
                    "</orderheader>\n" +
                    "<orderdetail>\n" +
                    "<detline>\n" +
                    "<itemno>JUNO-K-001</itemno>\n" +
                    "<qty>1</qty>\n" +
                    "<pkunit>EA</pkunit>\n" +
                    "<pkunitqty>1</pkunitqty>\n" +
                    "</detline>\n" +
                    "</orderdetail>\n" +
                    "</tagorder>\n" +
                    "</tagpayload>";

            System.out.println("Payload: " + body);

            String response = httpSend(url + "/orders", id, secret, body);

            System.out.println("Response: " + body);

            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return "";
    }

    protected String callAlliedCatalog(String url, String id, String secret) {
        try {
            return httpSend(url + "/catalog?item=", id, secret, null);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return "";
    }

    public String httpSend(String uri, String identity, String secret, String body) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest httpRequest;
        if(body != null) {
            httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .header("Identity", identity)
                    .header("Shared-Secret", secret)
                    .header("Content-Type", "application/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
        } else {
            httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .header("Identity", identity)
                    .header("Shared-Secret", secret)
                    .GET()
                    .build();
        }

        HttpResponse<String> response = HttpClient
                .newBuilder()
                .build()
                .send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() == 200)
            return response.body();
        else
            throw new HttpClientAccessException("Error occurred calling Juno API " + response.statusCode() + " : " + response.body() + " ");
    }
}
