package com.openshine.escova.esplugin;

import com.openshine.escova.Endpoints;
import com.openshine.escova.endpoints.Searchv;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.action.search.RestSearchAction.parseSearchRequest;

/**
 * @author Santiago Saavedra (ssaavedra@openshine.com)
 */
public class EscovaCostCalculatorAction extends BaseRestHandler {
    private final RestHandler dateParserHandler;

    public EscovaCostCalculatorAction(Settings settings, RestController controller) {
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

        return channel -> {
            RestResponse response = Endpoints.java(Searchv.apply(searchRequest.source()));
            channel.sendResponse(response);
        };
        /*
         return channel -> client.search(searchRequest, newRestStatusToXContentListener<>(channel));
         */
    }

}
