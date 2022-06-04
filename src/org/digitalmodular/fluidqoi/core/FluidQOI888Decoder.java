package org.digitalmodular.fluidqoi.core;

import java.awt.image.BufferedImage;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-25 Split from FluidQOIEncoder
public class FluidQOI888Decoder extends FluidQOIInterleavedByteDecoder {
	@SuppressWarnings("AssignmentToSuperclassField")
	public FluidQOI888Decoder() {
		super(FluidQOI888Encoder.OP_REPEAT);

		opMask3 = FluidQOI888Encoder.OP_MASK3;
		opLuma222 = FluidQOI888Encoder.OP_LUMA222;
		opLuma644 = FluidQOI888Encoder.OP_LUMA644;
	}

	@Override
	protected BufferedImage createImage(int width, int height) {
		return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
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
			} else if (code >= opMask3) {
				readOpMask3(code);
			} else if (code >= opLuma222) {
				readOpLuma222(code);
			} else /*if (code >= opLuma644)*/ {
				readOpLuma644(code);
			}

			if (repeatCount * 4 > pixels.length - p) {
				repeatCount = (pixels.length - p) / 4;
			}

			do {
				pixels[p++] = lastB;
				pixels[p++] = lastG;
				pixels[p++] = lastR;

				repeatCount--;
			} while (repeatCount > 0);

			if (p == pixels.length || in.remaining() < FluidQOI888Encoder.LONGEST_OP) {
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
