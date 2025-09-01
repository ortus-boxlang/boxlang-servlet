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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.el.ELContext;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;

/**
 * BoxPageContext provides a custom implementation of Jakarta Servlet's PageContext
 * for the BoxLang servlet environment.
 *
 * This class manages page-scoped attributes and provides access to various servlet
 * contexts including request, response, session, and application scopes. It serves
 * as a bridge between the BoxLang runtime and the Jakarta Servlet API.
 *
 * Key features:
 * - Manages page-scoped attributes in a local HashMap
 * - Provides attribute access across all four scopes (page, request, session, application)
 * - Supports attribute searching with findAttribute() following scope hierarchy
 * - Handles servlet request forwarding and including
 * - Implements proper resource cleanup via release() method
 *
 * Scope hierarchy for attribute resolution:
 * 1. Page scope (local HashMap)
 * 2. Request scope
 * 3. Session scope (if session exists)
 * 4. Application scope
 *
 * Note: Some methods like getELContext() and handlePageException() are not yet
 * implemented and will throw UnsupportedOperationException.
 *
 * @author Ortus Solutions, Corp
 *
 * @since 1.0.0
 */
public class BoxPageContext extends PageContext {

	private final Map<String, Object>	pageAttributes	= new HashMap<>();
	private ServletRequest				request;
	private ServletResponse				response;
	private HttpSession					session;
	private ServletContext				application;
	private Object						page;
	private JspWriter					out;
	private Exception					exception;
	private ServletConfig				servletConfig;

	@Override
	public ELContext getELContext() {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public JspWriter getOut() {
		return out;
	}

	@Override
	public HttpSession getSession() {
		return session;
	}

	@Override
	public Object getAttribute( String name, int scope ) {
		switch ( scope ) {
			case PAGE_SCOPE :
				return pageAttributes.get( name );
			case REQUEST_SCOPE :
				return request.getAttribute( name );
			case SESSION_SCOPE :
				return session.getAttribute( name );
			case APPLICATION_SCOPE :
				return application.getAttribute( name );
			default :
				throw new IllegalArgumentException( "Invalid scope: " + scope );
		}
	}

	@Override
	public Enumeration<String> getAttributeNamesInScope( int scope ) {
		switch ( scope ) {
			case PAGE_SCOPE :
				return java.util.Collections.enumeration( pageAttributes.keySet() );
			case REQUEST_SCOPE :
				return request.getAttributeNames();
			case SESSION_SCOPE :
				return session.getAttributeNames();
			case APPLICATION_SCOPE :
				return application.getAttributeNames();
			default :
				throw new IllegalArgumentException( "Invalid scope: " + scope );
		}
	}

	@Override
	public int getAttributesScope( String name ) {
		if ( pageAttributes.containsKey( name ) ) {
			return PAGE_SCOPE;
		} else if ( request.getAttribute( name ) != null ) {
			return REQUEST_SCOPE;
		} else if ( session != null && session.getAttribute( name ) != null ) {
			return SESSION_SCOPE;
		} else if ( application.getAttribute( name ) != null ) {
			return APPLICATION_SCOPE;
		} else {
			return 0;
		}
	}

	@Override
	public void setAttribute( String name, Object value ) {
		pageAttributes.put( name, value );
	}

	@Override
	public void setAttribute( String name, Object value, int scope ) {
		switch ( scope ) {
			case PAGE_SCOPE :
				pageAttributes.put( name, value );
				break;
			case REQUEST_SCOPE :
				request.setAttribute( name, value );
				break;
			case SESSION_SCOPE :
				session.setAttribute( name, value );
				break;
			case APPLICATION_SCOPE :
				application.setAttribute( name, value );
				break;
			default :
				throw new IllegalArgumentException( "Invalid scope: " + scope );
		}
	}

	@Override
	public Object getAttribute( String name ) {
		return pageAttributes.get( name );
	}

	@Override
	public Object findAttribute( String name ) {
		Object value = pageAttributes.get( name );
		if ( value == null ) {
			value = request.getAttribute( name );
		}
		if ( value == null && session != null ) {
			value = session.getAttribute( name );
		}
		if ( value == null ) {
			value = application.getAttribute( name );
		}
		return value;
	}

	@Override
	public void removeAttribute( String name ) {
		pageAttributes.remove( name );
	}

	@Override
	public void removeAttribute( String name, int scope ) {
		switch ( scope ) {
			case PAGE_SCOPE :
				pageAttributes.remove( name );
				break;
			case REQUEST_SCOPE :
				request.removeAttribute( name );
				break;
			case SESSION_SCOPE :
				session.removeAttribute( name );
				break;
			case APPLICATION_SCOPE :
				application.removeAttribute( name );
				break;
			default :
				throw new IllegalArgumentException( "Invalid scope: " + scope );
		}
	}

	@Override
	public void initialize( Servlet servlet, ServletRequest request, ServletResponse response, String errorPageURL, boolean needsSession, int bufferSize,
	    boolean autoFlush ) {
		this.request		= request;
		this.response		= response;
		this.application	= request.getServletContext();
		this.session		= null;
	}

	@Override
	public void release() {
		pageAttributes.clear();
		request			= null;
		response		= null;
		session			= null;
		application		= null;
		page			= null;
		out				= null;
		exception		= null;
		servletConfig	= null;
	}

	@Override
	public Object getPage() {
		return page;
	}

	@Override
	public ServletRequest getRequest() {
		return request;
	}

	@Override
	public ServletResponse getResponse() {
		return response;
	}

	@Override
	public Exception getException() {
		return exception;
	}

	@Override
	public ServletConfig getServletConfig() {
		return servletConfig;
	}

	@Override
	public ServletContext getServletContext() {
		return application;
	}

	@Override
	public void forward( String path ) throws ServletException, IOException {
		request.getRequestDispatcher( path ).forward( request, response );
	}

	@Override
	public void include( String path ) throws ServletException, IOException {
		request.getRequestDispatcher( path ).include( request, response );
	}

	@Override
	public void include( String path, boolean flush ) throws ServletException, IOException {
		if ( flush ) {
			// out.flush();
		}
		request.getRequestDispatcher( path ).include( request, response );
	}

	@Override
	public void handlePageException( Exception e ) throws ServletException, IOException {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public void handlePageException( Throwable t ) throws ServletException, IOException {
		throw new UnsupportedOperationException( "Not supported yet." );
	}
}