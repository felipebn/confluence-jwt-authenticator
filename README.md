# confluence-jwt-authenticator

A custom Atlassian Confluence authenticator to allow automatically authenticate users based on a JWT

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
	```
- Restart Confluence

**TODO: explain how to test**
	

