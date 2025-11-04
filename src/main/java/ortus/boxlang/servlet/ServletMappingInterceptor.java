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
		String	path			= data.getAsString( Key.path );
		// Check if path contains "..". If so, get the path leading up to the first ".." and then resolve the rest of the path against that
		// This is because the servlet's getRealPath() will not allow you to back up "above" the web root.
		int		dotDotIndex		= path.indexOf( ".." );
		String	realPath		= null;

		Path	resolvedPath	= null;

		String	mappingName;
		String	mappingPath;

		// We need special handling for paths with ".." in them
		if ( dotDotIndex >= 0 ) {
			// Break off the part before ".."
			String beforeDotDot = path.substring( 0, dotDotIndex );
			realPath = servletContext.getRealPath( beforeDotDot );
			if ( realPath != null ) {
				// Process the rest of the path relative to realPath
				String afterDotDot = path.substring( dotDotIndex );
				resolvedPath = Path.of( realPath ).resolve( afterDotDot ).normalize();
			}
		} else {
			// Paths with out .. are simpler
			realPath = servletContext.getRealPath( path );
			if ( realPath != null ) {
				resolvedPath = Path.of( realPath ).normalize();
			}
		}

		// if resolvedPath is non-null here, then it means we have a match. The logic for determining the mapping name/path is the same
		if ( resolvedPath != null ) {
			// The web root for this site
			String	rootPath		= servletContext.getRealPath( "/" );
			Path	relativePath	= Path.of( path ).normalize();

			// If the resolved path is inside the web root, then we'll assume the root mapping
			if ( resolvedPath.startsWith( rootPath ) ) {
				mappingName	= "/";
				mappingPath	= rootPath;
			} else {
				// If it's outside the web root, then there really is no mapping to use, so we'll just use the parent folder of the file
				Path	relativePathParent	= relativePath.getParent();
				Path	resolvedPathParent	= resolvedPath.getParent();
				mappingName	= relativePathParent != null ? relativePathParent.toString() : "/";
				mappingPath	= resolvedPathParent != null ? resolvedPathParent.toString() : resolvedPath.getRoot().toString();
			}
			/*
			 * 
			 * Ex:
			 * mappingName /tests/
			 * mappingPath C:\sandbox\appTemplate\tests\
			 * relativePath /tests/runner.bxm
			 * absolutePath C:\sandbox\appTemplate\tests\runner.bxm
			 * 
			 * It's important that the absolute path is actually under the mapping path.
			 */

			data.put( Key.resolvedFilePath,
			    // The servlet already makes the path "real", so we can use ofReal() for better performance
			    ResolvedFilePath.ofReal(
			        mappingName,
			        mappingPath,
			        relativePath.toString(),
			        resolvedPath
			    )
			);
		}
	}

}
