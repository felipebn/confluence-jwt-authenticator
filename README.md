# confluence-jwt-authenticator

A custom Atlassian Confluence authenticator to allow automatically authenticate users based on a JWT

## How it works

This authenticator expects the following JWT structure to be signed then passed as the value of `JWT_TOKEN` request parameter to Confluence login URL.

If the signature is valid then the user will be authenticated.

This implementation follows the JWT spec, so you can use other algorithms or include the `nbf` (Not Before) or `exp` (Expiration) claims on your payload, they will be considered for token validation.

Actually you can include anything inside the claims, the user will be authenticated with the `username` claim.

### JWS Header

```
{
  "typ": "JWT",
  "alg": "HS256"
}
```

### JWS Payload (Claims)

```
{
  "username": "a_valid_confluence_username"
}
```


## Installation

To install this authenticator follow the steps below:

- Download the latest JAR [here](confluence-authentication-jwt-1.0.0.jar)
- Copy the JAR to your Confluence `/WEB-INF/lib` directory
- Edit your Seraph config file, which should be at `/WEB-INF/classes/seraph-config.xml`
	- Replace the line `<authenticator class="com.atlassian.confluence.user.ConfluenceAuthenticator"/>` with
	```
	<authenticator class="com.github.felipebn.confluence.authentication.jwt.CustomJWTConfluenceAuthenticator">
		<init-param>
			<param-name>com.github.felipebn.confluence.authentication.jwt.plaintext-signing-key</param-name>
			<param-value>YOUR_SUPER_SECRET_KEY_AS_PLAINTEXT</param-value>
		</init-param>
	</authenticator>
	```
	- Needless to say that you should replace `YOUR_SUPER_SECRET_KEY_AS_PLAINTEXT` with a personalized hard to find key
- Restart confluence

### Testing

To test if everything is working as expected do the following:

- Go to http://jwtbuilder.jamiekurtz.com/
- Clear all Standard JWT Claims
- Clear all Additional JWT Claims
- Add the `username` claim setting it's value to an existing username in Confluence
- Set the key to whatever you have set on the `init-param`
- Set the algorithm to `HS256`
- Click on `Create Signed JWT`
- Copy the JWT
- Go to your Confluence URL adding the JWT to it, should be something like 

	```
	http://myconfluenceinstallation/?JWT_TOKEN=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6ImFfdmFsaWRfY29uZmx1ZW5jZV91c2VybmFtZSJ9.cl9-d8I_DESOxqVwMg5oN2rXD_anATRemDaz9RcwRDQ
	```
- The user you defined in the claim should be authenticated
	
## Usage

After installing the authenticator and validating that the authentication works, all you need to do is:

- Generate a JWT with a `username` Claim, a `exp` Claim (optional but very recommended) and sign it with your Secret key
- Include the `JWT_TOKEN` on any URL pointing to your confluence installation
- Redirect to the URL or share it with anyone
