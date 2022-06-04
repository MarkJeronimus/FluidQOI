package org.digitalmodular.fluidqoi.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferShort;

import org.digitalmodular.fluidqoi.FluidQOIFormat;
import org.digitalmodular.fluidqoi.FluidQOIImageEncoder;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-29
public abstract class FluidQOIPackedShortDecoder extends FluidQOIDecoder {
	protected short   lastRGB           = 0;
	private   short[] recentColorsList  = null;
	private   int     recentColorsIndex = 0;

	protected FluidQOIPackedShortDecoder(int opRepeat) {
		super(opRepeat);
	}

	@Override
	protected BufferedImage decodeImageImpl(int width, int height, FluidQOIFormat format) {
		resetDecoderState();

		BufferedImage image  = createImage(width, height);
		short[]       pixels = ((DataBufferShort)image.getRaster().getDataBuffer()).getData();

		decodeImageImplImpl(pixels);

		return image;
	}

	protected abstract BufferedImage createImage(int width, int height);

	@Override
	protected void resetDecoderState() {
		super.resetDecoderState();

		lastRGB = 0;
		recentColorsList = new short[indexLength];
	}

	protected abstract void decodeImageImplImpl(short[] pixels);

	protected void recordRecentColor() {
		recentColorsList[recentColorsIndex] = lastRGB;
		recentColorsIndex = (recentColorsIndex + 1) % indexLength;
	}

	protected void readOpIndex(int data) {
		int index = data - opIndex;

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpIndex(data, index);
		}

		lastRGB = recentColorsList[index - opIndex];
	}

	protected void readOpLuma222(int data) {
		int value = data - opLuma222 + 1;

		int dy = (value & 0b110000) << 26 >> 3; // 5 bits, left-aligned
		int du = (value & 0b001100) << 28 >> 3; // 5 bits, left-aligned
		int dv = (value & 0b000011) << 30 >> 3; // 5 bits, left-aligned

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpLuma222(data, dy >> 27, du >> 27, dv >> 27);
		}

		int r = (lastRGB & 0b01111100_00000000) << 17; // 5 bits, left-aligned
		int g = (lastRGB & 0b00000011_11100000) << 22; // 5 bits, left-aligned
		int b = (lastRGB & 0b00000000_00011111) << 27; // 5 bits, left-aligned
		r = (r + dy + du) >>> 17;
		g = (g + dy) >>> 22;
		b = (b + dy + dv) >>> 27;
		lastRGB = (short)(r | g | b);
	}

	protected void readOpLuma322(int data) {
		int value = data - opLuma322 + 1;

		int dy = (value & 0b1110000) << 25 >> 3; // 6 bits, left-aligned
		int du = (value & 0b0001100) << 28 >> 4; // 6 bits, left-aligned
		int dv = (value & 0b0000011) << 30 >> 4; // 6 bits, left-aligned

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpLuma322(data, dy >> 26, du >> 26, dv >> 26);
		}

		int r = (lastRGB & 0b11111000_00000000) << 16; // 5 bits, left-aligned
		int g = (lastRGB & 0b00000111_11100000) << 21; // 6 bits, left-aligned
		int b = (lastRGB & 0b00000000_00011111) << 27; // 5 bits, left-aligned
		r = (r + du) >>> 16;
		g = (g + dy) >>> 21;
		b = (b + dv) >>> 27;
		lastRGB = (short)(r | g | b);
	}

	protected void readOpLuma433(int data1) {
		int value = data1 - opLuma433;
		int data2 = in.get();

		int dy = (((value & 0b00000011 << 30) | (data2 & 0b11000000) << 22)) >> 2; // 6 bits, left-aligned
		int du = ((data2 & 0b00111000) << 26) >> 3;                                // 6 bits, left-aligned
		int dv = ((data2 & 0b00000111) << 29) >> 3;                                // 6 bits, left-aligned

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpLuma433(data1, data2, dy >> 26, du >> 26, dv >> 26);
		}

		int r = (lastRGB & 0b11111000_00000000) << 16; // 5 bits, left-aligned
		int g = (lastRGB & 0b00000111_11100000) << 21; // 6 bits, left-aligned
		int b = (lastRGB & 0b00000000_00011111) << 27; // 5 bits, left-aligned
		r = (r + du) >>> 16;
		g = (g + dy) >>> 21;
		b = (b + dv) >>> 27;
		lastRGB = (short)(r | g | b);
	}

	protected void readOpRGB555(int data1) {
		int value = data1 - opRGB555;
		int data2 = in.get() & 0xFF;

		lastRGB = (short)((value << 8) | data2);

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpRGB555(data1, data2, lastRGB);
		}
	}

	protected void readOpRGB565() {
		int data2 = in.get() & 0xFF;
		int data3 = in.get() & 0xFF;

		lastRGB = (short)((data2 << 8) | data3);

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpRGB565(opRGB565, data2, data3, lastRGB);
		}
	}
}
