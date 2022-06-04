package org.digitalmodular.fluidqoi.core;

import java.util.Arrays;

import org.digitalmodular.fluidqoi.FluidQOIConfig;
import org.digitalmodular.fluidqoi.FluidQOIImageEncoder;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-22
public class FluidQOI565Encoder extends FluidQOIEncoder {
	static final int OP_REPEAT  = 132;
	static final int OP_RGB565  = 131; //   1 [????????] [R____G__] [g__B____]
	static final int OP_LUMA322 = 4;   // 127 [?Dy_DuDv]
	static final int OP_LUMA433 = 0;   //   4 [??????Dy] [__Du_Dv_]

	static final int LONGEST_OP = 3; // OP_RGB565

	// Encoder state
	private       short   lastRGB = 0;
	private final short[] recentColorsList;

	public FluidQOI565Encoder(FluidQOIConfig config) {
		super(LONGEST_OP, OP_REPEAT, config);

		opRGB565 = OP_RGB565;
		opLuma322 = OP_LUMA322;
		opLuma433 = OP_LUMA433;

		recentColorsList = new short[indexLength];
	}

	@Override
	protected void resetEncoderState() {
		super.resetEncoderState();

		lastRGB = 0;

		Arrays.fill(recentColorsList, (short)0);
	}

	@Override
	public void encodePixel(byte r, byte g, byte b, byte a) {
		// Premultiply alpha
		short rgb = (short)((((int)((r & 0xFF) / 255.0f * (a & 0xFF) + 0.5f) & 0b11111000) << 8) |
		                    (((int)((g & 0xFF) / 255.0f * (a & 0xFF) + 0.5f) & 0b11111100) << 3) |
		                    (((int)((b & 0xFF) / 255.0f * (a & 0xFF) + 0.5f) & 0b11111000) >> 3));

		encodePixel(rgb);
	}

	private void encodePixel(short rgb) {
		boolean recordRecent = true;

		if (rgb == lastRGB) {
			repeatCount++;
			recordRecent = firstPixel;
		} else {
			if (repeatCount != 0) {
				writeOpRepeat();
			}

			int recentColorIndex = findRecentColor(rgb);
			if (recentColorIndex >= 0) {
				writeOpIndex((byte)recentColorIndex);
				recordRecent = false;
			} else {
				byte dr = (byte)((rgb - (lastRGB & 0b11111000_00000000) & 0b11111000_00000000) << 15 >> 26);
				byte dg = (byte)((rgb - (lastRGB & 0b00000111_11100000) & 0b00000111_11100000) << 21 >> 26);
				byte db = (byte)((rgb - (lastRGB & 0b00000000_00011111) & 0b00000000_00011111) << 26 >> 26);
				//noinspection UnnecessaryLocalVariable
				byte dy = dg;
				byte du = (byte)(dr - dy);
				byte dv = (byte)(db - dy);

				if (FluidQOIImageEncoder.debugging) {
					statistics.recordDiffLumaCounts(dr, dg, db, du, dv, 0);
				}

				if (du >= -2 && du < 2 && // Ordered by largest chance to fail this test
				    dv >= -2 && dv < 2 &&
				    dy >= -4 && dy < 4) {
					writeOpLuma322(dy, du, dv);
				} else if (du >= -4 && du < 4 && // Ordered by largest chance to fail this test
				           dv >= -4 && dv < 4 &&
				           dy >= -8 && dy < 8) {
					writeOpLuma433(dy, du, dv);
				} else {
					writeOpRGB565(rgb);
				}
			}
		}

		firstPixel = false;
		lastRGB = rgb;

		if (recordRecent) {
			recentColorsList[recentColorsIndex] = rgb;
			recentColorsIndex = (recentColorsIndex + 1) % indexLength;
		}
	}

	private int findRecentColor(short rgb) {
		for (int i = 0; i < indexLength; i++) {
			if (recentColorsList[i] == rgb) {
				return i;
			}
		}

		return -1;
	}

}
