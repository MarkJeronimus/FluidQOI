package org.digitalmodular.fluidqoi.core;

import java.awt.image.BufferedImage;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-22
public class FluidQOI565Decoder extends FluidQOIPackedShortDecoder {
	@SuppressWarnings("AssignmentToSuperclassField")
	public FluidQOI565Decoder() {
		super(FluidQOI565Encoder.OP_REPEAT);

		opRGB565 = FluidQOI565Encoder.OP_RGB565;
		opLuma322 = FluidQOI565Encoder.OP_LUMA322;
		opLuma433 = FluidQOI565Encoder.OP_LUMA433;
	}

	@Override
	protected BufferedImage createImage(int width, int height) {
		return new BufferedImage(width, height, BufferedImage.TYPE_USHORT_565_RGB);
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
			} else if (code >= opRGB565) {
				readOpRGB565();
			} else if (code >= opLuma322) {
				readOpLuma322(code);
			} else /*if (code >= opLuma433)*/ {
				readOpLuma433(code);
			}

			if (repeatCount * 4 > pixels.length - p) {
				repeatCount = (pixels.length - p) / 4;
			}

			short shortRGB = lastRGB;
			do {
				pixels[p++] = shortRGB;

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
