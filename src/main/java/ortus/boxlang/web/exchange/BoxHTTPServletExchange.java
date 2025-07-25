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
package ortus.boxlang.web.exchange;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.jakarta.servlet5.JakartaServletFileUpload;
import org.apache.commons.io.output.NullWriter;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.web.context.WebRequestBoxContext;

/**
 * I implement a BoxLang HTTP exchange for a Servlet
 */
public class BoxHTTPServletExchange implements IBoxHTTPExchange {

	/**
	 * The servlet context
	 */
	protected ServletContext		servletContext;

	/**
	 * The servlet
	 */
	protected Servlet				servlet;

	/**
	 * The servlet request
	 */
	protected HttpServletRequest	request;

	/**
	 * The servlet response
	 */
	protected HttpServletResponse	response;

	/**
	 * The BoxLang context for this request
	 */
	protected WebRequestBoxContext	boxContext;

	/**
	 * The list of file uploads
	 */
	List<FileUpload>				fileUploads	= new ArrayList<FileUpload>();

	/**
	 * PrintWriter for the response that wraps the servlet's
	 */
	WhitespaceManagingPrintWriter	writer;

	/**
	 * Create a new BoxLang HTTP exchange for a Servlet
	 *
	 * @param request  The servlet request
	 * @param response The servlet response
	 */
	public BoxHTTPServletExchange( HttpServletRequest request, HttpServletResponse response, Servlet servlet ) {
		this.servletContext	= request.getServletContext();
		this.request		= request;
		this.response		= response;
		this.servlet		= servlet;
	}

	/**
	 * Get the servlet context
	 *
	 * @return The servlet context
	 */
	public ServletContext getServletContext() {
		return servletContext;
	}

	/**
	 * Get the servlet request
	 *
	 * @return The servlet request
	 */
	public HttpServletRequest getServletRequest() {
		return request;
	}

	/**
	 * Get the servlet response
	 *
	 * @return The servlet response
	 */
	public HttpServletResponse getServletResponse() {
		return response;
	}

	/**
	 * Get the servlet
	 *
	 * @return The servlet
	 */
	public Servlet getServlet() {
		return servlet;
	}

	@Override
	public void setWebContext( WebRequestBoxContext boxContext ) {
		this.boxContext = boxContext;
	}

	@Override
	public WebRequestBoxContext getWebContext() {
		return boxContext;
	}

	@Override
	public void forward( String URI ) {
		try {
			servletContext.getRequestDispatcher( URI ).forward( request, response );
		} catch ( ServletException | IOException e ) {
			throw new BoxRuntimeException( "Could not forward request", e );
		}
	}

	@Override
	public void addResponseCookie( BoxCookie cookie ) {
		response.addHeader( "Set-Cookie", cookie.toSetCookieHeader() );
		// We can't use the below until we starting using the Jakarta Servlet API 6.0 which supports setAttribute().
		// The current servlet API 5.0 does not support samesite or expires attributes on cookies.

		/*
		 * Cookie c = new Cookie( cookie.getName(), cookie.getEncodedValue() );
		 * // Make sure our cookies are RFC 2019 Compliant so that values are not encoded
		 * c.setVersion( cookie.getVersion() );
		 * if ( cookie.getDomain() != null )
		 * c.setDomain( cookie.getDomain() );
		 * if ( cookie.getPath() != null )
		 * c.setPath( cookie.getPath() );
		 * c.setSecure( cookie.isSecure() );
		 * c.setHttpOnly( cookie.isHttpOnly() );
		 * if ( cookie.getMaxAge() != null )
		 * c.setMaxAge( cookie.getMaxAge() );
		 * // TODO: Does servlet not support these?
		 * // c.setSameSite(cookie.isSameSite());
		 * // c.setExpires(cookie.getExpires());
		 * // c.setSameSiteMode(cookie.getSameSiteMode());
		 * 
		 * response.addCookie( c );
		 */
	}

	@Override
	public void addResponseHeader( String name, String value ) {
		response.addHeader( name, value );
	}

