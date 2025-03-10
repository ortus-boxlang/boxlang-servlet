/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.servlet;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.web.WebRequestExecutor;
import ortus.boxlang.web.exchange.BoxHTTPServletExchange;

/**
 * The BoxLangServlet is a servlet that can be used to run BoxLang code in a web application.
 */
public class BoxLangServlet implements Servlet {

	ServletConfig	config;
	BoxRuntime		runtime;

	/**
	 * Initialize the BoxLang servlet.
	 *
	 * @param config The servlet configuration.
	 *
	 * @throws ServletException If an error occurs during initialization.
	 */
	public void init( ServletConfig config ) throws ServletException {
		System.out.println( "Ortus BoxLang Servlet initializing..." );
		this.config = config;

		// detect directory the jar lives that this class was loaded from
		Path jarPath;
		try {
			jarPath = Paths.get( BoxLangServlet.class.getProtectionDomain().getCodeSource().getLocation().toURI() );
		} catch ( URISyntaxException e ) {
			throw new ServletException( e );
		}
		// back up from lib to webapp/WEB-INF folder
		Path	BLHome		= Path.of( jarPath.getParent().getParent().toString(), "boxlang" );

		// Override Boxlang home with init-param if it exists
		String	customHome	= config.getInitParameter( "boxlang-home" );
		if ( customHome != null ) {
			BLHome = Path.of( customHome );
		}
		// Null, if not exists
		Boolean	debug		= null;
		String	debugParam	= config.getInitParameter( "boxlang-debug" );

		// Null, if not exists. Must be absolute path.
		String	configPath	= config.getInitParameter( "boxlang-config-path" );
		if ( debugParam != null ) {
			debug = Boolean.parseBoolean( debugParam );
			System.out.println( "Ortus BoxLang Servlet debug mode: " + debug );
		}
		if ( configPath != null ) {
			System.out.println( "Ortus BoxLang Servlet config path: " + configPath );
		}
		System.out.println( "Ortus BoxLang Servlet home: " + BLHome.toString() );
		this.runtime = BoxRuntime.getInstance( debug, configPath, BLHome.toString() );

		// Register the servlet mapping interceptor
		this.runtime.getInterceptorService().register( new ServletMappingInterceptor( config.getServletContext() ) );

		System.out.println( "Ortus BoxLang Servlet initialized!" );
		IStruct versionInfo = runtime.getVersionInfo();
		System.out.println(
		    "Ortus BoxLang Version: " + versionInfo.getAsString( Key.of( "version" ) ) + " (Built On: " + versionInfo.getAsString( Key.of( "buildDate" ) )
		        + ")" );

	}

	/**
	 * Service the request.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 *
	 * @throws ServletException If an error occurs during request processing.
	 * @throws IOException      If an I/O error occurs.
	 */
	public void service( ServletRequest req, ServletResponse res ) throws ServletException, IOException {
		// FusionReactor automatically tracks servlets
		// Note: web root can be different every request if this is a multi-site server or using ModCFML
		var exchange = new BoxHTTPServletExchange( ( HttpServletRequest ) req, ( HttpServletResponse ) res, this );
		try {
			WebRequestExecutor.execute( exchange, config.getServletContext().getRealPath( "/" ), false );
		} finally {
			// Clean up file uploads
			for ( var upload : exchange.getUploadData() ) {
				try {
					upload.tmpPath().toFile().delete();
				} catch ( Exception e ) {
				}
			}
		}
	}

	/**
	 * Destroy the servlet.
	 */
	public void destroy() {
		this.runtime.shutdown();
		this.runtime = null;
	}

	/**
	 * Get the servlet configuration.
	 */
	public ServletConfig getServletConfig() {
		return this.config;
	}

	/**
	 * Get the BoxLang Servlet information.
	 */
	public String getServletInfo() {
		return "Ortus BoxLang " + this.runtime.getVersionInfo().getOrDefault( Key.version, "" ).toString();
	}
}
