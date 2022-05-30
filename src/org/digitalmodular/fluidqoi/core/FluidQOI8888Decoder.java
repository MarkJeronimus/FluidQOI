package org.digitalmodular.fluidqoi.core;

import java.awt.image.BufferedImage;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-25 Split from FluidQOIEncoder
public class FluidQOI8888Decoder extends FluidQOIInterleavedByteDecoder {
	@SuppressWarnings("AssignmentToSuperclassField")
	public FluidQOI8888Decoder() {
		super(FluidQOI8888Encoder.OP_REPEAT);

		opLuma4444 = FluidQOI8888Encoder.OP_LUMA4444;
		opMask4 = FluidQOI8888Encoder.OP_MASK4;
		opLuma222 = FluidQOI8888Encoder.OP_LUMA222;
		opLuma644 = FluidQOI8888Encoder.OP_LUMA644;
	}

	@Override
	protected BufferedImage createImage(int width, int height) {
		return new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
	}

	@Override
	protected void decodeImageImplImpl(byte[] pixels) {
		int lastCode = -1;

		int p = 0;
		while (true) {
			int     repeatCount             = 1;
			boolean doRecordRecentColor     = true;
			boolean doResetRepeatMultiplier = true;

			int code = in.get() & 0xFF;

			if (code >= opIndex) {
				readOpIndex(code);

				doRecordRecentColor = false;
				if (lastCode == 0 && code == 0) { // End code
					break;
				}
			} else if (code >= opRepeat) {
				repeatCount = readOpRepeat(code);

				doRecordRecentColor = p == 0;
				doResetRepeatMultiplier = false;
			} else if (code >= opLuma4444) {
				readOpLuma4444(code);
			} else if (code >= opMask4) {
				readOpMask4(code);
			} else if (code >= opLuma222) {
				readOpLuma222(code);
			} else /*if (code >= opLuma644)*/ {
				readOpLuma644(code);
			}

			if (repeatCount * 4 > pixels.length - p) {
				repeatCount = (pixels.length - p) / 4;
			}

			do {
				pixels[p++] = lastA;
				pixels[p++] = lastB;
				pixels[p++] = lastG;
				pixels[p++] = lastR;

				repeatCount--;
			} while (repeatCount > 0);

			if (p == pixels.length || in.remaining() < FluidQOI8888Encoder.LONGEST_OP) {
				break;
			}

			if (doRecordRecentColor) {
				recordRecentColor();
			}

			if (doResetRepeatMultiplier) {
				resetRepeatMultiplier();
			}

			lastCode = code;
		}
	}
}
