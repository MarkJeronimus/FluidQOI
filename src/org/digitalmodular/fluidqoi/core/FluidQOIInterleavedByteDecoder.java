package org.digitalmodular.fluidqoi.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;

import org.digitalmodular.fluidqoi.FluidQOIFormat;
import org.digitalmodular.fluidqoi.FluidQOIImageDecoder;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-29
public abstract class FluidQOIInterleavedByteDecoder extends FluidQOIDecoder {
	protected byte     lastR            = 0;
	protected byte     lastG            = 0;
	protected byte     lastB            = 0;
	protected byte     lastA            = (byte)255;
	private   byte[][] recentColorsList = null;

	protected FluidQOIInterleavedByteDecoder(int opRepeat) {
		super(opRepeat);
	}

	@Override
	protected BufferedImage decodeImageImpl(int width, int height, FluidQOIFormat format) {
		resetDecoderState();

		BufferedImage image  = createImage(width, height);
		byte[]        pixels = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();

		decodeImageImplImpl(pixels);

		return image;
	}

	protected abstract BufferedImage createImage(int width, int height);

	@Override
	protected void resetDecoderState() {
		super.resetDecoderState();

		lastR = 0;
		lastG = 0;
		lastB = 0;
		lastA = (byte)255;
		recentColorsList = new byte[indexLength][4];

		if (indexLength > 1) {
			Arrays.fill(recentColorsList[1], (byte)0xFF);
		}
	}

	protected abstract void decodeImageImplImpl(byte[] pixels);

	protected void recordRecentColor() {
		recentColorsList[recentColorsIndex][0] = lastR;
		recentColorsList[recentColorsIndex][1] = lastG;
		recentColorsList[recentColorsIndex][2] = lastB;
		recentColorsList[recentColorsIndex][3] = lastA;
		recentColorsIndex = (recentColorsIndex + 1) % indexLength;
	}

	protected void readOpIndex(int data) {
		int index = data - opIndex;

		if (FluidQOIImageDecoder.debugging) {
			statistics.recordOpIndex(data, index);
		}

		byte[] c = recentColorsList[index];
		lastR = c[0];
		lastG = c[1];
		lastB = c[2];
		lastA = c[3];
	}

	protected void readOpLuma222(int data) {
		int value = data - opLuma222;

		int dy = ((value & 0b110000) << 26) >> 30;
		int du = ((value & 0b001100) << 28) >> 30;
		int dv = ((value & 0b000011) << 30) >> 30;

		if (FluidQOIImageDecoder.debugging) {
			statistics.recordOpLuma222(data, dy, du, dv);
		}

		lastR += dy + du;
		lastG += dy;
		lastB += dy + dv;
	}

	protected void readOpLuma644(int data1) {
		int value = data1 - opLuma644;
		int data2 = in.get();

		int dy = ((value & 0b111111) << 26) >> 26;
		int du = ((data2 & 0b11110000) << 24) >> 28;
		int dv = ((data2 & 0b00001111) << 28) >> 28;

		if (FluidQOIImageDecoder.debugging) {
			statistics.recordOpLuma644(data1, data2, dy, du, dv);
		}

		lastR += dy + du;
		lastG += dy;
		lastB += dy + dv;
	}

	protected void readOpLuma4444(int data1) {
		byte data2 = in.get();
		byte data3 = in.get();

		int dy = ((data2 & 0b11110000) << 24) >> 28;
		int du = ((data2 & 0b00001111) << 28) >> 28;
		int dv = ((data3 & 0b11110000) << 24) >> 28;
		int da = ((data3 & 0b00001111) << 28) >> 28;

		if (FluidQOIImageDecoder.debugging) {
			statistics.recordOpLuma4444(data1, data2, data3, dy, du, dv, da);
		}

		lastR += dy + du;
		lastG += dy;
		lastB += dy + dv;
		lastA += da;
	}

	protected void readOpMask4(int data) {
		int mask = data - opMask4;

		if ((mask & 0b1000) != 0) {
			lastR = in.get();
		}

		if ((mask & 0b0100) != 0) {
			lastG = in.get();
		}

		if ((mask & 0b0010) != 0) {
			lastB = in.get();
		}

		if ((mask & 0b0001) != 0) {
			lastA = in.get();
		}

		if (FluidQOIImageDecoder.debugging) {
			statistics.recordOpMask4(data, mask, lastR, lastG, lastB, lastA);
		}
	}
}
