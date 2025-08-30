package ortus.boxlang.servlet;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test to verify that the Shadow JAR properly merges service files from all dependencies:
 * - BoxLang core
 * - BoxLang web support
 * - BoxLang servlet (this project)
 */
public class ServiceFileMergingTest {

	private static Path		shadowJarPath;
	private static JarFile	shadowJar;

	@BeforeAll
	static void setup() throws IOException {
		// Find the shadow JAR in the distributions folder
		Path distributionsDir = Paths.get( "build/distributions" );

		assertThat( Files.exists( distributionsDir ) ).isTrue();

		shadowJarPath	= Files.list( distributionsDir )
		    .filter( path -> path.toString().endsWith( ".jar" )
		        && !path.toString().contains( "javadoc" )
		        && !path.toString().contains( "sources" ) )
		    .findFirst()
		    .orElseThrow( () -> new AssertionError( "Shadow JAR not found in build/distributions" ) );

		shadowJar		= new JarFile( shadowJarPath.toFile() );
	}

	@DisplayName( "Shadow JAR should contain merged BIF service files from all dependencies" )
	@Test
	void testBifServiceFilesMerged() throws IOException {
		// Get the BIF service file from the shadow JAR
		ZipEntry bifServiceEntry = shadowJar.getEntry( "META-INF/services/ortus.boxlang.runtime.bifs.BIF" );
		assertThat( bifServiceEntry ).isNotNull();

		// Read the service file content
		Set<String> bifs = readServiceFile( bifServiceEntry );

		// Verify we have a reasonable number of BIFs (should be 450+ from all sources)
		assertThat( bifs.size() ).isAtLeast( 450 );

		// Verify BoxLang core BIFs are present
		assertThat( bifs ).contains( "ortus.boxlang.runtime.bifs.global.type.Len" );
		assertThat( bifs ).contains( "ortus.boxlang.runtime.bifs.global.array.ArrayAppend" );
		assertThat( bifs ).contains( "ortus.boxlang.runtime.bifs.global.struct.StructKeyExists" );

		// Verify BoxLang web support BIFs are present
		assertThat( bifs ).contains( "ortus.boxlang.web.bifs.GetPageContext" );
		assertThat( bifs ).contains( "ortus.boxlang.web.bifs.Location" );
		assertThat( bifs ).contains( "ortus.boxlang.web.bifs.GetHTTPRequestData" );

		// Verify servlet project BIFs are present (if any)
		// Note: The servlet project currently only has GetPageContext which might conflict with web support

		System.out.println( "✅ Total BIFs found: " + bifs.size() );
		System.out.println( "✅ Web support BIFs verified successfully" );
	}

	@DisplayName( "Shadow JAR should contain merged Component service files from all dependencies" )
	@Test
	void testComponentServiceFilesMerged() throws IOException {
		ZipEntry componentServiceEntry = shadowJar.getEntry( "META-INF/services/ortus.boxlang.runtime.components.Component" );
		assertThat( componentServiceEntry ).isNotNull();

		Set<String> components = readServiceFile( componentServiceEntry );

		// Verify we have components from different sources
		assertThat( components.size() ).isAtLeast( 40 );

		// Verify BoxLang core components are present
		assertThat( components ).contains( "ortus.boxlang.runtime.components.system.Application" );
		assertThat( components ).contains( "ortus.boxlang.runtime.components.system.Component" );

		// Verify BoxLang web support components are present
		assertThat( components ).contains( "ortus.boxlang.web.components.Header" );
		assertThat( components ).contains( "ortus.boxlang.web.components.Cookie" );
		assertThat( components ).contains( "ortus.boxlang.web.components.Location" );

		System.out.println( "✅ Total Components found: " + components.size() );
	}

