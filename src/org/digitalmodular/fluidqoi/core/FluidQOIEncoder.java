package org.digitalmodular.fluidqoi.core;

import java.nio.ByteBuffer;

import org.digitalmodular.fluidqoi.FluidQOIConfig;
import org.digitalmodular.fluidqoi.FluidQOIEncoderStatistics;
import org.digitalmodular.fluidqoi.FluidQOIFormat;
import org.digitalmodular.fluidqoi.FluidQOIImageEncoder;

/**
 * Superclass for all encoders
 *
 * @author Mark Jeronimus
 */
// Created 2022-05-26
public abstract class FluidQOIEncoder extends FluidQOICodec {
	protected final int longestOp;

	private   ByteBuffer out         = null;
	protected boolean    firstPixel  = false;
	protected int        repeatCount = 0;

	protected final        FluidQOIEncoderStatistics statistics      = new FluidQOIEncoderStatistics();
	protected static final FluidQOIEncoderStatistics totalStatistics = new FluidQOIEncoderStatistics();

	protected FluidQOIEncoder(int longestOp, int opRepeat, FluidQOIConfig config) {
		this.longestOp = longestOp;

		//noinspection OverridableMethodCallDuringObjectConstruction
		setIndexLength(config.getIndexLength(), opRepeat);
	}

	public void beginEncoding(int width, int height, FluidQOIFormat format) {
		resetEncoderState();

		out = ByteBuffer.allocate(width * height * longestOp + FluidQOIImageEncoder.HEADER_LENGTH);

		writeHeader(width, height, format);
	}

	public abstract void encodePixel(byte r, byte g, byte b, byte a);

	public ByteBuffer finishEncoding() {
		if (repeatCount > 0) {
			writeOpRepeat();
		}

		for (int i = 0; i < longestOp; i++) {
			out.put((byte)0);
		}

		totalStatistics.add(statistics);

		out.flip();
		ByteBuffer returnValue = out;
		out = null;
		return returnValue;
	}

	protected void resetEncoderState() {
		resetCodecState();

		firstPixel = true;
		repeatCount = 0;
	}

	private void writeHeader(int width, int height, FluidQOIFormat format) {
		out.putInt(FluidQOIImageEncoder.FLUID_QOI_MAGIC);
		out.putInt(width);
		out.putInt(height);
		out.put(format.code());
		out.put((byte)indexLength);
		out.put((byte)0x00); // Format-dependent extension 1 (unused here)
		out.put((byte)0x00); // Format-dependent extension 2 (unused here)
	}

	protected void writeOpIndex(int index) {
		int data = opIndex + index;

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpIndex(data, index);
		}

		out.put((byte)data);
	}

	protected void writeOpRepeat() {
		repeatCount--;
		int multiplier = 1;

		do {
			int countMinusOne = repeatCount % repeatLength;
			int data          = opRepeat + countMinusOne;

			if (FluidQOIImageEncoder.debugging) {
				statistics.recordOpRepeat(data, countMinusOne + 1, multiplier);
				multiplier *= repeatLength;
			}

			out.put((byte)data);
			repeatCount = ((repeatCount - countMinusOne) / repeatLength) - 1;
		} while (repeatCount >= 0);

		repeatCount = 0;
	}

	protected void writeOpLuma222(int dy, int du, int dv) {
		int data = opLuma222 + ((dy & 0b11) << 4) | ((du & 0b11) << 2) | (dv & 0b11);

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpLuma222(data, dy, du, dv);
		}

		out.put((byte)data);
	}

	protected void writeOpLuma322(int dy, int du, int dv) {
		int data = opLuma322 + (((dy & 0b111) << 4) | ((du & 0b11) << 2) | (dv & 0b11));

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpLuma322(data, dy, du, dv);
		}

		out.put((byte)data);
	}

	protected void writeOpLuma433(int dy, int du, int dv) {
		int data1 = opLuma433 + ((dy & 0b1100) >> 2);
		int data2 = ((dy & 0b11) << 6) | ((du & 0b111) << 3) | (dv & 0b111);

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpLuma433(data1, data2, dy, du, dv);
		}

		out.put((byte)data1);
		out.put((byte)data2);
	}

	protected void writeOpLuma4444(int dy, int du, int dv, int da) {
		int data2 = ((dy & 0x1111) << 4) | (du & 0x1111);
		int data3 = ((dv & 0x1111) << 4) | (da & 0x1111);

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpLuma4444(opLuma4444, data2, data3, dy, du, dv, da);
		}

		out.put((byte)opLuma4444);
		out.put((byte)data2);
		out.put((byte)data3);
	}

	protected void writeOpLuma644(int dy, int du, int dv) {
		int data1 = opLuma644 + dy & 0b111111;
		int data2 = ((du & 0b1111) << 4) | (dv & 0b1111);

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpLuma644(data1, data2, dy, du, dv);
		}

		out.put((byte)data1);
		out.put((byte)data2);
	}

	protected void writeOpRGB555(short rgb) {
		int data1 = opRGB555 + (rgb >> 8);
		int data2 = rgb & 0xFF;

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpRGB555(data1, data2, rgb);
		}

		out.put((byte)data1);
		out.put((byte)data2);
	}

	protected void writeOpRGB565(short rgb) {
		int data2 = rgb >> 8;
		int data3 = rgb & 0xFF;

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpRGB565(opRGB565, data2, data3, rgb);
		}

		out.put((byte)opRGB565);
		out.put((byte)data2);
		out.put((byte)data3);
	}

	protected void writeOpMask3(int mask, byte r, byte g, byte b) {
		int data1 = opMask3 + mask;

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpMask3(data1, mask, r, g, b);
		}

		out.put((byte)data1);

		if ((mask & 0b1000) != 0) {
			out.put(r);
		}

		if ((mask & 0b0100) != 0) {
			out.put(g);
		}

		if ((mask & 0b0010) != 0) {
			out.put(b);
		}
	}

	protected void writeOpMask4(int mask, byte r, byte g, byte b, byte a) {
		int data1 = opMask4 + mask;

		if (FluidQOIImageEncoder.debugging) {
			statistics.recordOpMask4(data1, mask, r, g, b, a);
		}

		out.put((byte)data1);

		if ((mask & 0b1000) != 0) {
			out.put(r);
		}

		if ((mask & 0b0100) != 0) {
			out.put(g);
		}

		if ((mask & 0b0010) != 0) {
			out.put(b);
		}

		if ((mask & 0b0001) != 0) {
			out.put(a);
		}
	}
}
