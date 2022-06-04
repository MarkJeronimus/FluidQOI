package org.digitalmodular.fluidqoi;

import java.util.StringJoiner;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-25
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "MethodWithTooManyParameters"})
public class FluidQOIEncoderStatistics {
	private int countIndex    = 0;
	private int countLuma222  = 0;
	private int countLuma322  = 0;
	private int countLuma433  = 0;
	private int countLuma4444 = 0;
	private int countLuma644  = 0;
	private int countRGB555   = 0;
	private int countRGB565   = 0;
	private int countMask3    = 0;
	private int countMask4    = 0;
	private int countRepeat   = 0;

	private final int[]   countIndexCounts      = new int[255];
	private final int[]   countMask3Counts      = new int[15];
	private final int[]   countMask4Counts      = new int[15];
	private final int[]   countRepeatCounts     = new int[254];
	private final int[][] diffLumaChannelCounts = new int[6][256];

	private final int[] headCodes = new int[256];

	public void recordOpIndex(int data, int index) {
		countIndex++;
		headCodes[index]++;
		countIndexCounts[index]++;
		System.out.printf("%02X             OP_INDEX(%d)\n", data, index);
	}

	public void recordOpLuma222(int data, int dy, int du, int dv) {
		countLuma222++;
		headCodes[data]++;
		System.out.printf("%02X             OP_LUMA222(%d %d %d)\n", data, dy, du, dv);
	}

	public void recordOpLuma322(int data, int dy, int du, int dv) {
		countLuma322++;
		headCodes[data]++;
		System.out.printf("%02X             OP_LUMA322(%d %d %d)\n", data, dy, du, dv);
	}

	public void recordOpLuma433(int data1, int data2, int dy, int du, int dv) {
		countLuma433++;
		headCodes[data1]++;
		System.out.printf("%02X %02X          OP_LUMA433(%d %d %d)\n", data1, data2 & 0xFF, dy, du, dv);
	}

	public void recordOpLuma4444(int data1, int data2, int data3, int dy, int du, int dv, int da) {
		countLuma4444++;
		headCodes[data1]++;
		System.out.printf("%02X %02X %02X       OP_LUMA4444(%d %d %d %d)\n",
		                  data1, data2 & 0xFF, data3 & 0xFF, dy, du, dv, da);
	}

	public void recordOpLuma644(int data1, int data2, int dy, int du, int dv) {
		countLuma644++;
		headCodes[data1]++;
		System.out.printf("%02X %02X          OP_LUMA644(%d %d %d)\n", data1, data2 & 0xFF, dy, du, dv);
	}

	public void recordOpRGB555(int data1, int data2, short rgb) {
		countRGB555++;
		headCodes[data1]++;
		System.out.printf("%02X %02X          OP_RGB555(%04X)\n", data1, data2 & 0xFF, rgb);
	}

	public void recordOpRGB565(int data1, int data2, int data3, short rgb) {
		countRGB565++;
		headCodes[data1]++;
		System.out.printf("%02X %02X %02X       OP_RGB565(%04X)\n", data1, data2 & 0xFF, data3 & 0xFF, rgb);
	}

	public void recordOpMask3(int data, int mask, byte r, byte g, byte b) {
		r &= 0xFF;
		g &= 0xFF;
		b &= 0xFF;

		if (mask == 0b0010) {
			System.out.printf("%02X %02X          OP_MASK4(--B- %d)\n",
			                  data, b & 0xFF, b);
		} else if (mask == 0b0100) {
			System.out.printf("%02X %02X          OP_MASK4(-G-- %d)\n",
			                  data, g & 0xFF, g);
		} else if (mask == 0b0110) {
			System.out.printf("%02X %02X %02X       OP_MASK4(-GB- %d %d)\n",
			                  data, g & 0xFF, b & 0xFF, g, b);
		} else if (mask == 0b1000) {
			System.out.printf("%02X %02X          OP_MASK4(R--- %d)\n",
			                  data, r & 0xFF, r);
		} else if (mask == 0b1010) {
			System.out.printf("%02X %02X %02X       OP_MASK4(R-B- %d %d)\n",
			                  data, r & 0xFF, b & 0xFF, r, b);
		} else if (mask == 0b1100) {
			System.out.printf("%02X %02X %02X       OP_MASK4(RG-- %d %d)\n",
			                  data, r & 0xFF, g & 0xFF, r, g);
		} else if (mask == 0b1110) {
			System.out.printf("%02X %02X %02X %02X    OP_MASK4(RGB- %d %d %d)\n",
			                  data, r & 0xFF, g & 0xFF, b & 0xFF, r, g, b);
		} else {
			throw new AssertionError("Invalid mask: " + mask);
		}

		countMask3++;
		countMask3Counts[mask - 1]++;
		headCodes[data]++;
	}

