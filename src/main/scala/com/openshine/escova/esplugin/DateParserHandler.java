package com.openshine.escova.esplugin;

import com.openshine.escova.DateParser;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;

import static org.elasticsearch.rest.action.search.RestSearchAction.parseSearchRequest;

/**
 * @author Santiago Saavedra (ssaavedra@openshine.com)
 */
class DateParserHandler implements RestHandler {
    public DateParserHandler(Settings settings) {
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
        SearchRequest searchRequest = new SearchRequest();

        String fieldName = request.param("dateFieldName", "@timestamp");

        request.withContentOrSourceParamParserOrNull(parser ->
                parseSearchRequest(searchRequest, request, parser));

        DateParser.analyze(
                searchRequest.source(), fieldName);

        channel.sendResponse(new RestResponse() {
            @Override
            public String contentType() {
                return "application/json";
            }

            @Override
            public BytesReference content() {
                return null;
            }

            @Override
            public RestStatus status() {
                return RestStatus.OK;
            }
        });
    }
}
