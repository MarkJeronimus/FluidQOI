package org.digitalmodular.fluidqoi.core;

import java.awt.image.BufferedImage;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-22
public class FluidQOI555Decoder extends FluidQOIPackedShortDecoder {
	@SuppressWarnings("AssignmentToSuperclassField")
	public FluidQOI555Decoder() {
		super(FluidQOI555Encoder.OP_REPEAT);

		opLuma222 = FluidQOI555Encoder.OP_LUMA222;
		opRGB555 = FluidQOI555Encoder.OP_RGB555;
	}

	@Override
	protected BufferedImage createImage(int width, int height) {
		return new BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB);
	}

	@Override
	protected void decodeImageImplImpl(short[] pixels) {
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
			} else if (code >= opLuma222) {
				readOpLuma222(code);
			} else /*if (code >= opRGB555)*/ {
				readOpRGB555(code);
			}

			if (repeatCount > pixels.length - p) {
				repeatCount = (pixels.length - p) / 4;
			}

			short shortRGB = lastRGB;
			do {
				pixels[p++] = shortRGB;

				repeatCount--;
			} while (repeatCount > 0);

			if (p == pixels.length || in.remaining() < FluidQOI555Encoder.LONGEST_OP) {
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
