package com.github.felipebn.confluence.authentication.jwt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.seraph.auth.DefaultAuthenticator;
import com.atlassian.seraph.config.SecurityConfig;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@RunWith(DataProviderRunner.class)
public class CustomJWTConfluenceAuthenticatorTest{
	private static final String VALID_USERNAME = "thetesteruser"; 
	private static final ConfluenceUser VALID_CONFLUENCE_USER = Mockito.mock(ConfluenceUser.class);
	
	/**
	 * An Authenticator with one valid username to be expected
	 */
	private CustomJWTConfluenceAuthenticator customJWTConfluenceAuthenticator = new CustomJWTConfluenceAuthenticator(){
		private static final long serialVersionUID = 1L;

		protected ConfluenceUser getUser(String uid) {
			if(uid.equals(VALID_USERNAME)){
				return VALID_CONFLUENCE_USER;
			}else{
				return null;	
			}
		};
	};
	
	private HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
	private HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
	private HttpSession session = Mockito.mock(HttpSession.class);
	@BeforeClass
	public static void setup() {
		//Setup log4j
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
	
	@Before
	public void setupSession(){
		//Setup a mock session on the request
		Mockito.when(request.getSession()).thenReturn(session);
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
		
		//We expect the user to be null as the token doesn't contains the username info inside the claim
		assertNull(customJWTConfluenceAuthenticator.getUser(request, response));
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
		final String signingKey = "the-super-secure-signing-key";
		final String validJwtToken = Jwts.builder()
										.setHeaderParam("typ", "JWT")
										.claim("username", VALID_USERNAME)
										.signWith(SignatureAlgorithm.HS256, getBase64EncodedKey(signingKey))
										.compact();
		System.out.println(validJwtToken);
		
		

		Mockito.when(request.getParameter(CustomJWTConfluenceAuthenticator.JWT_TOKEN_REQUEST_PARAM)).thenReturn(validJwtToken);

		//Initializes after replacing the authenticator
		initializeAuthenticatorWithSigningKeyParameter(signingKey);
		
		assertEquals(VALID_CONFLUENCE_USER,customJWTConfluenceAuthenticator.getUser(request, response));
		
		//Asserts that the user was registered on HttpSession
		Mockito.verify(session,Mockito.times(1)).setAttribute(DefaultAuthenticator.LOGGED_IN_KEY, VALID_CONFLUENCE_USER);
		Mockito.verify(session,Mockito.times(1)).setAttribute(DefaultAuthenticator.LOGGED_OUT_KEY, null);
	}

	@Test
	public void shouldNotAuthenticateWhenTheTokenIsExpired(){
		final String signingKey = "the-super-secure-signing-key";
		final String expiredJwtToken = Jwts.builder()
										.setHeaderParam("typ", "JWT")
										.claim("username", "test")
										.setExpiration(new Date(0L))
										.signWith(SignatureAlgorithm.HS256, getBase64EncodedKey(signingKey))
										.compact();
		System.out.println(expiredJwtToken);
		
		initializeAuthenticatorWithSigningKeyParameter(signingKey);

		Mockito.when(request.getParameter(CustomJWTConfluenceAuthenticator.JWT_TOKEN_REQUEST_PARAM)).thenReturn(expiredJwtToken);

		customJWTConfluenceAuthenticator.getUser(request, response);
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
