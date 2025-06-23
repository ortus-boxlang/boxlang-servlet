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

import java.nio.file.Path;

import jakarta.servlet.ServletContext;
import ortus.boxlang.runtime.events.BaseInterceptor;
import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.events.Interceptor;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * I allow paths to be expanded using the servlets mappings/resource manager
 */
@Interceptor( autoLoad = false )
public class ServletMappingInterceptor extends BaseInterceptor {

	private ServletContext servletContext;

	/**
	 * No Arg-Constructor
	 */
	public ServletMappingInterceptor() {
	}

	/**
	 * Constructor
	 */
	public ServletMappingInterceptor( ServletContext servletContext ) {
		this.servletContext = servletContext;
	}

	/**
	 * Listen to the "onMissingMapping" event
	 */
	@InterceptionPoint
	public void onMissingMapping( IStruct data ) {
		String	path		= data.getAsString( Key.path );
		// Check if path contains "..". If so, get the path leaing up to the first ".." and then resolve the rest of the path against that
		// This is because the servlet's getRealPath() will not allow you to back up "above" the web root.
		int		dotDotIndex	= path.indexOf( ".." );
		String	realPath	= null;
		if ( dotDotIndex >= 0 ) {
			// Break off the part before ".."
			String beforeDotDot = path.substring( 0, dotDotIndex );
			realPath = servletContext.getRealPath( beforeDotDot );
			if ( realPath != null ) {
				// Process the rest of the path relative to realPath
				String	afterDotDot	= path.substring( dotDotIndex );
				Path	resolved	= Path.of( realPath ).resolve( afterDotDot ).normalize();
				data.put( Key.resolvedFilePath,
				    ResolvedFilePath.of( "/", servletContext.getRealPath( "/" ), Path.of( path ).normalize().toString(), resolved )
				);
			}
			return;
		}
		// Fallback to normal processing
		realPath = servletContext.getRealPath( path );
		if ( realPath != null ) {
			data.put( Key.resolvedFilePath,
			    ResolvedFilePath.of( "/", servletContext.getRealPath( "/" ), Path.of( path ).normalize().toString(), Path.of( realPath ).normalize() )
			);
		}
	}

}