	@Override
	public void flushResponseBuffer() {
		try {
			ensureResponseContentType();

			// Update this in case the content type has changed
			writer.setWhitespaceCompressionEnabled( boxContext.isWhitespaceCompressionEnabled() );
			writer.flush();
			response.flushBuffer();
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Could not flush response buffer", e );
		}
	}

	@Override
	public Object getRequestAttribute( String name ) {
		return request.getAttribute( name );
	}

	@Override
	public Map<String, Object> getRequestAttributeMap() {
		Map<String, Object>	attributes		= new HashMap<>();
		Enumeration<String>	attributeNames	= request.getAttributeNames();
		while ( attributeNames.hasMoreElements() ) {
			String name = attributeNames.nextElement();
			attributes.put( name, request.getAttribute( name ) );
		}
		return attributes;
	}

	@Override
	public String getRequestAuthType() {
		return request.getAuthType();
	}

	@Override
	public String getRequestCharacterEncoding() {
		return request.getCharacterEncoding();
	}

	@Override
	public long getRequestContentLength() {
		return request.getContentLengthLong();
	}

	@Override
	public String getRequestContentType() {
		return request.getContentType();
	}

	@Override
	public String getRequestContextPath() {
		return request.getContextPath();
	}

	@Override
	public BoxCookie[] getRequestCookies() {
		Cookie[] cookies = request.getCookies();
		if ( cookies == null ) {
			return new BoxCookie[ 0 ];
		}
		List<Cookie>	cookieList	= List.of( cookies );
		List<BoxCookie>	boxCookies	= new ArrayList<>();
		for ( Cookie cookie : cookieList ) {
			// Only name and value are sent from the browser, so we don't bother with the other getters
			var c = BoxCookie.fromEncoded( cookie.getName(), cookie.getValue() );
			boxCookies.add( c );
		}
		return boxCookies.toArray( new BoxCookie[ 0 ] );
	}

	@Override
	public Map<String, String[]> getRequestHeaderMap() {
		Map<String, String[]>	headers		= new HashMap<>();
		Enumeration<String>		headerNames	= request.getHeaderNames();
		while ( headerNames.hasMoreElements() ) {
			String				name		= headerNames.nextElement();
			Enumeration<String>	values		= request.getHeaders( name );
			List<String>		valueList	= Collections.list( values );
			headers.put( name, valueList.toArray( new String[ 0 ] ) );
		}
		return headers;
	}

	@Override
	public String getRequestHeader( String name ) {
		return request.getHeader( name );
	}

	@Override
	public String getRequestLocalAddr() {
		return request.getLocalAddr();
	}

	@Override
	public String getRequestLocalName() {
		return request.getLocalName();
	}

	@Override
	public int getRequestLocalPort() {
		return request.getLocalPort();
	}

	@Override
	public Locale getRequestLocale() {
		return request.getLocale();
	}

	@Override
	public Enumeration<Locale> getRequestLocales() {
		return request.getLocales();
	}

	@Override
	public String getRequestMethod() {
		return request.getMethod();
	}

	@Override
	public Map<String, String[]> getRequestURLMap() {
		String queryString = request.getQueryString();
		if ( queryString == null ) {
			return Collections.emptyMap();
		}

		try {
			Map<String, List<String>>	params	= new HashMap<>();
			String[]					pairs	= queryString.split( "&" );
			for ( String pair : pairs ) {
				int		idx	= pair.indexOf( "=" );
				String	key;
				key = idx > 0 ? URLDecoder.decode( pair.substring( 0, idx ), "UTF-8" ) : pair;
				if ( !params.containsKey( key ) ) {
					params.put( key, new LinkedList<String>() );
				}
				String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode( pair.substring( idx + 1 ), "UTF-8" ) : "";
				params.get( key ).add( value );
			}

			return params.entrySet().stream()
			    .collect( Collectors.toMap( Map.Entry::getKey, e -> e.getValue().toArray( new String[ 0 ] ) ) );
		} catch ( UnsupportedEncodingException e ) {
			throw new BoxRuntimeException( "Could not decode URL", e );
		}
	}

