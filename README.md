# confluence-jwt-authenticator

A custom Atlassian Confluence authenticator to allow automatically authenticate users based on a JWT

## How it works

This authenticator expects the following JWT structure to be signed then passed as the value of `JWT_TOKEN` request parameter to Confluence login URL.

If the signature is valid then the user will be authenticated.

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

- Download the latest JAR here (TODO link to latest)
- Copy the JAR to your Confluence `/WEB-INF/lib` directory
- Edit your Seraph config file, which should be at `/WEB-INF/classes/seraph-config.xml`
	- Replace the line `<authenticator class="com.atlassian.confluence.user.ConfluenceAuthenticator"/>` with
	```
	<authenticator class="com.github.felipebn.confluence.authentication.jwt.CustomJWTConfluenceAuthenticator"/>
	```
	- Include the following tags inside the `<parameters>` tag 
	```
<init-param>
        <param-name>com.github.felipebn.confluence.authentication.jwt.plaintext-signing-key</param-name>
        <param-value>YOUR_SUPER_SECRET_KEY_AS_PLAINTEXT</param-value>
    </init-param>
    ````
- Restart confluence

## Testing

To test if everything is working as expected do the following:

- Go to https://jwt.io/
- Set the algorithm to `HS256` and edit the header as defined previously (see JWS Header)
- Set the payload to what is expected (see JWS Payload) and use a valid username for your installation
- Set the signature to whatever you have set on the `init-param`
- Copy the generated token from the right side of the screen
- Go to your Confluence URL adding the JWT to it, should be something like 

	```
	http://myconfluenceinstallation/?JWT_TOKEN=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6ImFfdmFsaWRfY29uZmx1ZW5jZV91c2VybmFtZSJ9.cl9-d8I_DESOxqVwMg5oN2rXD_anATRemDaz9RcwRDQ
	```
- The user you defined in the claim should be authenticated
	