	public void recordOpMask4(int data, int mask, int r, int g, int b, int a) {
		r &= 0xFF;
		g &= 0xFF;
		b &= 0xFF;
		a &= 0xFF;

		if (mask == 0b0001) {
			System.out.printf("%02X %02X          OP_MASK4(---A %d)\n",
			                  data, a & 0xFF, a);
		} else if (mask == 0b0010) {
			System.out.printf("%02X %02X          OP_MASK4(--B- %d)\n",
			                  data, b & 0xFF, b);
		} else if (mask == 0b0011) {
			System.out.printf("%02X %02X %02X       OP_MASK4(--BA %d %d)\n",
			                  data, b & 0xFF, a & 0xFF, b, a);
		} else if (mask == 0b0100) {
			System.out.printf("%02X %02X          OP_MASK4(-G-- %d)\n",
			                  data, g & 0xFF, g);
		} else if (mask == 0b0101) {
			System.out.printf("%02X %02X %02X       OP_MASK4(-G-A %d %d)\n",
			                  data, g & 0xFF, a & 0xFF, g, a);
		} else if (mask == 0b0110) {
			System.out.printf("%02X %02X %02X       OP_MASK4(-GB- %d %d)\n",
			                  data, g & 0xFF, b & 0xFF, g, b);
		} else if (mask == 0b0111) {
			System.out.printf("%02X %02X %02X %02X    OP_MASK4(-GBA %d %d %d)\n",
			                  data, g & 0xFF, b & 0xFF, a & 0xFF, g, b, a);
		} else if (mask == 0b1000) {
			System.out.printf("%02X %02X          OP_MASK4(R--- %d)\n",
			                  data, r & 0xFF, r);
		} else if (mask == 0b1001) {
			System.out.printf("%02X %02X %02X       OP_MASK4(R--A %d %d)\n",
			                  data, r & 0xFF, a & 0xFF, r, a);
		} else if (mask == 0b1010) {
			System.out.printf("%02X %02X %02X       OP_MASK4(R-B- %d %d)\n",
			                  data, r & 0xFF, b & 0xFF, r, b);
		} else if (mask == 0b1011) {
			System.out.printf("%02X %02X %02X %02X    OP_MASK4(R-BA %d %d %d)\n",
			                  data, r & 0xFF, b & 0xFF, a & 0xFF, r, b, a);
		} else if (mask == 0b1100) {
			System.out.printf("%02X %02X %02X       OP_MASK4(RG-- %d %d)\n",
			                  data, r & 0xFF, g & 0xFF, r, g);
		} else if (mask == 0b1101) {
			System.out.printf("%02X %02X %02X %02X    OP_MASK4(RG-A %d %d %d)\n",
			                  data, r & 0xFF, g & 0xFF, a & 0xFF, r, g, a);
		} else if (mask == 0b1110) {
			System.out.printf("%02X %02X %02X %02X    OP_MASK4(RGB- %d %d %d)\n",
			                  data, r & 0xFF, g & 0xFF, b & 0xFF, r, g, b);
		} else if (mask == 0b1111) {
			System.out.printf("%02X %02X %02X %02X %02X OP_MASK4(RGBA %d %d %d %d)\n",
			                  data, r & 0xFF, g & 0xFF, b & 0xFF, a & 0xFF, r, g, b, a);
		} else {
			throw new AssertionError("Invalid mask: " + mask);
		}

		countMask4++;
		countMask4Counts[mask - 1]++;
		headCodes[data]++;
	}

	public void recordOpRepeat(int data, int count, int multiplier) {
		countRepeat++;
		countRepeatCounts[count - 1]++;
		headCodes[data]++;

		if (multiplier == 1) {
			System.out.printf("%02X             OP_REPEAT(%d)\n", data, count);
		} else {
			System.out.printf("%02X             OP_REPEAT(%d) (×%d)\n", data, count, multiplier);
		}
	}

	public void recordDiffLumaCounts(int dr, int dg, int db, int du, int dv, int da) {
		dr &= 0xFF;
		dg &= 0xFF;
		db &= 0xFF;
		du &= 0xFF;
		dv &= 0xFF;
		da &= 0xFF;
		diffLumaChannelCounts[0][dr]++;
		diffLumaChannelCounts[1][dg]++;
		diffLumaChannelCounts[2][db]++;
		diffLumaChannelCounts[3][du]++;
		diffLumaChannelCounts[4][dv]++;
		diffLumaChannelCounts[5][da]++;
	}