	@Override
	public Map<String, String[]> getRequestFormMap() {
		Map<String, List<String>>	params		= new HashMap<>();

		String						contentType	= request.getContentType();
		// We can only parse form fields if this is a POST request with a form content type
		if ( !getRequestMethod().equalsIgnoreCase( "POST" ) || contentType == null ) {
			return Collections.emptyMap();
		}

		try {
			if ( contentType.startsWith( "application/x-www-form-urlencoded" ) ) {
				String		requestBody	= request.getReader().lines().collect( Collectors.joining( System.lineSeparator() ) );
				String[]	pairs		= requestBody.split( "&" );
				for ( String pair : pairs ) {
					int idx = pair.indexOf( "=" );
					// ignore empty names
					if ( idx <= 0 ) {
						continue;
					}
					String key = URLDecoder.decode( pair.substring( 0, idx ), getCharacterEncodingOrDefault() );
					// Just in case, check again for empty names
					if ( key.isEmpty() ) {
						continue;
					}
					String value = URLDecoder.decode( pair.substring( idx + 1 ), getCharacterEncodingOrDefault() );
					params.computeIfAbsent( key, k -> new LinkedList<>() ).add( value );
				}
			} else if ( contentType.startsWith( "multipart/form-data" ) ) {

				DiskFileItemFactory											factory	= DiskFileItemFactory.builder().get();
				JakartaServletFileUpload<DiskFileItem, DiskFileItemFactory>	upload	= new JakartaServletFileUpload<DiskFileItem, DiskFileItemFactory>(
				    factory );

				List<DiskFileItem>											items	= upload.parseRequest( request );
				for ( DiskFileItem item : items ) {
					String name = item.getFieldName();
					if ( item.isFormField() ) {
						// This is a regular form field
						String value = item.getString();
						params.computeIfAbsent( name, k -> new LinkedList<>() ).add( value );
					} else {
						// This is a file
						Path storeLocation = item.getPath();
						if ( storeLocation != null ) {
							// A file was uploaded (it might be a 0KB file)
							String filePath = storeLocation.toString();
							params.computeIfAbsent( name, k -> new LinkedList<>() ).add( filePath );
							fileUploads.add( new FileUpload( Key.of( name ), storeLocation, item.getName() ) );
						} else {
							// The file input field was left empty
							params.computeIfAbsent( name, k -> new LinkedList<>() ).add( "" );
						}
					}
				}
			}
		} catch ( IllegalStateException e ) {
			// if request is already read, we can't read it again
		} catch ( IOException e ) {
			throw new RuntimeException( "Could not parse form parameters", e );
		}

		return params.entrySet().stream()
		    .collect( Collectors.toMap( Map.Entry::getKey, e -> e.getValue().toArray( new String[ 0 ] ) ) );
	}

	public String getCharacterEncodingOrDefault() {
		String encoding = request.getCharacterEncoding();
		return encoding != null ? encoding : "UTF-8";
	}

	@Override
	public FileUpload[] getUploadData() {
		return fileUploads.toArray( new FileUpload[ 0 ] );
	}

	@Override
	public String getRequestPathInfo() {
		return request.getPathInfo();
	}

	@Override
	public String getRequestPathTranslated() {
		return request.getPathTranslated();
	}

	@Override
	public String getRequestProtocol() {
		return request.getProtocol();
	}

	@Override
	public String getRequestQueryString() {
		return request.getQueryString();
	}

	@Override
	public Object getRequestBody() {
		try {
			InputStream inputStream = request.getInputStream();

			// FUSIONREACTOR BUG-- available() returns 0 EVEN WHEN THERE ARE ACTUALLY BYTES TO READ because they wrap the stupid input stream
			// If the stream is actually read, we'll just have to hit the catch blow below instead of a nice pre-emptive check.
			// If this stream has already been read, return an empty string

			// TODO: Figure out how to intercept the input stream so we can access
			// it even after the form scope has been processed.
			// if ( inputStream.available() == 0 ) {
			// return "";
			// }
			if ( isTextBasedContentType() ) {
				try ( Scanner scanner = new java.util.Scanner( inputStream ).useDelimiter( "\\A" ) ) {
					return scanner.hasNext() ? scanner.next() : "";
				}
			} else {
				return inputStream.readAllBytes();
			}
		} catch ( IOException | IllegalStateException e ) {
			return "";
		}
	}

