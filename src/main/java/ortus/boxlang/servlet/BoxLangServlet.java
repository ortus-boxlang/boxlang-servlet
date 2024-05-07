package ortus.boxlang.servlet;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.web.WebRequestExecutor;

public class BoxLangServlet implements Servlet {

	HttpHandler		undertowhandler;
	ServletConfig	config;
	BoxRuntime		runtime;

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
		Boolean	debug		= Boolean.parseBoolean( config.getInitParameter( "boxlang-debug" ) );
		// Null, if not exists. Must be absolute path.
		String	configPath	= config.getInitParameter( "boxlang-config-path" );
		if ( debug != null ) {
			System.out.println( "Ortus BoxLang Servlet debug mode: " + debug );
		}
		if ( configPath != null ) {
			System.out.println( "Ortus BoxLang Servlet config path: " + configPath );
		}
		System.out.println( "Ortus BoxLang Servlet home: " + BLHome.toString() );
		this.runtime = BoxRuntime.getInstance( debug, configPath, BLHome.toString() );
		System.out.println( "Ortus BoxLang Servlet initialized!" );
	}

	public void service( ServletRequest req, ServletResponse res ) throws ServletException, IOException {
		HttpServerExchange		exchange				= null;
		ServletRequestContext	servletRequestContext	= ServletRequestContext.current();
		if ( servletRequestContext != null ) {
			exchange = servletRequestContext.getExchange();
		}
		if ( exchange == null ) {
			throw new ServletException( "This servlet only works inside Undertow. Running on: " + req.getServletContext().getServerInfo() );
		}
		// FusionReactor automatically tracks servlets
		// Note: web root can be different every request if this is a multi-site server or using ModCFML
		WebRequestExecutor.execute( exchange, config.getServletContext().getRealPath( "/" ), false );
	}

	public void destroy() {
		undertowhandler = null;
		this.runtime.shutdown();
		this.runtime = null;
	}

	public ServletConfig getServletConfig() {
		return this.config;
	}

	public String getServletInfo() {
		return "Ortus BoxLang " + this.runtime.getVersionInfo().getOrDefault( Key.version, "" ).toString();
	}
}
