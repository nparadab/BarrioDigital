package cl.duoc.barriodigital.bff.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@Getter
@Slf4j
public class JwtKeyProvider {

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String keyId = "barriodigital-key-1";

    @Value("${security.jwt.issuer:https://login.microsoftonline.com/common/v2.0}")
    private String issuer;

    @Value("${security.jwt.audience:api://barriodigital-api}")
    private String audience;

    public JwtKeyProvider() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            this.publicKey = (RSAPublicKey) kp.getPublic();
            this.privateKey = (RSAPrivateKey) kp.getPrivate();
            log.info("Par de llaves criptográficas RSA 2048-bit inicializado para validación JWT.");
        } catch (Exception e) {
            throw new RuntimeException("Error al generar llaves RSA para validación JWT", e);
        }
    }

    public String generateToken(String username, String displayName, String role, long durationSeconds) {
        try {
            RSASSASigner signer = new RSASSASigner(this.privateKey);
            Instant now = Instant.now();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(this.issuer)
                    .subject(UUID.randomUUID().toString())
                    .audience(this.audience)
                    .issueTime(Date.from(now))
                    .notBeforeTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(durationSeconds)))
                    .claim("preferred_username", username)
                    .claim("name", displayName)
                    .claim("roles", List.of(role))
                    .claim("role", role)
                    .claim("tid", "tenant-barriodigital-duoc")
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(this.keyId)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar token JWT firmado", e);
        }
    }
}
