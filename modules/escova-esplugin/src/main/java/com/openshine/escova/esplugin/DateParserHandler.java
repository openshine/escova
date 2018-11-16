package com.openshine.escova.esplugin;

import com.openshine.escova.Endpoints;
import com.openshine.escova.endpoints.ParseDate;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;

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

        RestResponse response = Endpoints.java(
                ParseDate.apply(searchRequest.source(), fieldName));

        channel.sendResponse(response);
    }
}
