package org.digitalmodular.fluidqoi;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.nio.ByteBuffer;
import java.util.Objects;

import org.digitalmodular.fluidqoi.core.FluidQOI555Encoder;
import org.digitalmodular.fluidqoi.core.FluidQOI565Encoder;
import org.digitalmodular.fluidqoi.core.FluidQOI8888Encoder;
import org.digitalmodular.fluidqoi.core.FluidQOI888Encoder;
import org.digitalmodular.fluidqoi.core.FluidQOIEncoder;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-16
public final class FluidQOIImageEncoder {
	@SuppressWarnings("CharUsedInArithmeticContext")
	public static final int     FLUID_QOI_MAGIC = 'f' << 24 |
	                                              'q' << 16 |
	                                              'o' << 8 |
	                                              'i';
	@SuppressWarnings({"PublicField", "StaticNonFinalField"})
	public static       boolean debugging       = false;

	public static final int HEADER_LENGTH = 16;

	private final FluidQOIConfig config;

	public FluidQOIImageEncoder(FluidQOIConfig config) {
		this.config = config;
	}

	/**
	 * @return A new ByteBuffer, backed by a heap array (with royally overestimated capacity), ready to be read.
	 */
	public ByteBuffer encode(BufferedImage image) {
		Objects.requireNonNull(image, "image");

		FluidQOIFormat  format  = determineFormat(image);
		FluidQOIEncoder encoder = makeEncoder(format);

		encoder.beginEncoding(image.getWidth(), image.getHeight(), format);
		encodeImage(image, encoder);
		return encoder.finishEncoding();
	}

	private FluidQOIFormat determineFormat(BufferedImage image) {
		if (config.getFormatOverride() != null) {
			return config.getFormatOverride();
		}

		// TODO Do actual image analysis to decide the "smallest" required format

		switch (image.getType()) {
			case BufferedImage.TYPE_INT_RGB:
			case BufferedImage.TYPE_INT_BGR:
			case BufferedImage.TYPE_3BYTE_BGR:
			case BufferedImage.TYPE_BYTE_GRAY:
			case BufferedImage.TYPE_USHORT_GRAY:
			case BufferedImage.TYPE_BYTE_BINARY:
			case BufferedImage.TYPE_BYTE_INDEXED:
				return FluidQOIFormat.RGB888;
			case BufferedImage.TYPE_INT_ARGB:
			case BufferedImage.TYPE_4BYTE_ABGR:
				return FluidQOIFormat.RGBA8888;
			case BufferedImage.TYPE_USHORT_565_RGB:
				return FluidQOIFormat.RGB565;
			case BufferedImage.TYPE_USHORT_555_RGB:
				return FluidQOIFormat.RGB555;
			default:
				break;
		}

		boolean hasAlpha = image.getColorModel().hasAlpha();
		return hasAlpha ? FluidQOIFormat.RGBA8888 : FluidQOIFormat.RGB888;
	}

	private FluidQOIEncoder makeEncoder(FluidQOIFormat format) {
		switch (format.getEncoder()) {
			case 3:
				return new FluidQOI888Encoder(config);
			case 4:
				return new FluidQOI8888Encoder(config);
			case 5:
				return new FluidQOI555Encoder(config);
			case 6:
				return new FluidQOI565Encoder(config);
			default:
				throw new AssertionError("Unimplemented encoder type: " + format.getEncoder() + " (" + format + ')');
		}
	}

	private static void encodeImage(RenderedImage image, FluidQOIEncoder encoder) {
		boolean hasAlpha  = image.getColorModel().hasAlpha();
		int     imageType = hasAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;

		BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), imageType);

		Graphics2D g = convertedImage.createGraphics();
		try {
			g.drawRenderedImage(image, new AffineTransform());
		} finally {
			g.dispose();
		}

		encodeComponentColorModelImage(convertedImage.getRaster(), hasAlpha, encoder);
	}

	private static void encodeComponentColorModelImage(Raster raster, boolean hasAlpha, FluidQOIEncoder encoder) {
		byte[] samples     = ((DataBufferByte)raster.getDataBuffer()).getData();
		int[]  bandOffsets = ((ComponentSampleModel)raster.getSampleModel()).getBandOffsets();

		int p = 0;
		if (hasAlpha) {
			while (p < samples.length) {
				byte r = samples[p + bandOffsets[0]];
				byte g = samples[p + bandOffsets[1]];
				byte b = samples[p + bandOffsets[2]];
				byte a = samples[p + bandOffsets[3]];
				encoder.encodePixel(r, g, b, a);
				p += 4;
			}
		} else {
			while (p < samples.length) {
				byte r = samples[p + bandOffsets[0]];
				byte g = samples[p + bandOffsets[1]];
				byte b = samples[p + bandOffsets[2]];
				encoder.encodePixel(r, g, b, (byte)255);
				p += 3;
			}
		}
	}
}