	@Override
	public String getRequestRemoteAddr() {
		return request.getRemoteAddr();
	}

	@Override
	public String getRequestRemoteHost() {
		return request.getRemoteHost();
	}

	@Override
	public int getRequestRemotePort() {
		return request.getRemotePort();
	}

	@Override
	public String getRequestRemoteUser() {
		return request.getRemoteUser();
	}

	@Override
	public String getRequestScheme() {
		return request.getScheme();
	}

	@Override
	public String getRequestServerName() {
		return request.getServerName();
	}

	@Override
	public int getRequestServerPort() {
		return request.getServerPort();
	}

	@Override
	public String getRequestURI() {
		return request.getServletPath();
	}

	@Override
	public StringBuffer getRequestURL() {
		return request.getRequestURL();
	}

	@Override
	public Principal getRequestUserPrincipal() {
		return request.getUserPrincipal();
	}

	@Override
	public String getResponseHeader( String name ) {
		return response.getHeader( name );
	}

	@Override
	public Map<String, String[]> getResponseHeaderMap() {
		Map<String, String[]>	headers		= new HashMap<>();
		Collection<String>		headerNames	= response.getHeaderNames();
		for ( String name : headerNames ) {
			headers.put( name, response.getHeaders( name ).toArray( new String[ 0 ] ) );
		}
		return headers;
	}

	@Override
	public int getResponseStatus() {
		return response.getStatus();
	}

	@Override
	public PrintWriter getResponseWriter() {
		if ( writer == null ) {
			PrintWriter servletWriter;

			try {
				servletWriter = response.getWriter();
			} catch ( IOException e ) {
				throw new BoxRuntimeException( "Could not get response writer", e );
			} catch ( IllegalStateException e ) {
				// reponse has already been sent, so return a dummy writer
				servletWriter = new PrintWriter( NullWriter.INSTANCE );
			}
			writer = new WhitespaceManagingPrintWriter( servletWriter, boxContext.isWhitespaceCompressionEnabled() );
		}
		return writer;
	}

	@Override
	public void sendResponseBinary( byte[] data ) {
		resetResponseBuffer();
		try {
			response.getOutputStream().write( data );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Could not send binary response", e );
		}
	}

	@Override
	public void sendResponseFile( File file ) {
		resetResponseBuffer();
		try ( FileInputStream inputStream = new FileInputStream( file ) ) {
			FileChannel	channel	= inputStream.getChannel();
			ByteBuffer	buffer	= ByteBuffer.allocate( 1024 );
			while ( channel.read( buffer ) > 0 ) {
				buffer.flip();
				response.getOutputStream().write( buffer.array() );
				buffer.clear();
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Could not send file response", e );
		}
	}

	@Override
	public boolean isRequestSecure() {
		return request.isSecure();
	}

	@Override
	public void removeRequestAttribute( String name ) {
		request.removeAttribute( name );
	}

	@Override
	public void resetResponseBuffer() {
		try {
			response.resetBuffer();
		} catch ( IllegalStateException e ) {
			throw new BoxRuntimeException( "Could not reset response buffer", e );
		}
	}

	@Override
	public void setRequestAttribute( String name, Object value ) {
		request.setAttribute( name, value );
	}

	@Override
	public void setResponseHeader( String name, String value ) {
		response.setHeader( name, value );
	}

	@Override
	public void setResponseStatus( int sc ) {
		response.setStatus( sc );
	}

	@Override
	public void setResponseStatus( int sc, String sm ) {
		// The latest JakartaEE spec has removed the version of setStatus() which accepts a custom status message
		// as it's removed in the HTTP/2 and HTTP/3 specs. There is simply no way to do this any longer via the servlet API.
		response.setStatus( sc );
	}

	@Override
	public BoxCookie getRequestCookie( String name ) {
		var cookies = getRequestCookies();
		for ( BoxCookie cookie : cookies ) {
			if ( cookie.getName().equalsIgnoreCase( name ) ) {
				return cookie;
			}
		}
		return null;
	}

	@Override
	public boolean isResponseStarted() {
		return response.isCommitted();
	}

	@Override
	public void reset() {
		response.reset();
	}
}
