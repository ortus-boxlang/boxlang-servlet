package ortus.boxlang.web.exchange;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.annotation.Nullable;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

	private static final byte[]	EMPTY_CONTENT	= new byte[ 0 ];

	private byte[]				cachedBody;

	public CachedBodyHttpServletRequest( HttpServletRequest request ) throws IOException {
		super( request );
		InputStream requestInputStream = request.getInputStream();
		this.cachedBody = copyToByteArray( requestInputStream );
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return new CachedBodyServletInputStream( this.cachedBody );
	}

	@Override
	public BufferedReader getReader() throws IOException {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( this.cachedBody );
		return new BufferedReader( new InputStreamReader( byteArrayInputStream ) );
	}

	/**
	 * Copy the contents of the given InputStream into a new byte array.
	 * <p>
	 * Leaves the stream open when done.
	 * 
	 * @param in the stream to copy from (may be {@code null} or empty)
	 * 
	 * @return the new byte array that has been copied to (possibly empty)
	 * 
	 * @throws IOException in case of I/O errors
	 */
	public static byte[] copyToByteArray( @Nullable InputStream in ) throws IOException {
		if ( in == null ) {
			return EMPTY_CONTENT;
		}

		return in.readAllBytes();
	}

}
