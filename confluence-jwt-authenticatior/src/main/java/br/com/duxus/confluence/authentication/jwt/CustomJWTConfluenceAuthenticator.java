package br.com.duxus.confluence.authentication.jwt;

import java.security.Principal;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.atlassian.confluence.user.ConfluenceAuthenticator;
import com.atlassian.seraph.config.SecurityConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;

/**
 * Authenticates an user based on a JWT claim.
 * 
 * The security of this authentication is based on the JWT signature.
 * 
 * If someone can provide a valid JWT with an existing username, then the user
 * will be authenticaded.
 * 
 * @author felipebn
 *
 */
public class CustomJWTConfluenceAuthenticator extends ConfluenceAuthenticator{

	private static final long serialVersionUID = 1L;
	
	private static final Log log = LogFactory.getLog(CustomJWTConfluenceAuthenticator.class);
	
	/**
	 * This init param will be the signing key to validate the JWT.
	 */
	protected static final String JWT_SIGNING_KEY_PARAM = "jwt-signing-key";
	/**
	 * This request param will carry the JWT with claims for authentication
	 */
	protected static final String JWT_TOKEN_REQUEST_PARAM = "JWT_TOKEN";
	
	private String jwtSigningKey;

	@Override
	public void init(Map<String, String> params, SecurityConfig config) {
		this.jwtSigningKey = params.get(JWT_SIGNING_KEY_PARAM);
		if( StringUtils.isBlank(this.jwtSigningKey) ){
			throw new IllegalArgumentException(String.format("You need to setup the '%s' init param on Confluence's 'seraph-config.xml' file!",JWT_SIGNING_KEY_PARAM));
		}
		super.init(params, config);
	}
	
	@Override
	public Principal getUser(HttpServletRequest request, HttpServletResponse response) {
		final String jwtToken = request.getParameter(JWT_TOKEN_REQUEST_PARAM);
		if( StringUtils.isBlank(jwtToken) ){
			return super.getUser(request,response);
		}else{
			return getUserFromJWTToken(jwtToken);	
		}
	}

	protected Principal getUserFromJWTToken(String jwtToken) {
		String username = getUsernameFromJWT(jwtToken);
		
		Principal user = null;
		
		if( StringUtils.isNotBlank(username) ) {
			user = getUser(username);
		}
		
		return user;
	}

	private String getUsernameFromJWT(String jwtToken) {
		try{
			String base64EncodedKey = Base64.encodeBase64String(this.jwtSigningKey.getBytes("UTF-8"));
			@SuppressWarnings("rawtypes")
			Jwt<JwsHeader, Claims> token = Jwts.parser()
							.setSigningKey(base64EncodedKey)
							.parseClaimsJws(jwtToken);//As it's a signed token we use JWS claims.
			
			String username = token.getBody().get("username", String.class);
			return username;
		}catch(Exception e){
			log.info(String.format("Unable to parse JWT: %s",jwtToken), e);
			return null;
		}
	}
	
}