	public void add(FluidQOIEncoderStatistics other) {
		countIndex += other.countIndex;
		countLuma222 += other.countLuma222;
		countLuma4444 += other.countLuma4444;
		countLuma644 += other.countLuma644;
		countMask4 += other.countMask4;
		countRepeat += other.countRepeat;

		for (int i = 0; i < countMask4Counts.length; i++) {
			countMask4Counts[i] += other.countMask4Counts[i];
		}

		for (int i = 0; i < countRepeatCounts.length; i++) {
			countRepeatCounts[i] += other.countRepeatCounts[i];
		}

		for (int i = 0; i < diffLumaChannelCounts.length; i++) {
			for (int j = 0; j < diffLumaChannelCounts[i].length; j++) {
				diffLumaChannelCounts[i][j] += other.diffLumaChannelCounts[i][j];
			}
		}

		for (int i = 0; i < 256; i++) {
			headCodes[i] += other.headCodes[i];
		}
	}

	public void dump() {
		System.out.println("Statistics:");

		boolean hasLuma = false;
		if (countIndex > 0) {
			System.out.println("  Index     " + countIndex);
			System.out.println("  Index#    " + printDynamicArray(countIndexCounts));
		}

		if (countLuma222 > 0) {
			System.out.println("  Luma222   " + countLuma222);
			hasLuma = true;
		}

		if (countLuma322 > 0) {
			System.out.println("  Luma322   " + countLuma322);
			hasLuma = true;
		}

		if (countLuma433 > 0) {
			System.out.println("  Luma433   " + countLuma433);
			hasLuma = true;
		}

		if (countLuma4444 > 0) {
			System.out.println("  Luma4444  " + countLuma4444);
			hasLuma = true;
		}

		if (countLuma644 > 0) {
			System.out.println("  Luma644   " + countLuma644);
			hasLuma = true;
		}

		if (countRGB555 > 0) {
			System.out.println("  RGB555    " + countRGB555);
		}

		if (countRGB565 > 0) {
			System.out.println("  RGB565    " + countRGB565);
		}

		if (countMask3 > 0) {
			System.out.println("  Mask3     " + countMask3);
			for (int i = 1; i < 16; i++) {
				if (countMask3Counts[i - 1] > 0)
					System.out.printf("  Mask3%s%s%s  %d\n",
					                  (i & 0b1000) != 0 ? "R" : "-",
					                  (i & 0b0100) != 0 ? "G" : "-",
					                  (i & 0b0010) != 0 ? "B" : "-",
					                  countMask3Counts[i - 1]);
			}
		}

		if (countMask4 > 0) {
			System.out.println("  Mask4     " + countMask4);
			for (int i = 1; i < 16; i++) {
				if (countMask4Counts[i - 1] > 0)
					System.out.printf("  Mask4%s%s%s%s %d\n",
					                  (i & 0b1000) != 0 ? "R" : "-",
					                  (i & 0b0100) != 0 ? "G" : "-",
					                  (i & 0b0010) != 0 ? "B" : "-",
					                  (i & 0b0001) != 0 ? "A" : "-",
					                  countMask4Counts[i - 1]);
			}
		}

		if (countRepeat > 0) {
			System.out.println("  Repeat    " + countRepeat);
			System.out.println("  Repeat#   " + printDynamicArray(countRepeatCounts));
		}

		if (hasLuma) {
			System.out.println("  LumaCountsR " + printDynamicArray(diffLumaChannelCounts[0]));
			System.out.println("  LumaCountsG " + printDynamicArray(diffLumaChannelCounts[1]));
			System.out.println("  LumaCountsB " + printDynamicArray(diffLumaChannelCounts[2]));
			System.out.println("  LumaCountsU " + printDynamicArray(diffLumaChannelCounts[3]));
			System.out.println("  LumaCountsV " + printDynamicArray(diffLumaChannelCounts[4]));
			System.out.println("  LumaCountsA " + printDynamicArray(diffLumaChannelCounts[5]));
		}

		System.out.println("  HeadCodes " + printDynamicArray(headCodes));
	}

	private static String printDynamicArray(int[] array) {
		int lastNonZeroIndex;
		for (lastNonZeroIndex = array.length - 1; lastNonZeroIndex >= 0; lastNonZeroIndex--) {
			if (array[lastNonZeroIndex] != 0) {
				break;
			}
		}

		StringJoiner sj = new StringJoiner(", ");
		for (int i = 0; i <= lastNonZeroIndex; i++) {
			sj.add(Integer.toString(array[i]));
		}

		return sj.toString();
	}
}
