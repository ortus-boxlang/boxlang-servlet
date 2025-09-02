package ortus.boxlang.servlet.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration test for BoxLang Servlet WAR deployment.
 * This test deploys the WAR file to an embedded Jetty server and verifies
 * that BoxLang templates can be executed successfully.
 */
@TestMethodOrder( OrderAnnotation.class )
public class ServletIntegrationTest {

	private static final int	PORT			= 9999;
	private static final String	BASE_URL		= "http://localhost:" + PORT;
	private static final String	CONTEXT_PATH	= "/boxlang-test";

	private Server				server;
	private File				tempDir;
	private CloseableHttpClient	httpClient;
	private WebAppContext		webAppContext;

	@BeforeEach
	public void setUp() throws Exception {
		// Create temp directory for Jetty
		tempDir = Files.createTempDirectory( "jetty-test" ).toFile();
		tempDir.deleteOnExit();

		// Setup HTTP client
		httpClient = HttpClients.custom()
		    .setDefaultRequestConfig(
		        org.apache.hc.client5.http.config.RequestConfig.custom()
		            .setConnectionRequestTimeout( org.apache.hc.core5.util.Timeout.ofSeconds( 30 ) )
		            .setResponseTimeout( org.apache.hc.core5.util.Timeout.ofSeconds( 30 ) )
		            .build()
		    )
		    .build();

		// Setup embedded Jetty
		setupJetty();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if ( httpClient != null ) {
			httpClient.close();
		}

		if ( server != null && server.isStarted() ) {
			try {
				server.stop();
			} catch ( Exception e ) {
				System.err.println( "Error stopping Jetty: " + e.getMessage() );
			}
		}

		// Clean up temp directory
		deleteRecursively( tempDir );
	}

	private void setupJetty() throws Exception {
		// Configure Jetty logging to reduce verbose output
		System.setProperty( "org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog" );
		System.setProperty( "org.eclipse.jetty.LEVEL", "WARN" );
		System.setProperty( "org.eclipse.jetty.server.LEVEL", "INFO" );
		System.setProperty( "org.eclipse.jetty.webapp.LEVEL", "INFO" );
		System.setProperty( "org.eclipse.jetty.io.LEVEL", "WARN" );
		System.setProperty( "org.eclipse.jetty.http.LEVEL", "WARN" );

		server = new Server( PORT );

		// Find the WAR file
		File warFile = findWarFile();
		assertNotNull( warFile, "WAR file not found. Make sure to run 'gradle buildRuntime' first." );

		// Create WebApp context
		webAppContext = new WebAppContext();
		webAppContext.setWar( warFile.getAbsolutePath() );
		webAppContext.setContextPath( CONTEXT_PATH );

		// Set temp directory for extraction
		File extractDir = new File( tempDir, "webapp-extract" );
		extractDir.mkdirs();
		webAppContext.setTempDirectory( extractDir );

		server.setHandler( webAppContext );

		// Start Jetty
		server.start();

		// Wait for startup
		waitForStartup();

		// Copy test resources to the webapp after extraction
		copyTestResources();
	}

	private File findWarFile() {
		// Look in the distributions folder first
		File distDir = new File( "build/distributions" );
		if ( distDir.exists() ) {
			File[] warFiles = distDir.listFiles( ( dir, name ) -> name.endsWith( ".war" ) && name.contains( "boxlang-servlet" ) );
			if ( warFiles != null && warFiles.length > 0 ) {
				return warFiles[ 0 ]; // Return the first WAR found
			}
		}

		// Fallback to libs directory
		File libsDir = new File( "build/libs" );
		if ( libsDir.exists() ) {
			File[] warFiles = libsDir.listFiles( ( dir, name ) -> name.endsWith( ".war" ) && name.contains( "boxlang-servlet" ) );
			if ( warFiles != null && warFiles.length > 0 ) {
				return warFiles[ 0 ];
			}
		}

		return null;
	}

