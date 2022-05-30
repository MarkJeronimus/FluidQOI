package org.digitalmodular.fluidqoi.core;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.digitalmodular.fluidqoi.FluidQOIFormat;

/**
 * Superclass for all decoders
 *
 * @author Mark Jeronimus
 */
// Created 2022-05-27
public abstract class FluidQOIDecoder extends FluidQOICodec {
	protected ByteBuffer in = null;

	private int repeatMultiplier = 1;

	protected FluidQOIDecoder(int opRepeat) {
		// We have to delay setting opIndex until the indexLength is decoded from the file.
		//noinspection AssignmentToSuperclassField
		this.opRepeat = opRepeat;
	}

	public BufferedImage decode(ByteBuffer in, int width, int height, FluidQOIFormat format, int indexLength) {
		this.in = in;

		setIndexLength(indexLength, opRepeat);

		resetDecoderState();
		BufferedImage image = decodeImageImpl(width, height, format);

		return image;
	}

	protected void resetDecoderState() {
		resetCodecState();

		resetRepeatMultiplier();
	}

	protected abstract BufferedImage decodeImageImpl(int width, int height, FluidQOIFormat format);

	protected int readOpRepeat(int data) {
		int count = data - opRepeat + 1;

		int repeatCount = count * repeatMultiplier;
		repeatMultiplier *= repeatLength;

		return repeatCount;
	}

	protected void resetRepeatMultiplier() {
		repeatMultiplier = 1;
	}
}
