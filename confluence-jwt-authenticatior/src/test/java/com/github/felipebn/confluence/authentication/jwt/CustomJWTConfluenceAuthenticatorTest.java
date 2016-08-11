package com.github.felipebn.confluence.authentication.jwt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.atlassian.seraph.config.SecurityConfig;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@RunWith(DataProviderRunner.class)
public class CustomJWTConfluenceAuthenticatorTest{
	
	private CustomJWTConfluenceAuthenticator customJWTConfluenceAuthenticator = new CustomJWTConfluenceAuthenticator();
	private HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
	private HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
	
	@BeforeClass
	public static void setup() {
		Logger rootLogger = Logger.getRootLogger();
		rootLogger.removeAllAppenders();
		rootLogger.addAppender(new ConsoleAppender(new PatternLayout("%d [%p|%c|%C{1}] %m%n")){
			@SuppressWarnings("deprecation")
			@Override
			public Priority getThreshold() {
				return Priority.FATAL;
			}
		});
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void shouldThrowAnErrorOnInitWithoutTheSigningKeyParam(){
		Map<String, String> params = new HashMap<String, String>();
		customJWTConfluenceAuthenticator.init(params , Mockito.mock(SecurityConfig.class));
	}
	
	
	@Test
	public void shouldReturnNullBecauseTheTokenIsInvalid(){
		initializeAuthenticatorWithSigningKeyParameter("testing!!!");
				
		String invalidJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";
		Mockito.when(request.getParameter(CustomJWTConfluenceAuthenticator.JWT_TOKEN_REQUEST_PARAM)).thenReturn(invalidJwtToken);
		
		//We expect the user to be null as the token is invalid
		assertNull(customJWTConfluenceAuthenticator.getUser(request, response));
	}

	@Test
	public void shouldReturnNullBecauseTheTokenDoesntContainUsernameInsideTheClaim(){
		final String signingKey = "the-super-secure-signing-key";
		final String validJwtToken = Jwts.builder()
										.setHeaderParam("typ", "JWT")
										.claim("anyinfo", "whatever")
										.signWith(SignatureAlgorithm.HS256, getBase64EncodedKey(signingKey))
										.compact();
		System.out.println(validJwtToken);
		
		initializeAuthenticatorWithSigningKeyParameter(signingKey);

		Mockito.when(request.getParameter(CustomJWTConfluenceAuthenticator.JWT_TOKEN_REQUEST_PARAM)).thenReturn(validJwtToken);
		
		//We expect the user to be null as the token doesn't contains the username info inside the claim
		assertNull(customJWTConfluenceAuthenticator.getUser(request, response));
	}
	
	@Test
	@DataProvider(convertNulls=true,value={
		"",
		"john_doe",
	})
	public void shouldReturnNullBecauseTheTokenDoesntContainAValidUsername(final String username){
		final String signingKey = "the-super-secure-signing-key";
		final String validJwtToken = Jwts.builder()
										.setHeaderParam("typ", "JWT")
										.claim("username", username)
										.signWith(SignatureAlgorithm.HS256, getBase64EncodedKey(signingKey))
										.compact();
		System.out.println(validJwtToken);
		
		initializeAuthenticatorWithSigningKeyParameter(signingKey);

		Mockito.when(request.getParameter(CustomJWTConfluenceAuthenticator.JWT_TOKEN_REQUEST_PARAM)).thenReturn(validJwtToken);
		
		/*
		 * A mock authenticator which will return null if the username is the same as informed 
		 * inside the claim.
		 */
		CustomJWTConfluenceAuthenticator authenticator = new CustomJWTConfluenceAuthenticator(){
			private static final long serialVersionUID = 1L;
			@Override
			protected Principal getUser(String uid) {
				if(uid.equals(username)){
					return null;
				}else{
					return Mockito.mock(Principal.class);	
				}
			}
		};
		
		//We expect the user to be null as the token doesn't contains the username info inside the claim
		assertNull(authenticator.getUser(request, response));
	}

	@Test
	public void shouldBypassTheJWTProcessingBecauseTheParamIsNotPresentAtTheRequest(){			
		/*
		 * Replaces the authenticator with a mock authenticator which 
		 * will fail the test if the method is invoked. 
		 */
		customJWTConfluenceAuthenticator = new CustomJWTConfluenceAuthenticator(){
			private static final long serialVersionUID = 1L;
			@Override
			protected Principal getUserFromJWTToken(String jwtToken) {
				fail("This method shouldn't be invoked during this test!");
				return null;
			}
		};
		
		//Initialize the authenticator so it has a SecurityConfig mock instance
		initializeAuthenticatorWithSigningKeyParameter("andjkjhakjdkjahdkj");
		
		customJWTConfluenceAuthenticator.getUser(request, response);
	}

	@Test
	public void shouldCorrectlyAuthenticateTheUserDefinedInsideTheClaim(){
		final Principal expectedUser = Mockito.mock(Principal.class);
		final String username = "THEUSER";
		final String signingKey = "the-super-secure-signing-key";
		final String validJwtToken = Jwts.builder()
										.setHeaderParam("typ", "JWT")
										.claim("username", username)
										.signWith(SignatureAlgorithm.HS256, getBase64EncodedKey(signingKey))
										.compact();
		System.out.println(validJwtToken);
		
		

		Mockito.when(request.getParameter(CustomJWTConfluenceAuthenticator.JWT_TOKEN_REQUEST_PARAM)).thenReturn(validJwtToken);

		/*
		 * Replaces the authenticator with a mock authenticator which 
		 * will return the expected Principal IF the username is the same
		 * as it expects.
		 */
		customJWTConfluenceAuthenticator = new CustomJWTConfluenceAuthenticator(){
			private static final long serialVersionUID = 1L;
			@Override
			protected Principal getUser(String uid) {
				if(uid.equals(username)){
					return expectedUser;
				}else{
					return null;
				}
			}
		};
		
		//Initializes after replacing the authenticator
		initializeAuthenticatorWithSigningKeyParameter(signingKey);
		
		assertEquals(expectedUser,customJWTConfluenceAuthenticator.getUser(request, response));
	}

	
	private void initializeAuthenticatorWithSigningKeyParameter(String signingKey){
		Map<String, String> params = new HashMap<String, String>();		
		params.put(CustomJWTConfluenceAuthenticator.JWT_PLAINTEXT_SIGNING_KEY_PARAM, signingKey);
		customJWTConfluenceAuthenticator.init(params, Mockito.mock(SecurityConfig.class,Mockito.RETURNS_DEEP_STUBS));		
	}
	
	private String getBase64EncodedKey(String plainTextSigningKey){
		try{
			return Base64.encodeBase64String(plainTextSigningKey.getBytes("UTF-8"));	
		}catch(Exception e){
			throw new RuntimeException("Something went wrong during base64 encoding...",e);
		}
		
	}
}