	private void copyTestResources() throws IOException {
		Path testResourcesPath = Paths.get( "src/test/resources/webroot" );
		if ( !Files.exists( testResourcesPath ) ) {
			System.out.println( "Test resources not found at: " + testResourcesPath );
			return;
		}

		// Get the webapp directory from Jetty - need to get the extracted webapp location
		File	tempWebApp			= webAppContext.getTempDirectory();

		// The webapp is extracted under tempDir/webapp-extract/webapp/
		// This is where Jetty extracts the WAR contents
		Path	webappExtractPath	= tempWebApp.toPath().resolve( "webapp" );

		if ( !Files.exists( webappExtractPath ) ) {
			System.out.println( "Extracted webapp not found at: " + webappExtractPath );
			// Try alternative path - maybe the extraction structure is different
			webappExtractPath = tempWebApp.toPath();
		}

		Path targetPath = webappExtractPath;

		System.out.println( "Copying test resources from: " + testResourcesPath + " to: " + targetPath );

		// Copy all test resources to the root of the webapp directory
		Files.walk( testResourcesPath )
		    .forEach( source -> {
			    try {
				    Path relativePath	= testResourcesPath.relativize( source );
				    Path target			= targetPath.resolve( relativePath );

				    if ( Files.isDirectory( source ) ) {
					    if ( !Files.exists( target ) ) {
						    Files.createDirectories( target );
					    }
				    } else {
					    // Ensure parent directory exists
					    if ( target.getParent() != null ) {
						    Files.createDirectories( target.getParent() );
					    }
					    Files.copy( source, target, StandardCopyOption.REPLACE_EXISTING );
					    System.out.println( "Copied: " + source + " -> " + target );
				    }
			    } catch ( IOException e ) {
				    System.err.println( "Failed to copy: " + source + " -> " + e.getMessage() );
				    throw new RuntimeException( "Failed to copy test resources", e );
			    }
		    } );

		// Verify the target file exists
		Path indexFile = targetPath.resolve( "index.bxm" );
		if ( Files.exists( indexFile ) ) {
			System.out.println( "✓ index.bxm successfully copied to: " + indexFile );
		} else {
			System.out.println( "✗ index.bxm not found at: " + indexFile );
		}
	}

	private void waitForStartup() throws InterruptedException {
		// Give Jetty some time to fully start and extract the WAR
		Thread.sleep( 3000 );

		// Try to connect to verify it's up
		int attempts = 0;
		while ( attempts < 10 ) {
			try {
				HttpGet request = new HttpGet( BASE_URL + CONTEXT_PATH + "/" );
				httpClient.execute( request, response -> {
					return null; // Just testing connectivity
				} );
				break; // Success
			} catch ( Exception e ) {
				attempts++;
				Thread.sleep( 1000 );
			}
		}
	}

	@Test
	@Order( 1 )
	@org.junit.jupiter.api.Timeout( value = 60 ) // 60 second timeout
	public void testServerStartup() throws Exception {
		System.out.println( "Testing server startup..." );

		HttpGet	request		= new HttpGet( BASE_URL + CONTEXT_PATH + "/" );

		String	response	= httpClient.execute( request, new HttpClientResponseHandler<String>() {

								@Override
								public String handleResponse( ClassicHttpResponse response ) throws IOException {
									int status = response.getCode();
									assertTrue( status == 200 || status == 404,
									    "Server should be running (got status: " + status + ")" );
									return "OK";
								}
							} );

		assertNotNull( response );
		System.out.println( "✓ Server is running and accessible" );
	}

	@Test
	@Order( 2 )
	@org.junit.jupiter.api.Timeout( value = 60 ) // 60 second timeout
	public void testBoxLangExecution() throws Exception {
		System.out.println( "Testing BoxLang template execution..." );

		HttpGet	request			= new HttpGet( BASE_URL + CONTEXT_PATH + "/index.bxm" );

		String	responseBody	= httpClient.execute( request, new HttpClientResponseHandler<String>() {

									@Override
									public String handleResponse( ClassicHttpResponse response ) throws IOException {
										int		status	= response.getCode();
										String	body	= new String( response.getEntity().getContent().readAllBytes() );

										System.out.println( "Response Status: " + status );

										// Only print first part of body if it's very long (like CSS error pages)
										if ( body.length() > 1000 ) {
											System.out.println( "Response Body Length: " + body.length() + " characters" );
											System.out.println( "Response Body (first 1000 chars): " + body.substring( 0, 1000 ) + "..." );
											System.out.println( "Response Body (last 500 chars): ..." + body.substring( Math.max( 0, body.length() - 500 ) ) );
										} else {
											System.out.println( "Response Body: " + body );
										}

										assertEquals( 200, status, "BoxLang template should execute successfully" );

										return body;
									}
								} );

		assertNotNull( responseBody, "Response body should not be null" );
		assertTrue( responseBody.contains( "success" ),
		    "Response should contain 'success' indicating BoxLang executed properly" );
		assertTrue( responseBody.contains( "BoxLang servlet is working" ),
		    "Response should contain expected message" );

		System.out.println( "✓ BoxLang template executed successfully" );
		System.out.println( "✓ Integration test PASSED - WAR deployment works!" );
	}

	private void deleteRecursively( File file ) {
		if ( file == null || !file.exists() ) {
			return;
		}

		if ( file.isDirectory() ) {
			File[] files = file.listFiles();
			if ( files != null ) {
				for ( File child : files ) {
					deleteRecursively( child );
				}
			}
		}

		file.delete();
	}
}
