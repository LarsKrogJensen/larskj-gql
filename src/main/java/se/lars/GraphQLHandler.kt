package se.lars


import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import se.lars.kutil.cast
import javax.inject.Inject

class GraphQLHandler
@Inject
constructor(apiController: IApiController,
            searchController: ISearchController) : GraphQLHandlerBase(apiController, searchController) {

    override fun handle(routingContext: RoutingContext) {
        // request response handler

        // execute with async callback handler that will respond to client
        executeGraphQL(routingContext.body.toString(), routingContext.user().cast<ApiUser>()) {
            routingContext.response()
                    .putHeader("Content-Type", "application/json")
                    .end(it.encode())
        }
    }
}

