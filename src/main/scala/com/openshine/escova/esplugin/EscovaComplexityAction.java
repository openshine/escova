package com.openshine.escova.esplugin;

import com.openshine.escova.DateParser;
import com.openshine.escova.Parser;
import com.openshine.escova.functional.ComplexityMeasure;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.action.search.RestSearchAction.parseSearchRequest;

/**
 * @author Santiago Saavedra (ssaavedra@openshine.com)
 */
public class EscovaComplexityAction extends BaseRestHandler {
    private final RestHandler dateParserHandler;

    public EscovaComplexityAction(Settings settings, RestController controller) {
        super(settings);

        dateParserHandler = new DateParserHandler(settings);
        controller.registerHandler(GET, "/_escova/parse_dates",
                dateParserHandler);
        controller.registerHandler(POST, "/_escova/parse_dates",
                dateParserHandler);

        controller.registerHandler(GET, "/_searchv", this);
        controller.registerHandler(POST, "/_searchv", this);
        controller.registerHandler(GET, "/{index}/_searchv", this);
        controller.registerHandler(POST, "/{index}/_searchv", this);
        controller.registerHandler(GET, "/{index}/{type}/_searchv", this);
        controller.registerHandler(POST, "/{index}/{type}/_searchv", this);

    }


    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        SearchRequest searchRequest = new SearchRequest();

        request.withContentOrSourceParamParserOrNull(parser ->
                parseSearchRequest(searchRequest, request, parser));

        ComplexityMeasure<Object> analyze =
                Parser.analyze(searchRequest.source());

        return channel -> {
            RestResponse response = new RestResponse() {
                @Override
                public String contentType() {
                    return "text/plain";
                }

                @Override
                public BytesReference content() {
                    return new BytesArray("The complexity is: " + analyze);
                }

                @Override
                public RestStatus status() {
                    return RestStatus.OK;
                }
            };
            channel.sendResponse(response);
        };
        /*
         return channel -> client.search(searchRequest, newRestStatusToXContentListener<>(channel));
         */
    }

    private class DateParserHandler implements RestHandler {
        public DateParserHandler(Settings settings) {
        }

        @Override
        public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
            SearchRequest searchRequest = new SearchRequest();

            request.withContentOrSourceParamParserOrNull(parser ->
                    parseSearchRequest(searchRequest, request, parser));

            DateParser.analyze(searchRequest.source(), DateParser.now());

            channel.sendResponse(new RestResponse() {
                @Override
                public String contentType() {
                    return null;
                }

                @Override
                public BytesReference content() {
                    return null;
                }

                @Override
                public RestStatus status() {
                    return null;
                }
            });
        }
    }
}
