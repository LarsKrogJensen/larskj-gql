package se.lars

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.PemKeyCertOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.ext.web.handler.StaticHandler
import se.lars.accesslog.AccessLogHandler
import se.lars.auth.JWTAuthenticator
import se.lars.chat.ChatSystemHandler
import se.lars.kutil.jsonObject
import se.lars.kutil.loggerFor
import se.lars.kutil.router
import javax.inject.Inject


class WebServerVerticle
@Inject
constructor(
        private val _serverOptions: IServerOptions,
        private val _graphQLHandler: GraphQLHandler,
        private val _graphQLHandlerWs: GraphQLHandlerOverWS,
        private val _chatHandler: ChatSystemHandler,
        private val _apiController: IApiController) : AbstractVerticle() {

    private val _log = loggerFor<WebServerVerticle>()
    private lateinit var httpServer: HttpServer

    override fun start(startFuture: Future<Void>) {
        val options = HttpServerOptions().apply {
            isCompressionSupported = true
            if (_serverOptions.useSsl) {
                isUseAlpn = true
                isSsl = true
                pemKeyCertOptions = PemKeyCertOptions().apply {
                    keyPath = "tls/server-key.pem"
                    certPath = "tls/server-cert.pem"
                }
            }
        }

        // configure cross domain access
        val corsHandler = with(CorsHandler.create("*")) {
            allowCredentials(true)
            allowedMethod(HttpMethod.POST)
            allowedHeaders(setOf("content-type", "authorization"))
        }

        val keystoreConfig = jsonObject("keyStore" to jsonObject("path" to "keystore.jceks",
                                                                 "type" to "jceks",
                                                                 "password" to "secret"))

        val provider = JWTAuth.create(vertx, keystoreConfig)

        val router = router(vertx) {
            route().handler(AccessLogHandler.create("%r %s \"%{Content-Type}o\" %D %T %B"))
            route().handler(corsHandler)
            route().handler(BodyHandler.create())
            route("/authenticate").handler(JWTAuthenticator(_apiController, provider))
            route("/graphql*").handler(JWTAuthHandler.create(provider))
            route("/chat").handler(_chatHandler)
            route("/graphql").handler(_graphQLHandler)
            route("/graphqlws").handler(_graphQLHandlerWs)
            route("/*").handler(StaticHandler.create().setCachingEnabled(false))
        }

        httpServer = vertx.createHttpServer(options)
                .requestHandler { router.accept(it) }
                .listen(_serverOptions.httpPort) {
                    when (it.succeeded()) {
                        true  -> {
                            _log.info("Http service started. on port " + it.result().actualPort())
                            startFuture.succeeded()
                        }
                        false -> {
                            _log.error("Http service failed to started.")
                            startFuture.fail(it.cause())
                        }
                    }
                }
    }

    override fun stop(stopFuture: Future<Void>) {
        _log.info("Stopping Http service...")
        httpServer.close {
            _log.info("Http service stopped")
            stopFuture.complete()
        }

    }
}