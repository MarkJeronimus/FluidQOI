package org.digitalmodular.fluidqoi.core;

import java.util.Arrays;

import org.digitalmodular.fluidqoi.FluidQOIConfig;
import org.digitalmodular.fluidqoi.FluidQOIImageEncoder;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-22
public class FluidQOI555Encoder extends FluidQOIEncoder {
	static final int OP_REPEAT  = 191;
	static final int OP_LUMA222 = 128; //  63 [??DyDuDv]
	static final int OP_RGB555  = 0;   // 128 [?R____G_] [g__B____]

	static final int LONGEST_OP = 2; // OP_RGB565

	// Encoder state
	private       short   lastRGB = 0;
	private final short[] recentColorsList;

	public FluidQOI555Encoder(FluidQOIConfig config) {
		super(LONGEST_OP, OP_REPEAT, config);

		opLuma222 = OP_LUMA222;
		opRGB555 = OP_RGB555;

		recentColorsList = new short[indexLength];
	}

	@Override
	protected void resetEncoderState() {
		lastRGB = 0;
		Arrays.fill(recentColorsList, (short)0);
	}

	@Override
	public void encodePixel(byte r, byte g, byte b, byte a) {
		short rgb = (short)((r & 0b11111) << 11 |
		                    (g & 0b111111) << 5 |
		                    (b & 0b11111));

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
				byte dr = (byte)((rgb - (lastRGB & 0b01111100_00000000) & 0b01111100_00000000) << 16 >> 26);
				byte dg = (byte)((rgb - (lastRGB & 0b00000011_11100000) & 0b00000011_11100000) << 22 >> 26);
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
				    dy >= -2 && dy < 2) {
					writeOpLuma222(dy, du, dv);
				} else {
					writeOpRGB555(rgb);
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
