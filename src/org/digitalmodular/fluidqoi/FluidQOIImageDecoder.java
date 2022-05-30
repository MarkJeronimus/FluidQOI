package org.digitalmodular.fluidqoi;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.digitalmodular.fluidqoi.core.FluidQOI555Decoder;
import org.digitalmodular.fluidqoi.core.FluidQOI565Decoder;
import org.digitalmodular.fluidqoi.core.FluidQOI8888Decoder;
import org.digitalmodular.fluidqoi.core.FluidQOI888Decoder;
import org.digitalmodular.fluidqoi.core.FluidQOIDecoder;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-14
@SuppressWarnings("ConstantConditions")
public class FluidQOIImageDecoder {
	private int            width  = 0;
	private int            height = 0;
	private FluidQOIFormat format;
	private int            indexLength;

	public BufferedImage decode(ByteBuffer in) throws IOException {
		readHeader(in);

		FluidQOIDecoder decoder = makeDecoder();

		return decoder.decode(in, width, height, format, indexLength);
	}

	private void readHeader(ByteBuffer in) throws IOException {
		int magic = in.getInt();

		if (magic != FluidQOIImageEncoder.FLUID_QOI_MAGIC) { // "fqoi" in big-endian
			throw new IOException("Bad 'magic': " + Integer.toString(magic, 16));
		}

		width = in.getInt();
		if (width < 0 || width > 32768) {
			throw new IOException("Bad width: " + width);
		}

		height = in.getInt();
		if (height < 0 || height > 32768) {
			throw new IOException("Bad height: " + height);
		}

		int formatCode = in.get() & 0xFF;
		format = FluidQOIFormat.fromCode(formatCode);
		if (format == null) {
			throw new IOException("Bad format: " + formatCode);
		}

		indexLength = in.get() & 0xFF;
		if (indexLength == 0 || indexLength == 255) {
			throw new IOException("Bad indexLength: " + indexLength);
		}

		in.get(); // Format-dependent extension 1 (unused here)
		in.get(); // Format-dependent extension 2 (unused here)
	}

	private FluidQOIDecoder makeDecoder() {
		switch (format.getEncoder()) {
			case 3:
				return new FluidQOI888Decoder();
			case 4:
				return new FluidQOI8888Decoder();
			case 5:
				return new FluidQOI555Decoder();
			case 6:
				return new FluidQOI565Decoder();
			default:
				throw new AssertionError("Unimplemented decoder type: " + format.getEncoder() + " (" + format + ')');
		}
	}
}
