package com.al.plugins

import com.al.Config
import com.al.features.session.SessionDao
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("JwtConfig")

fun Application.configureSecurity(sessionDao: SessionDao) {
    val config = Config()
    val jwtAudience = environment.config.property("ktor.security.jwt.audience").getString()
    val jwtIssuer = environment.config.property("ktor.security.jwt.issuer").getString()
    val jwtRealm = environment.config.property("ktor.security.jwt.realm").getString()

    logger.info("\n=== JWT Configuration ===")
    logger.info("Realm: $jwtRealm")
    logger.info("Audience: $jwtAudience")
    logger.info("Issuer: $jwtIssuer")
    logger.info("Secret: ${if (config.jwtSecret.isBlank()) "EMPTY" else "*****"}")

    install(Authentication) {
        jwt(name = "jwt-auth") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(config.jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                val jti = credential.payload.getClaim("jti").asString() ?: return@validate null

                // Validate session using SessionDao
                if (sessionDao.isSessionActive(jti)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}