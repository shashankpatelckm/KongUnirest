/**
 * The MIT License
 *
 * Copyright for portions of unirest-java are held by Kong Inc (c) 2013.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package kong.unirest.java;

import kong.unirest.*;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.function.Function;
import java.util.stream.Stream;

public class JavaClient implements Client {
    private final Config config;
    private final HttpClient client;

    public JavaClient(Config config){
        this.config = config;
        client = HttpClient.newHttpClient();
    }
    @Override
    public Object getClient() {
        return client;
    }

    @Override
    public <T> HttpResponse<T> request(HttpRequest request, Function<RawResponse, HttpResponse<T>> transformer) {
        HttpRequestSummary reqSum = request.toSummary();
        config.getUniInterceptor().onRequest(request, config);
        java.net.http.HttpRequest requestObj = getRequest(request);
        MetricContext metric = config.getMetric().begin(reqSum);
        try {
            //HttpHost host = determineTarget(requestObj, request.getHeaders());
            java.net.http.HttpResponse<InputStream> execute = client.send(requestObj,
                    responseInfo -> java.net.http.HttpResponse.BodySubscribers.ofInputStream());
            JavaResponse t = new JavaResponse(execute, config);
            metric.complete(t.toSummary(), null);
            HttpResponse<T> httpResponse = transformBody(transformer, t);
            //requestObj.releaseConnection();
            config.getUniInterceptor().onResponse(httpResponse, reqSum, config);
            return httpResponse;
        } catch (Exception e) {
            metric.complete(null, e);
            return (HttpResponse<T>) config.getUniInterceptor().onFail(e, reqSum, config);
        } finally {
           // requestObj.releaseConnection();
        }
    }

    private java.net.http.HttpRequest getRequest(HttpRequest request) {
        URI url = URI.create(request.getUrl());
        java.net.http.HttpRequest.Builder jreq = java.net.http.HttpRequest.newBuilder(url)
                .method(request.getHttpMethod().name(), getBody(request));
        request.getHeaders().all().forEach(h -> jreq.header(h.getName(), h.getValue()));

        return jreq.build();
    }

    private java.net.http.HttpRequest.BodyPublisher getBody(HttpRequest request) {
        if(request.getBody().isPresent()){

        }
        return java.net.http.HttpRequest.BodyPublishers.noBody();
    }

    @Override
    public Stream<Exception> close() {
        return Stream.of();
    }

    @Override
    public void registerShutdownHook() {

    }



    protected <T> HttpResponse<T> transformBody(Function<RawResponse, HttpResponse<T>> transformer, RawResponse rr) {
        try {
            return transformer.apply(rr);
        }catch (RuntimeException e){
            String originalBody = recoverBody(rr);
            return new BasicResponse(rr, originalBody, e);
        }
    }

    private String recoverBody(RawResponse rr){
        try {
            return rr.getContentAsString();
        }catch (Exception e){
            return null;
        }
    }
}
