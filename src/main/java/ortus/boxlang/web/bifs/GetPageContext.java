/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.web.bifs;

import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.web.context.WebRequestBoxContext;
import ortus.boxlang.web.exchange.BoxHTTPServletExchange;

@BoxBIF
public class GetPageContext extends BIF {

	private static final Key page_context_attachment = Key.of( "page_context_attachment" );

	/**
	 * Constructor
	 */
	public GetPageContext() {
		super();
	}

	/**
	 *
	 * Gets the current java PageContext object that provides access to page attributes and configuration, request and response objects.
	 * If not running in a servlet, this will be a fake class attempting to provide most of the common methods.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		WebRequestBoxContext requestContext = context.getParentOfType( WebRequestBoxContext.class );
		// Create if neccessary
		if ( !requestContext.hasAttachment( page_context_attachment ) ) {
			synchronized ( requestContext ) {
				// Double check lock pattern
				if ( !requestContext.hasAttachment( page_context_attachment ) ) {
					// Create a PageContext object
					BoxHTTPServletExchange	exchange	= ( BoxHTTPServletExchange ) requestContext.getHTTPExchange();
					JspFactory				jspFactory	= JspFactory.getDefaultFactory();

					// Ensure the JspFactory is available (it should be in a servlet container)
					if ( jspFactory != null ) {
						// Create a PageContext for this request using the container's JSP engine
						PageContext pageContext = jspFactory.getPageContext(
						    exchange.getServlet(),
						    exchange.getServletRequest(),
						    exchange.getServletResponse(),
						    null,
						    true,
						    JspWriter.DEFAULT_BUFFER,
						    true
						);
						// Attach the PageContext to the request context so it's available for the duration of the request
						requestContext.putAttachment( page_context_attachment, pageContext );
					} else {
						throw new BoxRuntimeException( "JspFactory is not available. This BIF can only be used in a servlet container." );
					}
				}
			}
		}
		// Return the PageContext object which is now attached to the request context
		return requestContext.getAttachment( page_context_attachment );
	}

}