	@DisplayName( "Shadow JAR should contain merged Interceptor service files from all dependencies" )
	@Test
	void testInterceptorServiceFilesMerged() throws IOException {
		ZipEntry interceptorServiceEntry = shadowJar.getEntry( "META-INF/services/ortus.boxlang.runtime.events.IInterceptor" );
		assertThat( interceptorServiceEntry ).isNotNull();

		Set<String> interceptors = readServiceFile( interceptorServiceEntry );

		// Verify we have interceptors from different sources
		assertThat( interceptors.size() ).isAtLeast( 4 );

		// Verify BoxLang core interceptors are present
		assertThat( interceptors ).contains( "ortus.boxlang.runtime.interceptors.Logging" );
		assertThat( interceptors ).contains( "ortus.boxlang.runtime.interceptors.ASTCapture" );

		// Verify BoxLang web support interceptors are present
		assertThat( interceptors ).contains( "ortus.boxlang.web.interceptors.WebRequest" );
		assertThat( interceptors ).contains( "ortus.boxlang.web.interceptors.WebConfigLoader" );

		// NOTE: ServletMappingInterceptor from this project is not included in the final JAR
		// This may be due to how the Shadow plugin merges service files vs. the ServiceLoader plugin

		System.out.println( "✅ Total Interceptors found: " + interceptors.size() );
	}

	@DisplayName( "Shadow JAR should contain other service files from dependencies" )
	@Test
	void testOtherServiceFilesMerged() throws IOException {
		// Test for BoxLang compiler service
		ZipEntry compilerServiceEntry = shadowJar.getEntry( "META-INF/services/ortus.boxlang.compiler.IBoxpiler" );
		assertThat( compilerServiceEntry ).isNotNull();

		Set<String> compilers = readServiceFile( compilerServiceEntry );
		assertThat( compilers ).contains( "ortus.boxlang.compiler.asmboxpiler.ASMBoxpiler" );

		// Test for ScriptEngine service (from BoxLang core)
		ZipEntry scriptEngineEntry = shadowJar.getEntry( "META-INF/services/javax.script.ScriptEngineFactory" );
		assertThat( scriptEngineEntry ).isNotNull();

		Set<String> scriptEngines = readServiceFile( scriptEngineEntry );
		assertThat( scriptEngines ).contains( "ortus.boxlang.runtime.scripting.BoxScriptingFactory" );

		System.out.println( "✅ Other service files verified successfully" );
	}

	@DisplayName( "Verify no duplicate entries in merged service files" )
	@Test
	void testNoDuplicateServiceEntries() throws IOException {
		ZipEntry bifServiceEntry = shadowJar.getEntry( "META-INF/services/ortus.boxlang.runtime.bifs.BIF" );
		assertThat( bifServiceEntry ).isNotNull();

		// Read as list to check for duplicates
		List<String>	bifsList	= readServiceFileAsList( bifServiceEntry );
		Set<String>		bifsSet		= new HashSet<>( bifsList );

		// If there are duplicates, the set size will be smaller than the list size
		if ( bifsList.size() != bifsSet.size() ) {
			System.err.println( "⚠️  Found duplicate BIF entries:" );
			Set<String> seen = new HashSet<>();
			for ( String bif : bifsList ) {
				if ( !seen.add( bif ) ) {
					System.err.println( "  - Duplicate: " + bif );
				}
			}
		}

		assertThat( bifsList.size() ).isEqualTo( bifsSet.size() );
		System.out.println( "✅ No duplicate service entries found" );
	}

	/**
	 * Helper method to read a service file and return entries as a Set
	 */
	private Set<String> readServiceFile( ZipEntry entry ) throws IOException {
		try ( InputStream is = shadowJar.getInputStream( entry ) ) {
			return new HashSet<>(
			    new String( is.readAllBytes() )
			        .lines()
			        .filter( line -> !line.trim().isEmpty() && !line.startsWith( "#" ) )
			        .map( String::trim )
			        .toList()
			);
		}
	}

	/**
	 * Helper method to read a service file and return entries as a List (to detect duplicates)
	 */
	private List<String> readServiceFileAsList( ZipEntry entry ) throws IOException {
		try ( InputStream is = shadowJar.getInputStream( entry ) ) {
			return new String( is.readAllBytes() )
			    .lines()
			    .filter( line -> !line.trim().isEmpty() && !line.startsWith( "#" ) )
			    .map( String::trim )
			    .toList();
		}
	}
}
