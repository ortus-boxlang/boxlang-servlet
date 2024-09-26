package ortus.boxlang.web.exchange;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class CachedBodyServletInputStream extends ServletInputStream {

	private InputStream cachedBodyInputStream;

	public CachedBodyServletInputStream( byte[] cachedBody ) {
		this.cachedBodyInputStream = new ByteArrayInputStream( cachedBody );
	}

	@Override
	public int read() throws IOException {
		return cachedBodyInputStream.read();
	}

	@Override
	public boolean isFinished() {
		try {
			return cachedBodyInputStream.available() == 0;
		} catch ( IOException e ) {
			throw new BoxRuntimeException( e.getMessage(), e );
		}
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setReadListener( ReadListener readListener ) {
		// Do nothing
	}
}