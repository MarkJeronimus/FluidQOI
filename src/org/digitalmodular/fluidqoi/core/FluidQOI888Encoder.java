package org.digitalmodular.fluidqoi.core;

import java.util.Arrays;

import org.digitalmodular.fluidqoi.FluidQOIConfig;
import org.digitalmodular.fluidqoi.FluidQOIImageEncoder;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-25 Split from FluidQOIEncoder
public class FluidQOI888Encoder extends FluidQOIEncoder {
	static final int OP_REPEAT  = 135;
	static final int OP_MASK3   = 127; //   8 [?????rgb] 3 bit channel mask, then individual channel bytes
	static final int OP_LUMA222 = 64;  //  63 [??DyDuDv]
	static final int OP_LUMA644 = 0;   //  64 [??Dy____] [Du__Dv__]

	static final int LONGEST_OP = 4; // OP_MASK3

	// Encoder state
	private       byte     lastR = 0;
	private       byte     lastG = 0;
	private       byte     lastB = 0;
	private final byte[][] recentColorsList;

	@SuppressWarnings("AssignmentToSuperclassField")
	public FluidQOI888Encoder(FluidQOIConfig config) {
		super(LONGEST_OP, OP_REPEAT, config);

		opMask3 = OP_MASK3;
		opLuma222 = OP_LUMA222;
		opLuma644 = OP_LUMA644;

		recentColorsList = new byte[indexLength][3];
	}

	@Override
	protected void resetEncoderState() {
		lastR = 0;
		lastG = 0;
		lastB = 0;

		for (byte[] bytes : recentColorsList) {
			Arrays.fill(bytes, (byte)0);
		}

		if (indexLength > 1) {
			Arrays.fill(recentColorsList[1], (byte)0xFF);
		}
	}

	@Override
	public void encodePixel(byte r, byte g, byte b, byte a) {
		boolean recordRecent = true;

		int mask = 0;
		if (lastR != r) {
			mask |= 0b100;
		}
		if (lastG != g) {
			mask |= 0b010;
		}
		if (lastB != b) {
			mask |= 0b001;
		}

		if (mask == 0) {
			repeatCount++;
			recordRecent = firstPixel;
		} else {
			if (repeatCount != 0) {
				writeOpRepeat();
			}

			int recentColorIndex = findRecentColor(r, g, b, a);
			if (recentColorIndex >= 0) {
				writeOpIndex((byte)recentColorIndex);
				recordRecent = false;
			} else {
				byte dr = (byte)(r - lastR); // wrap around 8 bits, but keep signed
				byte dg = (byte)(g - lastG);
				byte db = (byte)(b - lastB);
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
				} else if (du >= -8 && du < 8 && // Ordered by largest chance to fail this test
				           dv >= -8 && dv < 8 &&
				           dy >= -32 && dy < 32) {
					writeOpLuma644(dy, du, dv);
				} else {
					writeOpMask3(mask, r, g, b);
				}
			}
		}

		firstPixel = false;
		lastR = r;
		lastG = g;
		lastB = b;

		if (recordRecent) {
			recentColorsList[recentColorsIndex][0] = r;
			recentColorsList[recentColorsIndex][1] = g;
			recentColorsList[recentColorsIndex][2] = b;
			recentColorsIndex = (recentColorsIndex + 1) % indexLength;
		}
	}

	private int findRecentColor(byte r, byte g, byte b, byte a) {
		for (int i = 0; i < indexLength; i++) {
			if (recentColorsList[i][0] == r && recentColorsList[i][1] == g &&
			    recentColorsList[i][2] == b && recentColorsList[i][3] == a) {
				return i;
			}
		}
		return -1;
	}
}
